package com.swimshop.swim_mall.payment.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    
    private Long paymentNo; // paymentId
    private Long orderNo; // orderId
    private Long paymentAmount; // amount
    private String paymentMethod;
    private String paymentStatus; // APPROVED, FAILED, CANCELED, PENDING (OrderStatus에서 파생)
    private OrderStatus orderStatus; // 주문 상태
    private LocalDateTime paymentCreatedAt;
    private LocalDateTime paidAt; // 결제 승인 시점
    private Boolean paymentCancelYn;
}
