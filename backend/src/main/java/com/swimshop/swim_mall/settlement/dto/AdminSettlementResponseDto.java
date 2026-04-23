package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminSettlementResponseDto {
    
    private List<AdminSettlementItemDto> items; // 정산 대상 목록
    private SettlementSummaryDto summary; // 합계 정보
    private Integer totalPartnerCount; // 파트너 수 (전체 조회 시)
}
