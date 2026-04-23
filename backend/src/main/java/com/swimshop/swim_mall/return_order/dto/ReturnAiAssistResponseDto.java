package com.swimshop.swim_mall.return_order.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnAiAssistResponseDto {
    private boolean featureEnabled;

    private int fraudScore; // 0~100
    private String riskLevel; // LOW | MEDIUM | HIGH
    private List<String> riskFactors;

    private List<String> evidenceTags;
    private List<String> evidenceGaps;

    private List<String> recommendedActions;
}
