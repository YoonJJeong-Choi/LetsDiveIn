package com.swimshop.swim_mall.ai.reviewanalysis.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertsDto {

    private boolean pii;
    private boolean spam;
    private boolean policyViolation;
    private List<String> policyTypes;
}
