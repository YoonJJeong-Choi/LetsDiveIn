package com.swimshop.swim_mall.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.swimshop.swim_mall.common.enums.OrderItemStatus;
import com.swimshop.swim_mall.common.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    
    private Long orderNo; // 주문 번호
    private Long orderTotalPrice; // 총 주문 금액
    private Long usedPointAmount; // 주문 시 사용 포인트 스냅샷
    private LocalDateTime orderCreatedAt; // 주문 생성일시
    private OrderStatus orderStatus; // 주문 상태
    private OrderItemStatus orderDisplayStatus; // 주문 표시 상태 (주문상품 상태 기반)
    private String orderDisplayStatusLabel; // 주문 표시 상태 한글 라벨
    // 주의: 구매 확정은 OrderItem.completedAt으로 관리합니다.
    private String recipientName; // 수령인 이름
    private String recipientPhone; // 수령인 전화번호
    private String deliveryAddress; // 배송지 주소
    private String deliveryAddressDetail; // 배송지 상세 주소
    private String deliveryZipCode; // 우편번호
    private String orderMemo; // 주문 메모
    private List<OrderItemResponseDto> orderItems; // 주문 아이템 목록
    
    // 결제 정보
    private Long paymentNo; // 결제 번호
    private String paymentMethod; // 결제 방법
    private Long paymentAmount; // 결제 금액
    private LocalDateTime paymentCreatedAt; // 결제 생성일시
    private LocalDateTime paidAt; // 결제 승인일시
    private Boolean paymentCancelYn; // 결제 취소 여부
}
