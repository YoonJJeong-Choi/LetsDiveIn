package com.swimshop.swim_mall.ai.returnrisk.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRiskScoreResponseDto {

    private double score;
    private ReturnRiskLevel riskLevel;
    private String badgeText;
    private String guideText;
    private List<String> reasons;
}
