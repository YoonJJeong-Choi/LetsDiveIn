package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlySettlementDto {
    
    private String yearMonth; // YYYY-MM 형식
    private Long totalSettlementAmount; // 월별 총 정산 금액
    private Long totalSalesAmount; // 월별 총 판매 금액
    private Long totalCommissionAmount; // 월별 총 수수료
    private Integer settlementCount; // 월별 정산 건수
}
