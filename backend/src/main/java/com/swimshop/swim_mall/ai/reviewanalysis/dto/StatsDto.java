package com.swimshop.swim_mall.ai.reviewanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsDto {

    private int count;
    private double avgRating;
    private int ratingDist1;
    private int ratingDist2;
    private int ratingDist3;
    private int ratingDist4;
    private int ratingDist5;
}
