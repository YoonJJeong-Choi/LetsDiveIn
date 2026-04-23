package com.swimshop.swim_mall.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SettlementListResponseDto {
    
    private List<SettlementDetailResponseDto> settlements; // 생성된 정산 목록
    private Integer totalCount; // 전체 정산 수
}
