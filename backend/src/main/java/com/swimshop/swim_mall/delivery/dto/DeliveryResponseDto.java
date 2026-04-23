package com.swimshop.swim_mall.delivery.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.DeliveryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponseDto {
    
    private Long deliveryNo; // 배송 번호
    private Long orderItemNo; // 주문 아이템 번호
    private Long orderNo; // 주문 번호
    
    // 상품 정보
    private Long productNo; // 상품 번호
    private String productName; // 상품명
    private String productImageUrl; // 상품 이미지 URL
    private Long optionNo; // 옵션 번호 (nullable)
    private String color; // 색상 (nullable)
    private String size; // 사이즈 (nullable)
    private Integer quantity; // 주문 수량
    private Long itemPrice; // 단위 가격
    private Long itemTotalPrice; // 총 가격 (수량 * 단위가격 - 할인금액)
    
    // 배송 정보
    private DeliveryStatus deliveryStatus; // 배송 상태
    private LocalDateTime deliveryStartDate; // 배송 시작일
    private LocalDateTime deliveryEndDate; // 배송 완료일
    private String deliveryTrackingNumber; // 배송 송장번호
    private String deliveryCourier; // 배송 택배사
}
