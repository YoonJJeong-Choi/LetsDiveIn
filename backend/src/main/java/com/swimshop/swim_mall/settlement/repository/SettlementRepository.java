package com.swimshop.swim_mall.settlement.repository;

import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.settlement.entity.SettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<SettlementEntity, Long> {
    
    /**
     * 파트너별 정산 목록 조회
     */
    List<SettlementEntity> findByPartner_PartnerId(Long partnerId);

    long countByPartner_PartnerIdAndSettlementStatus(Long partnerId, SettlementStatus settlementStatus);

    @Query("SELECT COALESCE(SUM(s.settlementAmount), 0) FROM SettlementEntity s "
            + "WHERE s.partner.partnerId = :partnerId AND s.settlementStatus = :status")
    long sumSettlementAmountByPartnerAndStatus(
            @Param("partnerId") Long partnerId,
            @Param("status") SettlementStatus status);

    long countBySettlementStatus(SettlementStatus settlementStatus);

    @Query("SELECT COALESCE(SUM(s.settlementAmount), 0) FROM SettlementEntity s "
            + "WHERE s.settlementStatus = :status")
    long sumSettlementAmountByStatus(@Param("status") SettlementStatus status);

    long countBySettlementStatusAndSettlementCreatedAtBetween(
            SettlementStatus settlementStatus,
            LocalDate startInclusive,
            LocalDate endInclusive);

    @Query("SELECT COALESCE(SUM(s.settlementAmount), 0) FROM SettlementEntity s "
            + "WHERE s.settlementStatus = :status "
            + "AND s.settlementCreatedAt >= :start AND s.settlementCreatedAt <= :end")
    long sumSettlementAmountByStatusAndCreatedBetween(
            @Param("status") SettlementStatus status,
            @Param("start") LocalDate startInclusive,
            @Param("end") LocalDate endInclusive);

    @Query("SELECT COUNT(s) FROM SettlementEntity s "
            + "WHERE s.settlementStatus = :status "
            + "AND s.settlementPeriodEnd IS NOT NULL "
            + "AND s.settlementPeriodEnd >= :start AND s.settlementPeriodEnd <= :end")
    long countPendingWithPeriodEndBetween(
            @Param("status") SettlementStatus status,
            @Param("start") LocalDate startInclusive,
            @Param("end") LocalDate endInclusive);

    @Query("SELECT COALESCE(SUM(s.settlementAmount), 0) FROM SettlementEntity s "
            + "WHERE s.settlementStatus = :status "
            + "AND s.settlementPeriodEnd IS NOT NULL "
            + "AND s.settlementPeriodEnd >= :start AND s.settlementPeriodEnd <= :end")
    long sumPendingAmountWithPeriodEndBetween(
            @Param("status") SettlementStatus status,
            @Param("start") LocalDate startInclusive,
            @Param("end") LocalDate endInclusive);
}
