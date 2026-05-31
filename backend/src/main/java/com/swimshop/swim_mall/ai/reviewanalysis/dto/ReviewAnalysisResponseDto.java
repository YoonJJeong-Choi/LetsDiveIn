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
public class ReviewAnalysisResponseDto {

    private String summary;
    private List<IssueDto> issues;
    private List<ActionDto> actions;
    private List<KeywordDto> topKeywords;
    private List<QuoteDto> representativeQuotes;
    private StatsDto stats;
    private AlertsDto alerts;
}
