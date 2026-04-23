package com.swimshop.swim_mall.order.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {
    
    @NotEmpty(message = "장바구니 아이템이 필요합니다")
    private List<Long> cartItemNos; // 주문할 장바구니 아이템 번호 리스트
    
    // 배송지 정보
    @NotNull(message = "수령인 이름이 필요합니다")
    private String recipientName; // 수령인 이름
    
    @NotNull(message = "수령인 전화번호가 필요합니다")
    private String recipientPhone; // 수령인 전화번호
    
    @NotNull(message = "배송지 주소가 필요합니다")
    private String deliveryAddress; // 배송지 주소
    
    private String deliveryAddressDetail; // 배송지 상세 주소 (선택)
    
    private String deliveryZipCode; // 우편번호 (선택)
    
    // 결제 정보
    @NotNull(message = "결제 방법이 필요합니다")
    private String paymentMethod; // 결제 방법 (예: "CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT")

    // 포인트 사용 금액 (선택)
    // null 또는 0이면 포인트 사용 없음
    private Long usePointAmount;
    
    // 주문 메모 (선택)
    private String orderMemo; // 주문 메모
}
