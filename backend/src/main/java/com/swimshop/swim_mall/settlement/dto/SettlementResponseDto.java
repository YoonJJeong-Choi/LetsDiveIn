package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 파트너 정산 대상 조회 응답 DTO
 */
@Getter
@Builder
public class SettlementResponseDto {
    
    private List<SettlementItemDto> items; // 정산 대상 주문 아이템 목록
    private SettlementSummaryDto summary; // 정산 합계 정보
}
