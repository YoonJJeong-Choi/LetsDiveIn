package com.swimshop.swim_mall.partner.repository;

import com.swimshop.swim_mall.partner.entity.PartnerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerProfileRepository extends JpaRepository<PartnerProfileEntity, Long> {
    Optional<PartnerProfileEntity> findByPartner_PartnerId(Long partnerId);
}
