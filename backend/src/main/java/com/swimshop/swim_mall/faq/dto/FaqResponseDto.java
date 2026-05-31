package com.swimshop.swim_mall.faq.dto;

import lombok.Getter;

@Getter
public class FaqResponseDto {
    
    private Long faqNo;
    private String faqQuestion;
    private String faqAnswer;
    private String faqCategory; // InquiryCategory 코드 (ORDER_PAYMENT 등)
    private String faqCategoryLabel; // 화면용 한글 라벨
    private String adminName;

    public FaqResponseDto(Long faqNo, String faqQuestion, String faqAnswer,
                         String faqCategory, String faqCategoryLabel, String adminName) {
        this.faqNo = faqNo;
        this.faqQuestion = faqQuestion;
        this.faqAnswer = faqAnswer;
        this.faqCategory = faqCategory;
        this.faqCategoryLabel = faqCategoryLabel;
        this.adminName = adminName;
    }
}
