package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SettlementDetailResponseDto {
    
    private Long settlementId; // 정산 ID
    private Long partnerId; // 파트너 ID
    private String partnerName; // 파트너명
    private Long totalSalesAmount; // 총 판매금액
    private Long commissionAmount; // 총 수수료
    private Long settlementAmount; // 총 정산금액
    private LocalDate settlementCreatedAt; // 정산 생성일
    private String settlementStatus; // 정산 상태 (PENDING, COMPLETED, CANCELLED)
    private LocalDate settlementPeriodStart; // 정산 기간 시작일
    private LocalDate settlementPeriodEnd; // 정산 기간 종료일
    private LocalDate settlementPaidDate; // 정산 지급일
    private Integer itemCount; // 정산 처리된 주문 아이템 수
    private List<SettlementOrderItemDto> items; // 정산에 포함된 주문 아이템 목록
}
