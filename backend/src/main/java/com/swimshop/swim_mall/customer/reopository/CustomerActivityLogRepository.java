package com.swimshop.swim_mall.customer.reopository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerActivityLogEntity;
import com.swimshop.swim_mall.common.enums.CustomerActivityType;

@Repository
public interface CustomerActivityLogRepository extends JpaRepository<CustomerActivityLogEntity, Long> {
    
    /**
     * 고객별 활동 로그 조회 (최신순)
     */
    @Query("SELECT l FROM CustomerActivityLogEntity l " +
           "WHERE l.customer.customerId = :customerId " +
           "ORDER BY l.activityAt DESC")
    List<CustomerActivityLogEntity> findByCustomer_CustomerIdOrderByActivityAtDesc(
        @Param("customerId") Long customerId
    );
    
    /**
     * 고객별 활동 로그 조회 (페이징)
     */
    @Query("SELECT l FROM CustomerActivityLogEntity l " +
           "WHERE l.customer.customerId = :customerId " +
           "ORDER BY l.activityAt DESC")
    Page<CustomerActivityLogEntity> findByCustomer_CustomerIdOrderByActivityAtDesc(
        @Param("customerId") Long customerId,
        Pageable pageable
    );
    
    /**
     * 고객별 특정 활동 유형 로그 조회
     */
    @Query("SELECT l FROM CustomerActivityLogEntity l " +
           "WHERE l.customer.customerId = :customerId " +
           "AND l.activityType = :activityType " +
           "ORDER BY l.activityAt DESC")
    List<CustomerActivityLogEntity> findByCustomer_CustomerIdAndActivityTypeOrderByActivityAtDesc(
        @Param("customerId") Long customerId,
        @Param("activityType") CustomerActivityType activityType
    );
}
