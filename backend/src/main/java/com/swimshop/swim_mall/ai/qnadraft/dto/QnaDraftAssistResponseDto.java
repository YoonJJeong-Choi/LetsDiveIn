package com.swimshop.swim_mall.ai.qnadraft.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QnaDraftAssistResponseDto {

    private String draftBody;
    private List<FaqReference> referencedFaqs;
    private String confidence;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaqReference {
        private Long faqNo;
        private String question;
    }
}
