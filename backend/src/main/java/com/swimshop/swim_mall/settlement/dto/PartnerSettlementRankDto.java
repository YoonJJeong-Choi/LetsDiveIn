package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PartnerSettlementRankDto {
    
    private Long partnerId;
    private String partnerName;
    private Long totalSettlementAmount; // 파트너별 총 정산 금액
    private Long totalSalesAmount; // 파트너별 총 판매 금액
    private Long totalCommissionAmount; // 파트너별 총 수수료
    private Integer settlementCount; // 파트너별 정산 건수
    private Integer rank; // 순위
}
