package com.swimshop.swim_mall.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
}
