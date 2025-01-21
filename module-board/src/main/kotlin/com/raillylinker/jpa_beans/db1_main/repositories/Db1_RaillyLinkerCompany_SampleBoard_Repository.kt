package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_SampleBoard
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// (JPA 레포지토리)
// : 함수 작성 명명법에 따라 데이터베이스 SQL 동작을 자동지원
@Repository
interface Db1_RaillyLinkerCompany_SampleBoard_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_SampleBoard, Long> {
    fun findByUidAndRowDeleteDateStr(
        uid: Long,
        rowDeleteDateStr: String
    ): Db1_RaillyLinkerCompany_SampleBoard?

    fun findByUidAndTotalAuthMemberAndRowDeleteDateStr(
        uid: Long,
        totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,
        rowDeleteDateStr: String
    ): Db1_RaillyLinkerCompany_SampleBoard?

    fun findAllByTotalAuthMemberUidAndRowDeleteDateStr(
        totalAuthMemberUid: Long,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_SampleBoard>
}