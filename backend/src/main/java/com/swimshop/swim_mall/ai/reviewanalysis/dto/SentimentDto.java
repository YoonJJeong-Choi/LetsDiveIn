package com.swimshop.swim_mall.ai.reviewanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentDto {

    private double positiveRatio;
    private double negativeRatio;
    private double neutralRatio;
}
