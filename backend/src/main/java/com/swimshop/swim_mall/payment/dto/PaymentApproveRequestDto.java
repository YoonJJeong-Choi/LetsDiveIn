package com.swimshop.swim_mall.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 승인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApproveRequestDto {
    
    private Long orderNo; // 주문 번호
    private String paymentId; // 결제 ID (PG사에서 발급, 테스트용으로는 주문번호 사용 가능)
    private String paymentKey; // 결제 키 (PG사에서 발급, 테스트용으로는 임의 값 사용 가능)
    private Long paymentAmount; // 결제 금액 (클라이언트가 보낸 금액, 선택적 - 보내면 서버 금액과 검증)
}
