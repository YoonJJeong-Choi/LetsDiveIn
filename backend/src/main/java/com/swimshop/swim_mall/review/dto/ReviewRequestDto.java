package com.swimshop.swim_mall.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 리뷰 작성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {
    
    @NotNull(message = "주문 아이템 번호는 필수입니다.")
    private Long orderItemNo; // 주문 아이템 번호 (구매 인증, 중복 방지)
    
    @NotBlank(message = "리뷰 내용은 필수입니다.")
    private String reviewContent; // 리뷰 내용
    
    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
    private Integer reviewRating; // 평점 1~5

    // 선택: 리뷰 이미지 파일 IDs (category=review-image 업로드 결과)
    private List<Long> imageFileIds;
}
