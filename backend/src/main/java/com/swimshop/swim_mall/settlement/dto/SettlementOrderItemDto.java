package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettlementOrderItemDto {
    
    private Long orderItemId; // 주문 아이템 ID
    private Long orderId; // 주문 ID
    private String productName; // 상품명
    private Integer quantity; // 수량
    private Long salesAmount; // 판매금액
    private Long commissionAmount; // 수수료
    private Long settlementAmount; // 정산금액
    private LocalDateTime orderDate; // 주문일
    private LocalDateTime deliveryCompletedDate; // 배송완료일
}
