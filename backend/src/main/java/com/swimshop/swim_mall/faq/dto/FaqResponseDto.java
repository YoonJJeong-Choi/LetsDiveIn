package com.swimshop.swim_mall.faq.dto;

import lombok.Getter;

@Getter
public class FaqResponseDto {
    
    private Long faqNo;
    private String faqQuestion;
    private String faqAnswer;
    private String faqCategory;
    private String adminName; // 작성한 관리자 이름
    
    public FaqResponseDto(Long faqNo, String faqQuestion, String faqAnswer, 
                         String faqCategory, String adminName) {
        this.faqNo = faqNo;
        this.faqQuestion = faqQuestion;
        this.faqAnswer = faqAnswer;
        this.faqCategory = faqCategory;
        this.adminName = adminName;
    }
}
