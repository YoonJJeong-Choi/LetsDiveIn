package com.swimshop.swim_mall.review.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 리뷰 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    
    private Long reviewNo; // 리뷰 번호
    private Long orderItemNo; // 주문 아이템 번호
    private Long orderNo; // 주문 번호
    private Long productNo; // 상품 번호
    private String productName; // 상품명
    private String productImageUrl; // 상품 이미지 URL
    private Long optionNo; // 옵션 번호 (nullable)
    private String color; // 색상 (nullable)
    private String size; // 사이즈 (nullable)
    private Long customerId; // 고객 ID
    private String customerName; // 고객명 (또는 닉네임)
    private String reviewContent; // 리뷰 내용
    private Integer reviewRating; // 평점 1~5
    private LocalDateTime reviewCreatedAt; // 작성일시
    private String reviewReply; // 리뷰 답변 내용 (nullable)
    private LocalDateTime reviewReplyCreatedAt; // 답변 작성일시 (nullable)

    // 선택: 리뷰 이미지 URL 목록
    private List<String> imageUrls;
}
