package com.swimshop.swim_mall.partner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

    long countByPartnerStatus(PartnerStatus partnerStatus);
    
    /**
     * Account ID로 파트너 조회
     */
    Optional<PartnerEntity> findByAccount_AccountId(Long accountId);
}
