package com.swimshop.swim_mall.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 매출 현황 통계 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesStatisticsDto {
    
    /**
     * 기간별 매출 통계
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodSalesDto {
        private LocalDate date; // 날짜
        private Long totalSales; // 총 매출액
        private Long totalOrders; // 총 주문 수
        private Long totalQuantity; // 총 판매 수량
        private Long averageOrderAmount; // 평균 주문 금액
    }
    
    /**
     * 상품별 매출 통계
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesDto {
        private Long productNo; // 상품 번호
        private String productName; // 상품명
        private Long totalSales; // 총 매출액
        private Long totalQuantity; // 총 판매 수량
        private Long orderCount; // 주문 건수
    }
    
    /**
     * 고객별 매출 통계
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSalesDto {
        private Long customerId; // 고객 ID
        private String customerName; // 고객명
        private String customerEmail; // 고객 이메일
        private Long totalSales; // 총 구매 금액
        private Long orderCount; // 주문 건수
        private Long averageOrderAmount; // 평균 주문 금액
    }
    
    /**
     * 파트너별 매출 통계
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnerSalesDto {
        private Long partnerId; // 파트너 ID
        private String partnerName; // 파트너명
        private String partnerEmail; // 파트너 이메일
        private Long totalSales; // 총 매출액
        private Long totalQuantity; // 총 판매 수량
        private Long orderCount; // 주문 건수
        private Long averageOrderAmount; // 평균 주문 금액
    }
    
    /**
     * 카테고리별 매출 통계
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySalesDto {
        private String categoryName; // 카테고리명 (ProductType의 label)
        private Long totalSales; // 총 매출액
        private Long totalQuantity; // 총 판매 수량
        private Long orderCount; // 주문 건수
    }
    
    // 전체 통계
    private Long totalSales; // 전체 매출액
    private Long totalOrders; // 전체 주문 수
    private Long totalQuantity; // 전체 판매 수량
    private Long averageOrderAmount; // 평균 주문 금액
    
    // 기간별 매출 (일별, 주별, 월별)
    private List<PeriodSalesDto> dailySales; // 일별 매출
    private List<PeriodSalesDto> weeklySales; // 주별 매출
    private List<PeriodSalesDto> monthlySales; // 월별 매출
    
    // 상품별 매출 (TOP N)
    private List<ProductSalesDto> topProducts; // 인기 상품 TOP 10
    
    // 고객별 매출 (TOP N)
    private List<CustomerSalesDto> topCustomers; // VIP 고객 TOP 10
    
    // 파트너별 매출
    private List<PartnerSalesDto> partnerSales; // 파트너별 매출 (전체)
    
    // 카테고리별 매출
    private List<CategorySalesDto> categorySales; // 카테고리별 매출 (전체)
    
    // 기간 비교 (전월 대비, 전주 대비 등)
    private PeriodComparisonDto periodComparison; // 기간 비교
}
