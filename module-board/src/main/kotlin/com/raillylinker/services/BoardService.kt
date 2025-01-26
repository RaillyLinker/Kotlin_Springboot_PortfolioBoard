package com.raillylinker.services

import com.raillylinker.configurations.SecurityConfig.AuthTokenFilterTotalAuth.Companion.AUTH_JWT_CLAIMS_AES256_ENCRYPTION_KEY
import com.raillylinker.configurations.SecurityConfig.AuthTokenFilterTotalAuth.Companion.AUTH_JWT_CLAIMS_AES256_INITIALIZATION_VECTOR
import com.raillylinker.controllers.BoardController
import com.raillylinker.configurations.jpa_configs.Db1MainConfig
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_SampleBoard
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_SampleBoardComment
import com.raillylinker.jpa_beans.db1_main.repositories.*
import com.raillylinker.jpa_beans.db1_main.repositories_dsl.Db1_RaillyLinkerCompany_SampleBoardComment_RepositoryDsl
import com.raillylinker.jpa_beans.db1_main.repositories_dsl.Db1_RaillyLinkerCompany_SampleBoard_RepositoryDsl
import com.raillylinker.redis_map_components.redis1_main.Redis1_Lock_BoardView
import com.raillylinker.util_components.JwtTokenUtil
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Service
class BoardService(
    // (프로젝트 실행시 사용 설정한 프로필명 (ex : dev8080, prod80, local8080, 설정 안하면 default 반환))
    @Value("\${spring.profiles.active:default}") private var activeProfile: String,

    private val jwtTokenUtil: JwtTokenUtil,

    // (Database Repository)
    private val db1RaillyLinkerCompanySampleBoardRepository: Db1_RaillyLinkerCompany_SampleBoard_Repository,
    private val db1RaillyLinkerCompanySampleBoardCommentRepository: Db1_RaillyLinkerCompany_SampleBoardComment_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberRepository: Db1_RaillyLinkerCompany_TotalAuthMember_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberEmailRepository: Db1_RaillyLinkerCompany_TotalAuthMemberEmail_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberPhoneRepository: Db1_RaillyLinkerCompany_TotalAuthMemberPhone_Repository,
    private val db1RaillyLinkerCompanyTotalAuthMemberProfileRepository: Db1_RaillyLinkerCompany_TotalAuthMemberProfile_Repository,

    // (Database Repository DSL)
    private val db1RaillyLinkerCompanySampleBoardRepositoryDsl: Db1_RaillyLinkerCompany_SampleBoard_RepositoryDsl,
    private val db1RaillyLinkerCompanySampleBoardCommentRepositoryDsl: Db1_RaillyLinkerCompany_SampleBoardComment_RepositoryDsl,

    private val redis1LockBoardView: Redis1_Lock_BoardView
) {
    // <멤버 변수 공간>
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    // (스레드 풀)
    private val executorService: ExecutorService = Executors.newCachedThreadPool()


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (게시글 입력 API)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun createBoard(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        inputVo: BoardController.CreateBoardInputVo
    ): BoardController.CreateBoardOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            AUTH_JWT_CLAIMS_AES256_INITIALIZATION_VECTOR,
            AUTH_JWT_CLAIMS_AES256_ENCRYPTION_KEY
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val db1RaillyLinkerCompanySampleBoard = db1RaillyLinkerCompanySampleBoardRepository.save(
            Db1_RaillyLinkerCompany_SampleBoard(
                memberData,
                inputVo.title,
                inputVo.content,
                0L
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return BoardController.CreateBoardOutputVo(
            db1RaillyLinkerCompanySampleBoard.uid!!
        )
    }


    // ----
    // (게시글 리스트 (페이징))
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getBoardPage(
        httpServletResponse: HttpServletResponse,
        page: Int,
        pageElementsCount: Int,
        sortingTypeEnum: BoardController.GetBoardPageSortingTypeEnum,
        sortingDirectionEnum: BoardController.GetBoardPageSortingDirectionEnum,
        searchTypeEnum: BoardController.GetBoardPageSearchTypeEnum?,
        searchKeyword: String?
    ): BoardController.GetBoardPageOutputVo? {
        val pageable: Pageable = PageRequest.of(page - 1, pageElementsCount)
        val entityList = db1RaillyLinkerCompanySampleBoardRepositoryDsl.findPageAllFromBoardByNotDeleted(
            sortingTypeEnum,
            sortingDirectionEnum,
            pageable,
            searchTypeEnum,
            searchKeyword
        )

        val boardItemVoList =
            ArrayList<BoardController.GetBoardPageOutputVo.BoardItemVo>()
        for (entity in entityList) {
            val profileEntityList =
                db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findAllByTotalAuthMemberUidAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                    entity.writerUserUid,
                    "/"
                )

            boardItemVoList.add(
                BoardController.GetBoardPageOutputVo.BoardItemVo(
                    entity.boardUid,
                    entity.title,
                    entity.createDate.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                    entity.updateDate.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                    entity.viewCount,
                    entity.writerUserUid,
                    entity.writerUserNickname,
                    if (profileEntityList.isEmpty()) {
                        null
                    } else {
                        profileEntityList.first().imageFullUrl
                    }
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return BoardController.GetBoardPageOutputVo(
            entityList.totalElements,
            boardItemVoList
        )
    }


    // ----
    // (게시판 상세 화면)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getBoardDetail(
        httpServletResponse: HttpServletResponse,
        boardUid: Long
    ): BoardController.GetBoardDetailOutputVo? {
        val boardEntity = db1RaillyLinkerCompanySampleBoardRepository.findByUidAndRowDeleteDateStr(
            boardUid,
            "/"
        )

        if (boardEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val profileEntityList =
            db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findAllByTotalAuthMemberUidAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                boardEntity.totalAuthMember.uid!!,
                "/"
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return BoardController.GetBoardDetailOutputVo(
            boardEntity.boardTitle,
            boardEntity.boardContent,
            boardEntity.rowCreateDate!!.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
            boardEntity.rowUpdateDate!!.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
            boardEntity.viewCount,
            boardEntity.totalAuthMember.uid!!,
            boardEntity.totalAuthMember.accountId,
            if (profileEntityList.isEmpty()) {
                null
            } else {
                profileEntityList.first().imageFullUrl
            }
        )
    }


    // ----
    // (게시글 수정)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun updateBoard(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        boardUid: Long,
        inputVo: BoardController.UpdateBoardInputVo
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            AUTH_JWT_CLAIMS_AES256_INITIALIZATION_VECTOR,
            AUTH_JWT_CLAIMS_AES256_ENCRYPTION_KEY
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val boardEntity = db1RaillyLinkerCompanySampleBoardRepository.findByUidAndTotalAuthMemberAndRowDeleteDateStr(
            boardUid,
            memberData,
            "/"
        )

        if (boardEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        boardEntity.boardTitle = inputVo.title
        boardEntity.boardContent = inputVo.content

        db1RaillyLinkerCompanySampleBoardRepository.save(boardEntity)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (게시글 조회수 1 상승)
    fun updateBoardViewCount1Up(
        httpServletResponse: HttpServletResponse,
        boardUid: Long
    ) {
        // 바로 업데이트 작업 위임
        executorService.execute {
            updateBoardViewCount1UpInAsync(boardUid)
        }

        httpServletResponse.status = HttpStatus.OK.value()
    }

    // 게시글 조회수 상승 공유락 처리 함수
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun updateBoardViewCount1UpInAsync(
        boardUid: Long
    ) {
        redis1LockBoardView.tryLockRepeat<Unit>(
            // Redis 키
            "$boardUid",
            // Redis 만료시간
            1000L,
            // Lock 획득 후 작업 콜백
            {
                // 락 획득 후 기존 viewCount 조회 후 수정
                val boardEntity = db1RaillyLinkerCompanySampleBoardRepository.findByUidAndRowDeleteDateStr(
                    boardUid,
                    "/"
                )

                if (boardEntity == null) {
                    return@tryLockRepeat
                }

                boardEntity.viewCount += 1

                db1RaillyLinkerCompanySampleBoardRepository.save(boardEntity)
            },
            // 락 불발시 최소 대기시간
            50L,
            // 락 불발 반복시 대기시간 증가율
            0.1,
            // 락 불발 반복시 대기시간 증가 최대시간
            100L
        )
    }


    // ----
    // (게시글 삭제)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteBoard(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        boardUid: Long
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            AUTH_JWT_CLAIMS_AES256_INITIALIZATION_VECTOR,
            AUTH_JWT_CLAIMS_AES256_ENCRYPTION_KEY
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val boardEntity = db1RaillyLinkerCompanySampleBoardRepository.findByUidAndTotalAuthMemberAndRowDeleteDateStr(
            boardUid,
            memberData,
            "/"
        )

        if (boardEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        // 재귀적으로 댓글과 하위 댓글 삭제
        boardEntity.sampleBoardCommentList.forEach { comment ->
            deleteCommentsRecursively(comment)
        }

        boardEntity.rowDeleteDateStr =
            LocalDateTime.now().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))

        db1RaillyLinkerCompanySampleBoardRepository.save(boardEntity)

        httpServletResponse.status = HttpStatus.OK.value()
    }

    fun deleteCommentsRecursively(comment: Db1_RaillyLinkerCompany_SampleBoardComment) {
        // 자식 댓글 삭제
        comment.sampleBoardCommentList.forEach { childComment ->
            deleteCommentsRecursively(childComment)
        }

        // 현재 댓글 삭제 처리
        comment.rowDeleteDateStr = LocalDateTime.now()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
        db1RaillyLinkerCompanySampleBoardCommentRepository.save(comment)
    }


    // ----
    // (댓글 입력 API)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun createComment(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        inputVo: BoardController.CreateCommentInputVo
    ): BoardController.CreateCommentOutputVo? {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            AUTH_JWT_CLAIMS_AES256_INITIALIZATION_VECTOR,
            AUTH_JWT_CLAIMS_AES256_ENCRYPTION_KEY
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val boardEntity = db1RaillyLinkerCompanySampleBoardRepository.findByUidAndTotalAuthMemberAndRowDeleteDateStr(
            inputVo.boardUid,
            memberData,
            "/"
        )

        if (boardEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val commentEntity = if (inputVo.commentUid == null) {
            null
        } else {
            val commentResult =
                db1RaillyLinkerCompanySampleBoardCommentRepository.findByUidAndSampleBoardAndRowDeleteDateStr(
                    inputVo.commentUid,
                    boardEntity,
                    "/"
                )

            if (commentResult == null) {
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "2")
                return null
            }

            commentResult
        }

        val db1RaillylinkerCompanySampleBoardComment = db1RaillyLinkerCompanySampleBoardCommentRepository.save(
            Db1_RaillyLinkerCompany_SampleBoardComment(
                memberData,
                boardEntity,
                commentEntity,
                inputVo.content
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return BoardController.CreateCommentOutputVo(
            db1RaillylinkerCompanySampleBoardComment.uid!!
        )
    }


    // ----
    // (댓글 리스트 (페이징))
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME, readOnly = true)
    fun getCommentPage(
        httpServletResponse: HttpServletResponse,
        boardUid: Long,
        commentUid: Long?,
        page: Int,
        pageElementsCount: Int
    ): BoardController.GetCommentPageOutputVo? {
        val pageable: Pageable = PageRequest.of(page - 1, pageElementsCount)
        val entityList = db1RaillyLinkerCompanySampleBoardCommentRepositoryDsl.findPageAllFromBoardCommentByNotDeleted(
            boardUid,
            commentUid,
            pageable
        )

        val boardCommentItemVoList =
            ArrayList<BoardController.GetCommentPageOutputVo.CommentItemVo>()
        for (entity in entityList) {
            val profileEntityList =
                db1RaillyLinkerCompanyTotalAuthMemberProfileRepository.findAllByTotalAuthMemberUidAndRowDeleteDateStrOrderByPriorityDescRowCreateDateDesc(
                    entity.writerUserUid,
                    "/"
                )

            boardCommentItemVoList.add(
                BoardController.GetCommentPageOutputVo.CommentItemVo(
                    entity.commentUid,
                    entity.content,
                    entity.createDate.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                    entity.updateDate.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z")),
                    entity.writerUserUid,
                    entity.writerUserNickname,
                    if (profileEntityList.isEmpty()) {
                        null
                    } else {
                        profileEntityList.first().imageFullUrl
                    }
                )
            )
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return BoardController.GetCommentPageOutputVo(
            entityList.totalElements,
            boardCommentItemVoList
        )
    }


    // ----
    // (댓글 수정)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun updateComment(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        commentUid: Long,
        inputVo: BoardController.UpdateCommentInputVo
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            AUTH_JWT_CLAIMS_AES256_INITIALIZATION_VECTOR,
            AUTH_JWT_CLAIMS_AES256_ENCRYPTION_KEY
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val commentEntity =
            db1RaillyLinkerCompanySampleBoardCommentRepository.findByUidAndTotalAuthMemberAndRowDeleteDateStr(
                commentUid,
                memberData,
                "/"
            )

        if (commentEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        commentEntity.commentContent = inputVo.content

        db1RaillyLinkerCompanySampleBoardCommentRepository.save(commentEntity)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (댓글 삭제)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteComment(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        commentUid: Long
    ) {
        val memberUid = jwtTokenUtil.getMemberUid(
            authorization.split(" ")[1].trim(),
            AUTH_JWT_CLAIMS_AES256_INITIALIZATION_VECTOR,
            AUTH_JWT_CLAIMS_AES256_ENCRYPTION_KEY
        )
        val memberData =
            db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!

        val commentEntity =
            db1RaillyLinkerCompanySampleBoardCommentRepository.findByUidAndTotalAuthMemberAndRowDeleteDateStr(
                commentUid,
                memberData,
                "/"
            )

        if (commentEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        // 재귀적으로 댓글과 하위 댓글 삭제
        deleteCommentsRecursively(commentEntity)

        commentEntity.rowDeleteDateStr =
            LocalDateTime.now().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))

        db1RaillyLinkerCompanySampleBoardCommentRepository.save(commentEntity)

        httpServletResponse.status = HttpStatus.OK.value()
    }
}