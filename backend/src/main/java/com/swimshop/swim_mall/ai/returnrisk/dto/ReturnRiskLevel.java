package com.swimshop.swim_mall.ai.returnrisk.dto;

public enum ReturnRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN;

    public static ReturnRiskLevel from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("riskLevel is required");
        }
        return ReturnRiskLevel.valueOf(value.trim().toUpperCase());
    }
}
