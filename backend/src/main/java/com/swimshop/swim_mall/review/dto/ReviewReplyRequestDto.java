package com.swimshop.swim_mall.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리뷰 답변 작성/수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyRequestDto {
    
    @NotBlank(message = "답변 내용은 필수입니다.")
    private String reviewReply; // 답변 내용
}
