package com.swimshop.swim_mall.ai.returnrisk.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRiskScoreRequestDto {

    private Long productNo;
    private Long optionNo;
    private Double productReturnRate30d;
    private Double optionReturnRate30d;
    private Integer optionChangeCount;
    private Integer reviewViewCount;
    private Boolean sizeGuideClicked;
}
