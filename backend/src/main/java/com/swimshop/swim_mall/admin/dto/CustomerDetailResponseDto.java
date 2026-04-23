package com.swimshop.swim_mall.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자용 고객 상세 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailResponseDto {
    
    // 기본 정보
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private LocalDate customerBirth;
    private LocalDateTime customerCreateAt;
    private Boolean emailChecked; // 이메일 인증 여부
    private String accountStatus; // 계정 상태 (ACTIVE, INACTIVE)
    
    // 등급 정보
    private CustomerGradeDto customerGrade; // 고객 등급
    private Long totalPurchaseAmount; // 누적 구매액 (등급 산정용)
    
    // 통계 정보
    private Long totalOrderCount; // 총 주문 건수
    private Long totalOrderAmount; // 총 주문 금액
    private Long totalReviewCount; // 총 리뷰 건수
    private Long totalReturnCount; // 총 반품 건수
    
    // 최근 주문 목록 (최대 10개)
    private List<OrderSummaryDto> recentOrders;
    
    // 최근 리뷰 목록 (최대 10개)
    private List<ReviewSummaryDto> recentReviews;
    
    // 최근 반품 목록 (최대 10개)
    private List<ReturnSummaryDto> recentReturns;
    
    // 고객 태그 목록
    private List<CustomerTagDto> tags;
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummaryDto {
        private Long orderNo;
        private LocalDateTime orderCreatedAt;
        private String orderStatus;
        private Long orderTotalPrice;
        private Integer itemCount; // 주문 상품 수
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSummaryDto {
        private Long reviewNo;
        private Long productNo;
        private String productName;
        private Integer rating;
        private String reviewContent;
        private LocalDateTime reviewCreatedAt;
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnSummaryDto {
        private Long returnNo;
        private Long orderNo;
        private String productName;
        private String returnStatus;
        private Long returnAmount;
        private LocalDateTime returnRequestedAt;
    }
}
