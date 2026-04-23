package com.swimshop.swim_mall.event.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.event.admin.entity.AdminEventRewardOrderCapEntity;

@Repository
public interface AdminEventRewardOrderCapRepository extends JpaRepository<AdminEventRewardOrderCapEntity, Long> {

    boolean existsByEvent_EventNoAndCustomer_CustomerIdAndOrderNo(Long eventNo, Long customerId, Long orderNo);

    long countByEvent_EventNoAndCustomer_CustomerId(Long eventNo, Long customerId);
}
