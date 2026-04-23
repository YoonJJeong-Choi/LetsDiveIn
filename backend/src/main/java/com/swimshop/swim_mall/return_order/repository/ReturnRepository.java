package com.swimshop.swim_mall.return_order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.common.enums.ReturnStatus;

public interface ReturnRepository extends JpaRepository<ReturnEntity, Long> {
    
    // 주문 아이템 번호로 반품 조회
    Optional<ReturnEntity> findByOrderItem_OrderItemNo(Long orderItemNo);
    
    // 주문 번호로 반품 목록 조회
    @Query("SELECT r FROM ReturnEntity r " +
           "JOIN FETCH r.orderItem oi " +
           "JOIN FETCH oi.order o " +
           "WHERE o.orderNo = :orderNo")
    List<ReturnEntity> findByOrderNo(@Param("orderNo") Long orderNo);
    
    // 고객 ID로 반품 목록 조회
    @Query("SELECT r FROM ReturnEntity r " +
           "JOIN FETCH r.orderItem oi " +
           "JOIN FETCH oi.order o " +
           "JOIN FETCH o.customer c " +
           "WHERE c.customerId = :customerId")
    List<ReturnEntity> findByCustomerId(@Param("customerId") Long customerId);
    
    // 파트너 ID로 반품 목록 조회 (파트너의 상품에 대한 반품만)
    @Query("SELECT DISTINCT r FROM ReturnEntity r " +
           "JOIN FETCH r.orderItem oi " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.product prod " +
           "LEFT JOIN FETCH opt.partner optPartner " +
           "LEFT JOIN FETCH prod.partner prodPartner " +
           "WHERE (optPartner.partnerId = :partnerId OR prodPartner.partnerId = :partnerId)")
    List<ReturnEntity> findByPartnerId(@Param("partnerId") Long partnerId);
    
    // 전체 반품 목록 조회 (관리자용)
    @Query("SELECT r FROM ReturnEntity r " +
           "JOIN FETCH r.orderItem oi " +
           "JOIN FETCH oi.order o " +
           "JOIN FETCH oi.product p")
    List<ReturnEntity> findAllWithRelations();
    
    // 반품 상태로 조회
    List<ReturnEntity> findByReturnStatus(ReturnStatus returnStatus);

    @Query("SELECT COUNT(r) FROM ReturnEntity r JOIN r.orderItem oi JOIN oi.order o WHERE o.customer.customerId = :customerId AND r.returnRequestedAt >= :since")
    long countByCustomerRequestedSince(@Param("customerId") Long customerId, @Param("since") LocalDateTime since);
}
