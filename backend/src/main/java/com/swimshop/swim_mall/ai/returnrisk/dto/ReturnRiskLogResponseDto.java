package com.swimshop.swim_mall.ai.returnrisk.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRiskLogResponseDto {

    private LocalDateTime createdAt;
    private Long productNo;
    private Long optionNo;
    private double score;
    private String riskLevel;
    private boolean success;
    private long latencyMs;
    private String model;
    private String errorMessage;
}
