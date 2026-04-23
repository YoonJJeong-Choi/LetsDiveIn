package com.swimshop.swim_mall.partner.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.PartnerHistoryActionType;
import com.swimshop.swim_mall.partner.entity.PartnerHistoryEntity;

@Repository
public interface PartnerHistoryRepository extends JpaRepository<PartnerHistoryEntity, Long> {
    
    /**
     * 파트너의 모든 이력 조회 (시간순 정렬)
     */
    List<PartnerHistoryEntity> findByPartner_PartnerIdOrderByCreatedAtDesc(Long partnerId);
    
    /**
     * 특정 액션 타입의 이력 조회
     */
    List<PartnerHistoryEntity> findByPartner_PartnerIdAndActionTypeOrderByCreatedAtDesc(
        Long partnerId, PartnerHistoryActionType actionType);
}
