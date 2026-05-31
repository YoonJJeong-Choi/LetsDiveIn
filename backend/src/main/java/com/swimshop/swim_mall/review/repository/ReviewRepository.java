package com.swimshop.swim_mall.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
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

    /**
     * 파트너 소유 상품의 리뷰만, 옵션/기간 조건을 DB에서 먼저 적용한 뒤 최신순 상한 건수까지 조회합니다.
     * AI 분석 후보 조회용이며 Pageable과 호환되도록 FETCH JOIN은 사용하지 않습니다.
     */
    @Query("SELECT r FROM ReviewEntity r " +
           "LEFT JOIN r.orderItem oi " +
           "LEFT JOIN oi.option opt " +
           "WHERE r.product.partner.partnerId = :partnerId " +
           "AND r.product.productNo = :productNo " +
           "AND r.reviewCreatedAt >= COALESCE(:fromAt, r.reviewCreatedAt) " +
           "AND r.reviewCreatedAt <= COALESCE(:toAt, r.reviewCreatedAt) " +
           "AND (:optionFilterDisabled = true OR opt.optionNo IN :optionNos) " +
           "ORDER BY r.reviewCreatedAt DESC")
    List<ReviewEntity> findPartnerAiCandidates(
            @Param("partnerId") Long partnerId,
            @Param("productNo") Long productNo,
            @Param("fromAt") java.time.LocalDateTime fromAt,
            @Param("toAt") java.time.LocalDateTime toAt,
            @Param("optionFilterDisabled") boolean optionFilterDisabled,
            @Param("optionNos") List<Long> optionNos,
            Pageable pageable);
}
