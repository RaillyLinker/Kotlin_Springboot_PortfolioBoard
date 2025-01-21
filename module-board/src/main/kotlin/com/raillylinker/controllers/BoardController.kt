package com.raillylinker.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import com.raillylinker.services.BoardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Tag(name = "/board APIs", description = "샘플 게시판 API 컨트롤러")
@Controller
@RequestMapping("/board")
class BoardController(
    private val service: BoardService
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <매핑 함수 공간>
    @Operation(
        summary = "게시글 입력 API <>",
        description = "게시글을 입력합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/board"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    fun createBoard(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: CreateBoardInputVo
    ): CreateBoardOutputVo? {
        return service.createBoard(httpServletResponse, authorization!!, inputVo)
    }

    data class CreateBoardInputVo(
        @Schema(description = "글 타이틀", required = true, example = "테스트 타이틀입니다.")
        @JsonProperty("title")
        val title: String,
        @Schema(description = "글 본문", required = true, example = "테스트 본문입니다.")
        @JsonProperty("content")
        val content: String
    )

    data class CreateBoardOutputVo(
        @Schema(description = "생성된 게시글 고유번호", required = true, example = "1234")
        @JsonProperty("uid")
        val uid: Long
    )


    // ----
    @Operation(
        summary = "게시글 리스트 (페이징)",
        description = "게시글 테이블의 정보를 페이징하여 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            )
        ]
    )
    @GetMapping(
        path = ["/board-page"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun getBoardPage(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "page", description = "원하는 페이지(1 부터 시작)", example = "1")
        @RequestParam("page")
        page: Int,
        @Parameter(name = "pageElementsCount", description = "페이지 아이템 개수", example = "10")
        @RequestParam("pageElementsCount")
        pageElementsCount: Int,
        @Parameter(
            name = "sortingTypeEnum",
            description = """
                정렬 기준(
                    CREATE_DATE : 게시글 작성 시간,
                    UPDATE_DATE : 게시글 수정 시간,
                    TITLE : 게시글 제목,
                    WRITER_USER_NICKNAME : 게시글 작성자 닉네임
                )
            """,
            example = "CREATE_DATE"
        )
        @RequestParam("sortingTypeEnum")
        sortingTypeEnum: GetBoardPageSortingTypeEnum,
        @Parameter(
            name = "sortingDirectionEnum",
            description = """
                정렬 방향(
                    DESC : 내림차순,
                    UPDATE_DATE : 오름차순
                )
            """,
            example = "DESC"
        )
        @RequestParam("sortingDirectionEnum")
        sortingDirectionEnum: GetBoardPageSortingDirectionEnum,
        @Parameter(
            name = "searchTypeEnum",
            description = """
                검색 기준(
                    WRITER : 게시글 작성자,
                    TITLE : 게시글 제목,
                    TITLE_OR_CONTENT : 게시글 제목 혹은 본문 내용
                )
            """,
            example = "WRITER"
        )
        @RequestParam("searchTypeEnum")
        searchTypeEnum: GetBoardPageSearchTypeEnum?,
        @Parameter(name = "searchKeyword", description = "검색 키워드", example = "테스트")
        @RequestParam("searchKeyword")
        searchKeyword: String?
    ): GetBoardPageOutputVo? {
        return service.getBoardPage(
            httpServletResponse,
            page,
            pageElementsCount,
            sortingTypeEnum,
            sortingDirectionEnum,
            searchTypeEnum,
            searchKeyword
        )
    }

    enum class GetBoardPageSortingTypeEnum {
        CREATE_DATE,
        UPDATE_DATE,
        TITLE,
        VIEW_COUNT,
        WRITER_USER_NICKNAME
    }

    enum class GetBoardPageSearchTypeEnum {
        WRITER,
        TITLE,
        TITLE_OR_CONTENT
    }

    enum class GetBoardPageSortingDirectionEnum {
        DESC,
        ASC
    }

    data class GetBoardPageOutputVo(
        @Schema(description = "아이템 전체 개수", required = true, example = "100")
        @JsonProperty("totalElements")
        val totalElements: Long,
        @Schema(description = "게시글 아이템 리스트", required = true)
        @JsonProperty("boardItemVoList")
        val boardItemVoList: List<BoardItemVo>
    ) {
        @Schema(description = "게시글 아이템")
        data class BoardItemVo(
            @Schema(description = "글 고유번호", required = true, example = "1234")
            @JsonProperty("boardUid")
            val boardUid: Long,
            @Schema(description = "글 제목", required = true, example = "테스트 텍스트입니다.")
            @JsonProperty("title")
            val title: String,
            @Schema(
                description = "글 작성일(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
                required = true,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("createDate")
            val createDate: String,
            @Schema(
                description = "글 수정일(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
                required = true,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("updateDate")
            val updateDate: String,
            @Schema(description = "글 조회수", required = true, example = "1234")
            @JsonProperty("viewCount")
            val viewCount: Long,
            @Schema(description = "글 작성자 고유번호", required = true, example = "1234")
            @JsonProperty("writerUserUid")
            val writerUserUid: Long,
            @Schema(description = "글 작성자 닉네임", required = true, example = "홍길동")
            @JsonProperty("writerUserNickname")
            val writerUserNickname: String,
            @Schema(description = "글 작성자 프로필 Full Url", required = false, example = "https://test-profile/1.jpg")
            @JsonProperty("writerUserProfileFullUrl")
            val writerUserProfileFullUrl: String?
        )
    }


    // ----
    @Operation(
        summary = "게시판 상세 화면",
        description = "게시판 상세 화면의 정보를 요청합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : boardUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @GetMapping(
        path = ["/board/{boardUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun getBoardDetail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "boardUid", description = "게시글 고유번호", example = "1")
        @PathVariable("boardUid")
        boardUid: Long
    ): GetBoardDetailOutputVo? {
        return service.getBoardDetail(
            httpServletResponse,
            boardUid
        )
    }

    data class GetBoardDetailOutputVo(
        @Schema(description = "글 제목", required = true, example = "테스트 텍스트입니다.")
        @JsonProperty("title")
        val title: String,
        @Schema(description = "글 본문", required = true, example = "테스트 텍스트입니다.")
        @JsonProperty("content")
        val content: String,
        @Schema(
            description = "글 작성일(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("createDate")
        val createDate: String,
        @Schema(
            description = "글 수정일(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_552_KST"
        )
        @JsonProperty("updateDate")
        val updateDate: String,
        @Schema(description = "글 조회수", required = true, example = "1234")
        @JsonProperty("viewCount")
        val viewCount: Long,
        @Schema(description = "글 작성자 고유번호", required = true, example = "1234")
        @JsonProperty("writerUserUid")
        val writerUserUid: Long,
        @Schema(description = "글 작성자 닉네임", required = true, example = "홍길동")
        @JsonProperty("writerUserNickname")
        val writerUserNickname: String,
        @Schema(description = "글 작성자 프로필 Full Url", required = false, example = "https://test-profile/1.jpg")
        @JsonProperty("writerUserProfileFullUrl")
        val writerUserProfileFullUrl: String?
    )


    // ----
    @Operation(
        summary = "게시글 수정 <>",
        description = "게시글 하나를 수정합니다.<br>" +
                "본인 게시글이 아니라면 204 코드 1 이 반환"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : boardUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PutMapping(
        path = ["/board/{boardUid}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    fun updateBoard(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "boardUid", description = "수정할 게시글 고유번호", example = "1")
        @PathVariable("boardUid")
        boardUid: Long,
        @RequestBody
        inputVo: UpdateBoardInputVo
    ) {
        return service.updateBoard(httpServletResponse, authorization!!, boardUid, inputVo)
    }

    data class UpdateBoardInputVo(
        @Schema(description = "글 타이틀", required = true, example = "테스트 타이틀입니다.")
        @JsonProperty("title")
        val title: String,
        @Schema(description = "글 본문", required = true, example = "테스트 본문입니다.")
        @JsonProperty("content")
        val content: String
    )


    // ----
    @Operation(
        summary = "게시글 조회수 1 상승",
        description = "게시글 조회수를 1 상승시킵니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            )
        ]
    )
    @PatchMapping(
        path = ["/board/{boardUid}/view_count_1up"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    fun updateBoardViewCount1Up(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "boardUid", description = "수정할 게시글 고유번호", example = "1")
        @PathVariable("boardUid")
        boardUid: Long
    ) {
        return service.updateBoardViewCount1Up(httpServletResponse, boardUid)
    }


    // ----
    @Operation(
        summary = "게시글 삭제 <>",
        description = "게시글을 삭제합니다.<br>" +
                "본인 게시글이 아니라면 204 코드 1 이 반환"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : boardUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/board/{boardUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    fun deleteBoard(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "boardUid", description = "삭제할 게시글 고유번호", example = "1")
        @PathVariable("boardUid")
        boardUid: Long
    ) {
        return service.deleteBoard(httpServletResponse, authorization!!, boardUid)
    }


    // ----
    @Operation(
        summary = "댓글 입력 API <>",
        description = "댓글을 입력합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : boardUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : commentUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/comment"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    fun createComment(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: CreateCommentInputVo
    ): CreateCommentOutputVo? {
        return service.createComment(httpServletResponse, authorization!!, inputVo)
    }

    data class CreateCommentInputVo(
        @Schema(description = "댓글을 달 게시글 고유번호", required = true, example = "1")
        @JsonProperty("boardUid")
        val boardUid: Long,
        @Schema(description = "댓글을 달 댓글 고유번호(댓글에 다는 댓글이 아니면 null)", required = false, example = "1")
        @JsonProperty("commentUid")
        val commentUid: Long?,
        @Schema(description = "글 본문", required = true, example = "테스트 본문입니다.")
        @JsonProperty("content")
        val content: String
    )

    data class CreateCommentOutputVo(
        @Schema(description = "생성된 댓글 고유번호", required = true, example = "1234")
        @JsonProperty("uid")
        val uid: Long
    )


    // ----
    @Operation(
        summary = "댓글 리스트 (페이징)",
        description = "댓글 정보를 페이징하여 반환합니다.<br>" +
                "댓글 리스트는 최신순, 대댓글 리스트는 오래된 순으로 정렬"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            )
        ]
    )
    @GetMapping(
        path = ["/board/{boardUid}/comment-page"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun getCommentPage(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "boardUid", description = "댓글이 달린 게시글 고유번호", example = "1")
        @PathVariable("boardUid")
        boardUid: Long,
        @Parameter(name = "commentUid", description = "댓글 고유번호(대댓글이 아니라면 null)", example = "1")
        @RequestParam("commentUid")
        commentUid: Long?,
        @Parameter(name = "page", description = "원하는 페이지(1 부터 시작)", example = "1")
        @RequestParam("page")
        page: Int,
        @Parameter(name = "pageElementsCount", description = "페이지 아이템 개수", example = "10")
        @RequestParam("pageElementsCount")
        pageElementsCount: Int
    ): GetCommentPageOutputVo? {
        return service.getCommentPage(
            httpServletResponse,
            boardUid,
            commentUid,
            page,
            pageElementsCount
        )
    }

    data class GetCommentPageOutputVo(
        @Schema(description = "아이템 전체 개수", required = true, example = "100")
        @JsonProperty("totalElements")
        val totalElements: Long,
        @Schema(description = "댓글 아이템 리스트", required = true)
        @JsonProperty("commentItemVoList")
        val commentItemVoList: List<CommentItemVo>
    ) {
        @Schema(description = "댓글 아이템")
        data class CommentItemVo(
            @Schema(description = "글 고유번호", required = true, example = "1234")
            @JsonProperty("commentUid")
            val commentUid: Long,
            @Schema(description = "글 본문", required = true, example = "테스트 텍스트입니다.")
            @JsonProperty("content")
            val content: String,
            @Schema(
                description = "글 작성일(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
                required = true,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("createDate")
            val createDate: String,
            @Schema(
                description = "글 수정일(yyyy_MM_dd_'T'_HH_mm_ss_SSS_z)",
                required = true,
                example = "2024_05_02_T_15_14_49_552_KST"
            )
            @JsonProperty("updateDate")
            val updateDate: String,
            @Schema(description = "글 작성자 고유번호", required = true, example = "1234")
            @JsonProperty("writerUserUid")
            val writerUserUid: Long,
            @Schema(description = "글 작성자 닉네임", required = true, example = "홍길동")
            @JsonProperty("writerUserNickname")
            val writerUserNickname: String,
            @Schema(description = "글 작성자 프로필 Full Url", required = false, example = "https://test-profile/1.jpg")
            @JsonProperty("writerUserProfileFullUrl")
            val writerUserProfileFullUrl: String?
        )
    }


    // ----
    @Operation(
        summary = "댓글 수정 <>",
        description = "댓글 하나를 수정합니다.<br>" +
                "본인 댓글이 아니라면 204 코드 1 이 반환"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : commentUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PutMapping(
        path = ["/comment/{commentUid}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    fun updateComment(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "commentUid", description = "수정할 댓글 고유번호", example = "1")
        @PathVariable("commentUid")
        commentUid: Long,
        @RequestBody
        inputVo: UpdateCommentInputVo
    ) {
        return service.updateComment(httpServletResponse, authorization!!, commentUid, inputVo)
    }

    data class UpdateCommentInputVo(
        @Schema(description = "글 본문", required = true, example = "테스트 본문입니다.")
        @JsonProperty("content")
        val content: String
    )


    // ----
    @Operation(
        summary = "댓글 삭제 <>",
        description = "댓글을 삭제합니다.<br>" +
                "본인 댓글이 아니라면 204 코드 1 이 반환"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : commentUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @DeleteMapping(
        path = ["/comment/{commentUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    fun deleteComment(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "commentUid", description = "삭제할 댓글 고유번호", example = "1")
        @PathVariable("commentUid")
        commentUid: Long
    ) {
        return service.deleteComment(httpServletResponse, authorization!!, commentUid)
    }
}