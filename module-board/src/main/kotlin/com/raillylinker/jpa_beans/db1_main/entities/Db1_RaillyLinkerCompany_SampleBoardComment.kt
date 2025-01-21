package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "sample_board_comment",
    catalog = "railly_linker_company"
)
@Comment("게시글 댓글 테이블")
class Db1_RaillyLinkerCompany_SampleBoardComment(
    @ManyToOne
    @JoinColumn(name = "total_auth_member_uid", nullable = false)
    @Comment("멤버 고유번호(railly_linker_company.total_auth_member.uid)")
    var totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,

    @ManyToOne
    @JoinColumn(name = "sample_board_uid", nullable = false)
    @Comment("게시글 고유번호(railly_linker_company.sample_board.uid)")
    var sampleBoard: Db1_RaillyLinkerCompany_SampleBoard,

    @ManyToOne
    @JoinColumn(name = "sample_board_comment_uid", nullable = true)
    @Comment("타겟 게시글 댓글 고유번호(railly_linker_company.sample_board_comment.uid)")
    var sampleBoardComment: Db1_RaillyLinkerCompany_SampleBoardComment?,

    @Column(name = "comment_content", nullable = false, columnDefinition = "TEXT")
    @Comment("댓글 본문")
    var commentContent: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid", columnDefinition = "BIGINT")
    @Comment("행 고유값")
    var uid: Long? = null

    @Column(name = "row_create_date", nullable = false, columnDefinition = "DATETIME(3)")
    @CreationTimestamp
    @Comment("행 생성일")
    var rowCreateDate: LocalDateTime? = null

    @Column(name = "row_update_date", nullable = false, columnDefinition = "DATETIME(3)")
    @UpdateTimestamp
    @Comment("행 수정일")
    var rowUpdateDate: LocalDateTime? = null

    @Column(name = "row_delete_date_str", nullable = false, columnDefinition = "VARCHAR(50)")
    @ColumnDefault("'/'")
    @Comment("행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)")
    var rowDeleteDateStr: String = "/"

    // ---------------------------------------------------------------------------------------------
    // [@OneToMany 변수들]
    @OneToMany(
        mappedBy = "sampleBoardComment",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL]
    )
    var sampleBoardCommentList: MutableList<Db1_RaillyLinkerCompany_SampleBoardComment> = mutableListOf()


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}