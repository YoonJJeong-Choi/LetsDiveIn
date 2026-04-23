package com.swimshop.swim_mall.faq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqRequestDto {
    
    @NotBlank(message = "질문은 필수입니다.")
    private String faqQuestion;
    
    @NotBlank(message = "답변은 필수입니다.")
    private String faqAnswer;
    
    @NotBlank(message = "카테고리는 필수입니다.")
    private String faqCategory;
}
