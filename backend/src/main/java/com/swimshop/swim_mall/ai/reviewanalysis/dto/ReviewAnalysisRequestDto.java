package com.swimshop.swim_mall.ai.reviewanalysis.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAnalysisRequestDto {

    private String partnerId;
    private LocalDateTime fromAt;
    private LocalDateTime toAt;
    private List<Long> productNos;
    private Integer maxReviews;
    private List<ReviewItemDto> reviews;
}
