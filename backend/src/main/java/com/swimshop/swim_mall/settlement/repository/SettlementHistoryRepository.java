package com.swimshop.swim_mall.settlement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.settlement.entity.SettlementHistoryEntity;

@Repository
public interface SettlementHistoryRepository extends JpaRepository<SettlementHistoryEntity, Long> {
    
    /**
     * 정산별 변경 이력 조회 (최신순)
     */
    @Query("SELECT h FROM SettlementHistoryEntity h " +
           "JOIN FETCH h.admin a " +
           "WHERE h.settlement.settlementId = :settlementId " +
           "ORDER BY h.changedAt DESC")
    List<SettlementHistoryEntity> findBySettlementIdOrderByChangedAtDesc(
        @Param("settlementId") Long settlementId
    );
    
    /**
     * 정산별 변경 이력 조회 (최신순, 페이징)
     */
    @Query("SELECT h FROM SettlementHistoryEntity h " +
           "JOIN FETCH h.admin a " +
           "WHERE h.settlement.settlementId = :settlementId " +
           "ORDER BY h.changedAt DESC")
    List<SettlementHistoryEntity> findTopNBySettlementIdOrderByChangedAtDesc(
        @Param("settlementId") Long settlementId
    );
}
