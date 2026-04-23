package com.swimshop.swim_mall.order.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.common.enums.OrderItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 아이템 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {
    
    private Long orderItemNo; // 주문 아이템 번호
    private Long productNo; // 상품 번호
    private String productName; // 상품명
    private String productImageUrl; // 상품 이미지 URL
    private Long optionNo; // 옵션 번호 (nullable)
    private String color; // 색상 (nullable)
    private String size; // 사이즈 (nullable)
    private Integer quantity; // 수량
    private Long itemPrice; // 단위 가격
    private Long itemDiscountAmount; // 할인 금액
    private Long itemTotalPrice; // 총 가격 (수량 * 단위가격 - 할인금액)
    private Long appliedSalePolicyNo; // 적용 세일 정책 번호
    private String appliedSaleCampaignId; // 적용 세일 캠페인 ID
    private Long appliedSaleEventNo; // 적용 세일 연계 이벤트 번호
    private EventType appliedSaleEventType; // 적용 세일 연계 이벤트 타입
    private LocalDateTime saleEvaluatedAt; // 세일 판정 시각
    private Boolean isCancelled; // 취소 여부
    
    // 배송 정보 (nullable)
    private Long deliveryNo; // 배송 번호
    private DeliveryStatus deliveryStatus; // 배송 상태
    private LocalDateTime deliveryStartDate; // 배송 시작일
    private LocalDateTime deliveryEndDate; // 배송 완료일
    private String deliveryTrackingNumber; // 배송 송장번호
    private String deliveryCourier; // 배송 택배사
    
    // 반품 정보 (nullable)
    private Long returnNo; // 반품 번호
    private ReturnStatus returnStatus; // 반품 상태
    private LocalDateTime returnRequestedAt; // 반품 신청일시
    private String returnReason; // 반품 사유
    private Long returnAmount; // 반품 금액
    private String returnTrackingNumber; // 반품 송장번호
    private String returnCourier; // 반품 택배사
    private String rejectionReason; // 반품 거절 사유
    
    // 구매 확정 정보 (nullable)
    private LocalDateTime completedAt; // 구매 확정일시
    
    // 발주 확인 정보 (nullable)
    private LocalDateTime confirmedAt; // 발주 확인일시
    
    // 주문 상품 상태 (computed 필드, 여러 필드를 기반으로 계산됨)
    private OrderItemStatus status; // 주문 상품의 현재 상태
}
