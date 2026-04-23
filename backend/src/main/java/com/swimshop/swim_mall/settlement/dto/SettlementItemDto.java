package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SettlementItemDto {
    
    private Long orderId; // 주문 번호
    private Long orderItemId; // 주문 아이템 번호
    private String productName; // 상품명
    private Integer quantity; // 수량
    private Long salesAmount; // 판매금액 (itemTotalPrice)
    private Long commissionAmount; // 수수료 (판매금액 × 10%)
    private Long settlementAmount; // 정산금액 (판매금액 - 수수료)
    private LocalDateTime orderDate; // 주문일
    private LocalDateTime deliveryCompletedDate; // 배송 완료일
    private Boolean isSettlementReady; // 정산 가능 여부
}
