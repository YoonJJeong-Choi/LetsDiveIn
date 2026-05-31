package com.swimshop.swim_mall.settlement.repository;

import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.settlement.entity.SettlementOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementOrderItemRepository extends JpaRepository<SettlementOrderItemEntity, Long> {
    
    /**
     * 정산에 포함된 주문 아이템 목록 조회
     */
    List<SettlementOrderItemEntity> findBySettlement_SettlementId(Long settlementId);

    /**
     * 비취소 정산에 이미 포함된 주문 상품인지 (정산 대상 JPQL과 동일한 기준)
     */
    @Query("""
            SELECT CASE WHEN COUNT(soi) > 0 THEN true ELSE false END
            FROM SettlementOrderItemEntity soi
            WHERE soi.orderItem.orderItemNo = :orderItemNo
              AND soi.settlement.settlementStatus <> :cancelled
            """)
    boolean existsLinkToNonCancelledSettlement(
            @Param("orderItemNo") Long orderItemNo,
            @Param("cancelled") SettlementStatus cancelled
    );
}
