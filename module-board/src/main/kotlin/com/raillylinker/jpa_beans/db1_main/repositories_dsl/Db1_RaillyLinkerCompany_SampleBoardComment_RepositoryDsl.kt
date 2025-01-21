package com.raillylinker.jpa_beans.db1_main.repositories_dsl

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.raillylinker.jpa_beans.db1_main.entities.QDb1_RaillyLinkerCompany_SampleBoardComment
import com.raillylinker.jpa_beans.db1_main.entities.QDb1_RaillyLinkerCompany_TotalAuthMember
import com.raillylinker.jpa_beans.db1_main.entities.QDb1_RaillyLinkerCompany_TotalAuthMemberProfile
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

@Repository
class Db1_RaillyLinkerCompany_SampleBoardComment_RepositoryDsl(entityManager: EntityManager) {
    private val queryFactory: JPAQueryFactory = JPAQueryFactory(entityManager)


    // ----
    // (railly_linker_company.sample_board 테이블 페이징)
    fun findPageAllFromBoardCommentByNotDeleted(
        boardUid: Long,
        commentUid: Long?,
        pageable: Pageable
    ): Page<FindPageAllFromBoardCommentByNotDeletedOutputVo> {
        // Q 엔티티
        val sampleBoardComment = QDb1_RaillyLinkerCompany_SampleBoardComment.db1_RaillyLinkerCompany_SampleBoardComment
        val totalAuthMember = QDb1_RaillyLinkerCompany_TotalAuthMember.db1_RaillyLinkerCompany_TotalAuthMember
        val totalAuthMemberProfile =
            QDb1_RaillyLinkerCompany_TotalAuthMemberProfile.db1_RaillyLinkerCompany_TotalAuthMemberProfile

        // 동적 정렬 조건
        val orderBy = if (commentUid == null) {
            sampleBoardComment.rowCreateDate.desc()
        } else {
            sampleBoardComment.rowCreateDate.asc()
        }

        val query = queryFactory.select(
            Projections.bean(
                FindPageAllFromBoardCommentByNotDeletedOutputVo::class.java,
                sampleBoardComment.uid.`as`("commentUid"),
                sampleBoardComment.commentContent.`as`("content"),
                sampleBoardComment.rowCreateDate.`as`("createDate"),
                sampleBoardComment.rowUpdateDate.`as`("updateDate"),
                sampleBoardComment.totalAuthMember.uid.`as`("writerUserUid"),
                totalAuthMember.accountId.`as`("writerUserNickname"),
                totalAuthMemberProfile.imageFullUrl.`as`("writerUserProfileFullUrl")
            )
        )
            .from(sampleBoardComment)
            .innerJoin(totalAuthMember).on(
                totalAuthMember.rowDeleteDateStr.eq("/")
                    .and(totalAuthMember.eq(sampleBoardComment.totalAuthMember))
            )
            .leftJoin(totalAuthMemberProfile).on(
                totalAuthMemberProfile.rowDeleteDateStr.eq("/")
                    .and(totalAuthMemberProfile.eq(totalAuthMember.frontTotalAuthMemberProfile))
            )
            .where(
                sampleBoardComment.rowDeleteDateStr.eq("/"),
                if (commentUid == null) {
                    sampleBoardComment.sampleBoard.uid.eq(boardUid)
                } else {
                    sampleBoardComment.sampleBoard.uid.eq(boardUid).and(
                        sampleBoardComment.sampleBoardComment.rowDeleteDateStr.eq("/")
                            .and(sampleBoardComment.sampleBoardComment.uid.eq(commentUid))
                    )
                }
            )
            .orderBy(orderBy)

        // Pageable 처리
        val offset = pageable.pageNumber * pageable.pageSize.toLong()
        val limit = pageable.pageSize.toLong()

        val queryWithPagination = query.offset(offset).limit(limit)

        // 결과 가져오기
        val results = queryWithPagination.fetch()

        val countQuery = queryFactory.select(sampleBoardComment.count())
            .from(sampleBoardComment)
            .innerJoin(totalAuthMember).on(
                totalAuthMember.rowDeleteDateStr.eq("/")
                    .and(totalAuthMember.eq(sampleBoardComment.totalAuthMember))
            )
            .where(
                sampleBoardComment.rowDeleteDateStr.eq("/"),
                if (commentUid == null) {
                    sampleBoardComment.sampleBoard.uid.eq(boardUid)
                } else {
                    sampleBoardComment.sampleBoard.uid.eq(boardUid).and(
                        sampleBoardComment.sampleBoardComment.rowDeleteDateStr.eq("/")
                            .and(sampleBoardComment.sampleBoardComment.uid.eq(commentUid))
                    )
                }
            )

        val total = countQuery.fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    data class FindPageAllFromBoardCommentByNotDeletedOutputVo(
        var commentUid: Long = 0L,
        var content: String = "",
        var createDate: LocalDateTime = LocalDateTime.now(),
        var updateDate: LocalDateTime = LocalDateTime.now(),
        var writerUserUid: Long = 0L,
        var writerUserNickname: String = "",
        var writerUserProfileFullUrl: String? = null
    )
}