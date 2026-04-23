package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SettlementDashboardDto {
    
    // 전체 정산 현황 통계
    private Long totalSettlementCount; // 전체 정산 건수
    private Long pendingSettlementCount; // 대기 중인 정산 건수
    private Long completedSettlementCount; // 완료된 정산 건수
    private Long totalSettlementAmount; // 전체 정산 금액 합계
    private Long totalSalesAmount; // 전체 판매 금액 합계
    private Long totalCommissionAmount; // 전체 수수료 합계
    
    // 파트너별 정산 금액 순위
    private List<PartnerSettlementRankDto> partnerRankings;
    
    // 월별 정산 현황
    private List<MonthlySettlementDto> monthlySettlements;
}
