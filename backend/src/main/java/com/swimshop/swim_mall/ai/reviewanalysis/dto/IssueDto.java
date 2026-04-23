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
public class IssueDto {

    private String type;
    private int frequency;
    private String severity;
    private List<Long> evidenceReviewNos;
}
