package com.raillylinker.kafka_components.consumers

import com.google.gson.Gson
import com.raillylinker.configurations.jpa_configs.Db1MainConfig
import com.raillylinker.configurations.kafka_configs.Kafka1MainConfig
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_SampleBoardComment
import com.raillylinker.jpa_beans.db1_main.repositories.Db1_RaillyLinkerCompany_SampleBoardComment_Repository
import com.raillylinker.jpa_beans.db1_main.repositories.Db1_RaillyLinkerCompany_SampleBoard_Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class Kafka1MainConsumer(
    private val db1RaillyLinkerCompanySampleBoardRepository: Db1_RaillyLinkerCompany_SampleBoard_Repository,
    private val db1RaillyLinkerCompanySampleBoardCommentRepository: Db1_RaillyLinkerCompany_SampleBoardComment_Repository
) {
    // <멤버 변수 공간>
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        // !!!모듈 컨슈머 그룹 아이디!!!
        private const val CONSUMER_GROUP_ID = "com.raillylinker.service_board"
    }

    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (Auth 모듈의 통합 멤버 정보 삭제 이벤트에 대한 리스너)
    // 이와 연관된 데이터 삭제 및 기타 처리
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    @KafkaListener(
        topics = ["from_auth_db_delete_from_railly_linker_company_total_auth_member"],
        groupId = CONSUMER_GROUP_ID,
        containerFactory = Kafka1MainConfig.CONSUMER_BEAN_NAME
    )
    fun fromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMemberListener(data: ConsumerRecord<String, String>) {
        classLogger.info(
            """
                KafkaConsumerLog>>
                {
                    "data" : {
                        "$data"
                    }
                }
            """.trimIndent()
        )

        val inputVo = Gson().fromJson(
            data.value(),
            FromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMemberListenerInputVo::class.java
        )

        val db1RaillyLinkerCompanySampleBoardList =
            db1RaillyLinkerCompanySampleBoardRepository.findAllByTotalAuthMemberUidAndRowDeleteDateStr(
                inputVo.deletedMemberUid,
                "/"
            )

        for (db1RaillyLinkerCompanySampleBoard in db1RaillyLinkerCompanySampleBoardList) {
            // 재귀적으로 댓글과 하위 댓글 삭제
            db1RaillyLinkerCompanySampleBoard.sampleBoardCommentList.forEach { comment ->
                deleteCommentsRecursively(comment)
            }

            // 삭제된 멤버 정보와 연관된 게시판 정보 삭제 처리
            db1RaillyLinkerCompanySampleBoard.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))

            db1RaillyLinkerCompanySampleBoardRepository.save(db1RaillyLinkerCompanySampleBoard)
        }
    }

    data class FromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMemberListenerInputVo(
        val deletedMemberUid: Long
    )

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
}