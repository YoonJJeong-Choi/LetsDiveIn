package com.swimshop.swim_mall.settlement.repository;

import com.swimshop.swim_mall.settlement.entity.SettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<SettlementEntity, Long> {
    
    /**
     * 파트너별 정산 목록 조회
     */
    List<SettlementEntity> findByPartner_PartnerId(Long partnerId);
}
