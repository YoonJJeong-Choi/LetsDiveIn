package com.swimshop.swim_mall.settlement.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class SettlementCreateRequestDto {
    
    private Long partnerId; // 파트너 ID
    private List<Long> orderItemIds; // 정산 처리할 주문 아이템 ID 목록
    private LocalDate settlementPeriodStart; // 정산 기간 시작일 (선택)
    private LocalDate settlementPeriodEnd; // 정산 기간 종료일 (선택)
}
