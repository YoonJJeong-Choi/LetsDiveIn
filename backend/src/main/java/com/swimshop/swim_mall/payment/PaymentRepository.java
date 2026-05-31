package com.swimshop.swim_mall.payment;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.swimshop.swim_mall.common.enums.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    
    /**
     * 주문 번호로 결제 조회
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.order.orderNo = :orderNo")
    Optional<PaymentEntity> findByOrderNo(@Param("orderNo") Long orderNo);
    
    /**
     * 결제 취소되지 않은 결제 조회
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentCancelYn = false")
    List<PaymentEntity> findNonCancelledPayments();
    
    /**
     * 기간별 결제 조회 (결제 취소되지 않은 것만)
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentCancelYn = false " +
           "AND p.paidAt >= :startDate AND p.paidAt < :endDate")
    List<PaymentEntity> findPaymentsByPeriod(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * 결제 완료된 결제 조회 (paidAt이 null이 아닌 것)
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.paymentCancelYn = false " +
           "AND p.paidAt IS NOT NULL")
    List<PaymentEntity> findCompletedPayments();

    @Query("SELECT COUNT(DISTINCT p.order.orderNo) FROM PaymentEntity p "
            + "WHERE p.paymentCancelYn = false AND p.paidAt IS NOT NULL AND p.order IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end")
    long countDistinctPaidOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM PaymentEntity p "
            + "WHERE p.paymentCancelYn = false AND p.paidAt IS NOT NULL AND p.order IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end")
    long sumPaidAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 결제 완료 건 목록 (대시보드 추이·비중 집계용)
     */
    @Query("SELECT p FROM PaymentEntity p "
            + "WHERE p.paymentCancelYn = false AND p.paidAt IS NOT NULL AND p.order IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end")
    List<PaymentEntity> findPaidPaymentsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 파트너 라인이 포함된 결제 완료 건(대시보드 추이용, DISTINCT payment)
     */
    @Query("SELECT DISTINCT p FROM PaymentEntity p "
            + "JOIN p.order o "
            + "JOIN o.orderItems oi "
            + "WHERE p.paymentCancelYn = false AND p.paidAt IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND oi.isCancelled = false "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId))")
    List<PaymentEntity> findPartnerRelatedPaidPaymentsBetween(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses);
}
