package com.swimshop.swim_mall.settlement.repository;

import com.swimshop.swim_mall.settlement.entity.SettlementOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementOrderItemRepository extends JpaRepository<SettlementOrderItemEntity, Long> {
    
    /**
     * 정산에 포함된 주문 아이템 목록 조회
     */
    List<SettlementOrderItemEntity> findBySettlement_SettlementId(Long settlementId);
}
