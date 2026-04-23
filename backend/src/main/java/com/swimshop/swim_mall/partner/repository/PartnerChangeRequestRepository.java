package com.swimshop.swim_mall.partner.repository;

import com.swimshop.swim_mall.common.enums.PartnerChangeRequestStatus;
import com.swimshop.swim_mall.partner.entity.PartnerChangeRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerChangeRequestRepository extends JpaRepository<PartnerChangeRequestEntity, Long> {
    List<PartnerChangeRequestEntity> findByPartner_PartnerIdOrderByCreatedAtDesc(Long partnerId);
    List<PartnerChangeRequestEntity> findByStatusOrderByCreatedAtDesc(PartnerChangeRequestStatus status);
}
