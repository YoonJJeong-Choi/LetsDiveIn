package com.swimshop.swim_mall.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.order.entity.OrderEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    
    // 고객별 주문 목록 조회
    List<OrderEntity> findByCustomerOrderByOrderCreatedAtDesc(CustomerEntity customer);
    
    // 주문 번호로 조회
    Optional<OrderEntity> findByOrderNo(Long orderNo);
    
    // 주문 번호로 조회 (OrderItems, Delivery, Return 포함)
    @Query("SELECT DISTINCT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer c " +
           "LEFT JOIN FETCH o.payment p " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product prod " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.delivery d " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE o.orderNo = :orderNo")
    Optional<OrderEntity> findByOrderNoWithRelations(@Param("orderNo") Long orderNo);
    
    // 관리자용: 모든 주문 조회 (고객, 주문 아이템, 결제 정보 포함)
    @Query("SELECT DISTINCT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer c " +
           "LEFT JOIN FETCH o.payment p " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product prod " +
           "LEFT JOIN FETCH oi.option opt " +
           "ORDER BY o.orderCreatedAt DESC")
    List<OrderEntity> findAllWithRelations();
    
    // 파트너용: 파트너의 상품이 포함된 주문만 조회
    @Query("SELECT DISTINCT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer c " +
           "LEFT JOIN FETCH o.payment p " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product prod " +
           "LEFT JOIN FETCH prod.partner prodPartner " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH opt.partner optPartner " +
           "WHERE (opt IS NOT NULL AND optPartner.partnerId = :partnerId) " +
           "   OR (opt IS NULL AND prodPartner.partnerId = :partnerId) " +
           "ORDER BY o.orderCreatedAt DESC")
    List<OrderEntity> findByPartnerId(@Param("partnerId") Long partnerId);
}
