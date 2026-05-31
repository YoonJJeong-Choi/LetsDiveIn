package com.swimshop.swim_mall.admin.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 분석 대시보드 — 플랫폼 추이·공급·이행·리스크·정산 보조(집계만)
 */
@Getter
@Builder
public class AdminDashboardAnalyticsDto {

    /** insights와 동일 기간(일수는 insights.trendDays) */
    private AdminDashboardInsightsDto insights;

    /** 발주·배송 병목(처리 큐와 동일 스냅샷) */
    private AdminDashboardQueueDto fulfillmentQueue;

    /** 최근 7일 vs 그 이전 7일 결제 완료(paidAt) 매출·주문 건수 */
    private SevenDayComparisonDto sevenDayVsPriorSeven;

    /** 기간 내 결제 완료 주문 라인 매출 기준 파트너 TOP */
    private List<PartnerLineRevenueRankDto> topPartnersByLineRevenue;

    /** 기간 내 결제 건수(행 기준) 시간대 분포 */
    private List<HourPaymentCountDto> paidPaymentCountByHour;

    /** 기간 내 요일별 결제 금액 합계 */
    private List<WeekdayRevenueDto> paidRevenueByWeekday;

    /** 추이 구간 내 반품 신청 건수 */
    private long returnsRequestedInTrendPeriod;

    /** 추이 구간 내 결제 완료 고유 주문 수 */
    private long paidDistinctOrdersInTrendPeriod;

    /** 반품 신청 / 결제 완료 주문 (구간) */
    private double returnRequestsPerPaidOrder;

    @Getter
    @Builder
    public static class SevenDayComparisonDto {
        private long last7PaidRevenueKrw;
        private long prior7PaidRevenueKrw;
        private long last7PaidOrderCount;
        private long prior7PaidOrderCount;
    }

    @Getter
    @Builder
    public static class PartnerLineRevenueRankDto {
        private int rank;
        private Long partnerId;
        private String partnerName;
        private long lineRevenueKrw;
    }

    @Getter
    @Builder
    public static class HourPaymentCountDto {
        private int hourOfDay;
        private long paymentCount;
    }

    @Getter
    @Builder
    public static class WeekdayRevenueDto {
        /** 월~일 */
        private String weekdayLabel;
        private long paidRevenueKrw;
        private long paymentCount;
    }
}
