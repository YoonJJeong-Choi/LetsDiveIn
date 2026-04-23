package com.swimshop.swim_mall.ai.reviewanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDto {

    private Long reviewNo;
    private String quote;
}
