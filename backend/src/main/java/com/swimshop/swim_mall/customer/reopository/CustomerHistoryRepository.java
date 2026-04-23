package com.swimshop.swim_mall.customer.reopository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerHistoryEntity;

@Repository
public interface CustomerHistoryRepository extends JpaRepository<CustomerHistoryEntity, Long> {
    
    /**
     * 고객별 작업 이력 조회 (최신순)
     */
    @Query("SELECT h FROM CustomerHistoryEntity h " +
           "LEFT JOIN FETCH h.admin a " +
           "WHERE h.customer.customerId = :customerId " +
           "ORDER BY h.createdAt DESC")
    List<CustomerHistoryEntity> findByCustomer_CustomerIdOrderByCreatedAtDesc(
        @Param("customerId") Long customerId
    );
}
