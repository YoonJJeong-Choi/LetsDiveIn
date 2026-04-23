package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementSummaryDto {
    
    private Long totalSalesAmount; // 총 판매금액
    private Long totalCommissionAmount; // 총 수수료
    private Long totalSettlementAmount; // 총 정산금액
    private Integer settlementReadyCount; // 정산 대상 건수
}
