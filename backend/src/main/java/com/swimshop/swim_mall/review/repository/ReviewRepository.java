package com.swimshop.swim_mall.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swimshop.swim_mall.review.entity.ReviewEntity;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    
    // 주문 아이템 번호로 리뷰 조회 (중복 체크용)
    Optional<ReviewEntity> findByOrderItem_OrderItemNo(Long orderItemNo);
    
    // 상품 번호로 리뷰 목록 조회
    @Query("SELECT r FROM ReviewEntity r " +
           "JOIN FETCH r.product p " +
           "JOIN FETCH r.customer c " +
           "WHERE p.productNo = :productNo " +
           "ORDER BY r.reviewCreatedAt DESC")
    List<ReviewEntity> findByProductNo(@Param("productNo") Long productNo);
    
    // 고객 ID로 리뷰 목록 조회
    @Query("SELECT r FROM ReviewEntity r " +
           "JOIN FETCH r.product p " +
           "JOIN FETCH r.orderItem oi " +
           "WHERE r.customer.customerId = :customerId " +
           "ORDER BY r.reviewCreatedAt DESC")
    List<ReviewEntity> findByCustomerId(@Param("customerId") Long customerId);
    
    // 전체 리뷰 목록 조회 (관리자용)
    @Query("SELECT r FROM ReviewEntity r " +
           "JOIN FETCH r.product p " +
           "JOIN FETCH r.customer c " +
           "JOIN FETCH r.orderItem oi " +
           "ORDER BY r.reviewCreatedAt DESC")
    List<ReviewEntity> findAllWithRelations();
    
    // 파트너 ID로 리뷰 목록 조회 (파트너의 상품 리뷰만)
    @Query("SELECT r FROM ReviewEntity r " +
           "JOIN FETCH r.product p " +
           "JOIN FETCH r.customer c " +
           "JOIN FETCH r.orderItem oi " +
           "WHERE p.partner.partnerId = :partnerId " +
           "ORDER BY r.reviewCreatedAt DESC")
    List<ReviewEntity> findByPartnerId(@Param("partnerId") Long partnerId);
}
