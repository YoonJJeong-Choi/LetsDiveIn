package com.swimshop.swim_mall.ai.reviewanalysis.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewItemDto {

    private Long reviewNo;
    private Long productNo;
    private Long optionNo;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
