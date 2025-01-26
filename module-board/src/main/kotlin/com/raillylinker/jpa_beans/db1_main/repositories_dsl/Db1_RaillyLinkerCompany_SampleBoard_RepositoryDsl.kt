package com.raillylinker.jpa_beans.db1_main.repositories_dsl

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.raillylinker.controllers.BoardController
import com.raillylinker.controllers.BoardController.GetBoardPageSortingDirectionEnum
import com.raillylinker.controllers.BoardController.GetBoardPageSortingTypeEnum
import com.raillylinker.jpa_beans.db1_main.entities.QDb1_RaillyLinkerCompany_SampleBoard
import com.raillylinker.jpa_beans.db1_main.entities.QDb1_RaillyLinkerCompany_TotalAuthMember
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

@Repository
class Db1_RaillyLinkerCompany_SampleBoard_RepositoryDsl(entityManager: EntityManager) {
    private val queryFactory: JPAQueryFactory = JPAQueryFactory(entityManager)


    // ----
    // (railly_linker_company.sample_board 테이블 페이징)
    fun findPageAllFromBoardByNotDeleted(
        sortingType: GetBoardPageSortingTypeEnum,
        sortingDirection: GetBoardPageSortingDirectionEnum,
        pageable: Pageable,
        searchTypeEnum: BoardController.GetBoardPageSearchTypeEnum?,
        searchKeyword: String?
    ): Page<FindPageAllFromBoardByNotDeletedOutputVo> {
        // Q 엔티티
        val sampleBoard = QDb1_RaillyLinkerCompany_SampleBoard.db1_RaillyLinkerCompany_SampleBoard
        val totalAuthMember = QDb1_RaillyLinkerCompany_TotalAuthMember.db1_RaillyLinkerCompany_TotalAuthMember

        // 동적 정렬 조건
        val orderBy = when (sortingType) {
            GetBoardPageSortingTypeEnum.CREATE_DATE -> sampleBoard.rowCreateDate
            GetBoardPageSortingTypeEnum.UPDATE_DATE -> sampleBoard.rowUpdateDate
            GetBoardPageSortingTypeEnum.TITLE -> sampleBoard.boardTitle
            GetBoardPageSortingTypeEnum.VIEW_COUNT -> sampleBoard.viewCount
            GetBoardPageSortingTypeEnum.WRITER_USER_NICKNAME -> totalAuthMember.accountId
        }

        val orderSpecifier = if (sortingDirection == GetBoardPageSortingDirectionEnum.ASC) {
            orderBy.asc()
        } else {
            orderBy.desc()
        }

        // 동적 검색 조건 생성
        val searchCondition: BooleanExpression? = if (searchKeyword == null) {
            null
        } else {
            when (searchTypeEnum) {
                BoardController.GetBoardPageSearchTypeEnum.WRITER -> totalAuthMember.accountId.containsIgnoreCase(
                    searchKeyword
                )

                BoardController.GetBoardPageSearchTypeEnum.TITLE -> sampleBoard.boardTitle.containsIgnoreCase(
                    searchKeyword
                )

                BoardController.GetBoardPageSearchTypeEnum.TITLE_OR_CONTENT -> sampleBoard.boardTitle.containsIgnoreCase(
                    searchKeyword
                )
                    .or(sampleBoard.boardContent.containsIgnoreCase(searchKeyword))

                null -> null
            }
        }

        // 기본 조건
        val baseCondition = sampleBoard.rowDeleteDateStr.eq("/")

        val query = queryFactory.select(
            Projections.bean(
                FindPageAllFromBoardByNotDeletedOutputVo::class.java,
                sampleBoard.uid.`as`("boardUid"),
                sampleBoard.boardTitle.`as`("title"),
                sampleBoard.rowCreateDate.`as`("createDate"),
                sampleBoard.rowUpdateDate.`as`("updateDate"),
                sampleBoard.viewCount.`as`("viewCount"),
                sampleBoard.totalAuthMember.uid.`as`("writerUserUid"),
                totalAuthMember.accountId.`as`("writerUserNickname")
            )
        )
            .from(sampleBoard)
            .innerJoin(totalAuthMember).on(
                totalAuthMember.rowDeleteDateStr.eq("/")
                    .and(totalAuthMember.eq(sampleBoard.totalAuthMember))
            )
            .where(baseCondition.and(searchCondition))
            .orderBy(orderSpecifier)

        // Pageable 처리
        val offset = pageable.pageNumber * pageable.pageSize.toLong()
        val limit = pageable.pageSize.toLong()

        val queryWithPagination = query.offset(offset).limit(limit)

        // 결과 가져오기
        val results = queryWithPagination.fetch()

        // 전체 데이터 수 가져오기
        val totalCount = queryFactory.select(sampleBoard.count())
            .from(sampleBoard)
            .innerJoin(totalAuthMember).on(
                totalAuthMember.rowDeleteDateStr.eq("/")
                    .and(totalAuthMember.eq(sampleBoard.totalAuthMember))
            )
            .where(baseCondition.and(searchCondition))
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, totalCount)
    }

    data class FindPageAllFromBoardByNotDeletedOutputVo(
        var boardUid: Long = 0L,
        var title: String = "",
        var createDate: LocalDateTime = LocalDateTime.now(),
        var updateDate: LocalDateTime = LocalDateTime.now(),
        var viewCount: Long = 0L,
        var writerUserUid: Long = 0L,
        var writerUserNickname: String = ""
    )
}