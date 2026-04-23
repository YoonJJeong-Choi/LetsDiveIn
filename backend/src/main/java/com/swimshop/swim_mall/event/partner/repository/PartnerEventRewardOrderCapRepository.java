package com.swimshop.swim_mall.event.partner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.event.partner.entity.PartnerEventRewardOrderCapEntity;

@Repository
public interface PartnerEventRewardOrderCapRepository extends JpaRepository<PartnerEventRewardOrderCapEntity, Long> {

    boolean existsByEvent_EventNoAndCustomer_CustomerIdAndOrderNo(Long eventNo, Long customerId, Long orderNo);

    long countByEvent_EventNoAndCustomer_CustomerId(Long eventNo, Long customerId);
}
