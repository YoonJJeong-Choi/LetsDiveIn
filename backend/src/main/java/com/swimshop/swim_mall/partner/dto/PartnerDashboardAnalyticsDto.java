package com.swimshop.swim_mall.partner.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 파트너 매출·운영 분석(기간·SKU 필터). 행동에 연결되는 지표 위주.
 */
@Getter
@Builder
public class PartnerDashboardAnalyticsDto {

    private int trendDays;
    /** 요청 시 SKU(상품번호) 필터. null이면 전체 */
    private Long productNoFilter;

    private List<DailyPaidTrendPointDto> dailyPaidTrend;

    /** 선택 기간 내 결제 완료 주문 수(본인 라인 포함 주문, 중복 제거) */
    private long periodPaidDistinctOrderCount;
    /** 선택 기간 내 본인 라인 매출 합계(원) */
    private long periodPartnerLineRevenueKrw;

    private List<ProductSkuPerformanceDto> topProductsByLineRevenue;

    private long productsPendingNewApprovalCount;
    private long productsPendingUpdateApprovalCount;
    private long optionsPendingNewApprovalCount;
    private long optionsPendingUpdateApprovalCount;

    private int lowStockThresholdUsed;
    private long lowStockLineCount;
    private long outOfStockLineCount;

    private long ordersPreShipmentDistinctCount;
    private long ordersInDeliveryDistinctCount;
    private int deliveryReadyDelayedDaysThreshold;
    private long deliveriesReadyDelayedPartnerLineCount;

    private long settlementReadyTotalSettlementAmountKrw;
    private long settlementReadyTotalSalesAmountKrw;
    private int settlementReadyLineCount;
    private long pendingSettlementBatchCount;
    private long pendingSettlementBatchAmountKrw;

    /** 기간 내 반품 신청 건수(본인 상품 라인) */
    private long returnsRequestedInPeriod;
    private List<ReturnReasonCountDto> returnReasonBreakdownInPeriod;

    /** 현재 미완료 반품(오늘의 운영과 동일 정의) */
    private long returnsOpenCount;

    @Getter
    @Builder
    public static class DailyPaidTrendPointDto {
        private LocalDate date;
        private long paidOrderCount;
        private long paidRevenueKrw;
    }

    @Getter
    @Builder
    public static class ProductSkuPerformanceDto {
        private long productNo;
        private String productName;
        private String sku;
        private long quantitySold;
        private long lineRevenueKrw;
    }

    @Getter
    @Builder
    public static class ReturnReasonCountDto {
        private String reasonType;
        private long count;
    }
}
