package com.swimshop.swim_mall.return_order.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.common.enums.ReturnStatus;

public interface ReturnRepository extends JpaRepository<ReturnEntity, Long> {

    long countByReturnStatusIn(Collection<ReturnStatus> statuses);

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

    @Query("SELECT COUNT(r) FROM ReturnEntity r "
            + "JOIN r.orderItem oi "
            + "LEFT JOIN oi.option opt "
            + "LEFT JOIN oi.product prod "
            + "LEFT JOIN opt.partner optPartner "
            + "LEFT JOIN prod.partner prodPartner "
            + "WHERE (optPartner.partnerId = :partnerId OR prodPartner.partnerId = :partnerId) "
            + "AND r.returnStatus IN :statuses")
    long countByPartnerIdAndReturnStatusIn(
            @Param("partnerId") Long partnerId,
            @Param("statuses") Collection<ReturnStatus> statuses);
    
    // 전체 반품 목록 조회 (관리자용)
    @Query("SELECT r FROM ReturnEntity r " +
           "JOIN FETCH r.orderItem oi " +
           "JOIN FETCH oi.order o " +
           "JOIN FETCH oi.product p")
    List<ReturnEntity> findAllWithRelations();
    
    // 반품 상태로 조회
    List<ReturnEntity> findByReturnStatus(ReturnStatus returnStatus);

    @Query("SELECT COUNT(r) FROM ReturnEntity r WHERE r.returnRequestedAt >= :start AND r.returnRequestedAt < :end")
    long countByReturnRequestedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM ReturnEntity r JOIN r.orderItem oi "
            + "LEFT JOIN oi.option opt LEFT JOIN oi.product prod "
            + "LEFT JOIN opt.partner optPartner LEFT JOIN prod.partner prodPartner "
            + "WHERE (optPartner.partnerId = :partnerId OR prodPartner.partnerId = :partnerId) "
            + "AND r.returnRequestedAt >= :start AND r.returnRequestedAt < :end")
    long countPartnerReturnsRequestedBetween(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT r.returnReasonType, COUNT(r) FROM ReturnEntity r JOIN r.orderItem oi "
            + "LEFT JOIN oi.option opt LEFT JOIN oi.product prod "
            + "LEFT JOIN opt.partner optPartner LEFT JOIN prod.partner prodPartner "
            + "WHERE (optPartner.partnerId = :partnerId OR prodPartner.partnerId = :partnerId) "
            + "AND r.returnRequestedAt >= :start AND r.returnRequestedAt < :end "
            + "GROUP BY r.returnReasonType")
    List<Object[]> countPartnerReturnsByReasonTypeBetween(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
