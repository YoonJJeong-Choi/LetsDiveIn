package com.swimshop.swim_mall.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.enums.ReturnStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 대시보드 — 추이·랭킹·최근 주문/반품·정산 요약
 */
@Getter
@Builder
public class AdminDashboardInsightsDto {

    /** 일별 추이에 사용한 일수 (insights API: 14~30, analytics API: 7~90) */
    private int trendDays;

    /** 결제 완료(paidAt) 기준 일별 주문 건수·매출 */
    private List<DailyPaidTrendPointDto> dailyPaidTrend;

    /** 카테고리(상품 대분류)별 매출 비중 */
    private List<RevenueShareSliceDto> categoryRevenueShare;

    /** 브랜드명별 매출 비중 (브랜드 없음은 별도 라벨) */
    private List<RevenueShareSliceDto> brandRevenueShare;

    /** 기간 내 매출 기준 인기 상품 TOP N */
    private List<TopProductInsightDto> topProductsByRevenue;

    private List<RecentOrderBriefDto> recentOrders;

    private List<RecentReturnBriefDto> recentReturns;

    private SettlementBriefForDashboardDto settlementBrief;

    @Getter
    @Builder
    public static class DailyPaidTrendPointDto {
        private LocalDate date;
        private long paidOrderCount;
        private long paidRevenueKrw;
    }

    @Getter
    @Builder
    public static class RevenueShareSliceDto {
        private String label;
        private long amountKrw;
        /** 0~1, 소수 둘째 자리까지 계산 후 직렬화 */
        private double ratio;
    }

    @Getter
    @Builder
    public static class TopProductInsightDto {
        private int rank;
        private Long productNo;
        private String productName;
        private long revenueKrw;
        private long quantitySold;
    }

    @Getter
    @Builder
    public static class RecentOrderBriefDto {
        private Long orderNo;
        private LocalDateTime orderCreatedAt;
        private OrderStatus orderStatus;
        private String recipientName;
        private long orderTotalPriceKrw;
    }

    @Getter
    @Builder
    public static class RecentReturnBriefDto {
        private Long returnNo;
        private Long orderNo;
        private ReturnStatus returnStatus;
        private LocalDateTime returnRequestedAt;
        private long returnAmountKrw;
    }

    @Getter
    @Builder
    public static class SettlementBriefForDashboardDto {
        /** 전체 미지급(PENDING) 건수·정산금 합계 */
        private long pendingSettlementCount;
        private long pendingSettlementAmountKrw;

        /** 이번 달 생성된 미지급 정산 건 (정산 모듈 생성일 기준) */
        private long pendingCreatedThisMonthCount;
        private long pendingCreatedThisMonthAmountKrw;

        /** 정산 기간 종료일이 이번 달인 미지급 건 (주기 말미 정리용) */
        private long pendingPeriodEndsThisMonthCount;
        private long pendingPeriodEndsThisMonthAmountKrw;
    }
}
