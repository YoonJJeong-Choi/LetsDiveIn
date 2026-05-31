package com.swimshop.swim_mall.partner.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 파트너 대시보드 — 운영·추이·정산 요약
 */
@Getter
@Builder
public class PartnerTodayOperationsDto {

    /** 오늘 결제 완료(paidAt)된 주문 중, 본인 라인이 포함된 주문 건수(중복 주문 제거) */
    private long todayPaidDistinctOrderCount;

    /** 위 주문들에 대한 본인 주문상품 라인 매출 합계(원) */
    private long todayPartnerLineRevenueKrw;

    /**
     * 발송 전으로 처리해야 할 주문 건수(중복 제거).
     * 발주 확인 전(confirmedAt 없음) 또는 배송 상태가 배송준비(READY)인 본인 라인이 하나라도 있는 주문.
     */
    private long ordersPreShipmentDistinctCount;

    private long productsPendingNewApprovalCount;
    private long productsPendingUpdateApprovalCount;
    private long optionsPendingNewApprovalCount;
    private long optionsPendingUpdateApprovalCount;

    /** 재고 임계(이하이면 집계). 응답에 그대로 노출해 프론트와 맞출 수 있음 */
    private int lowStockThresholdUsed;

    /** 임계 이하 재고 행 수(옵션 1행 또는 옵션 없는 상품 1행) */
    private long lowStockLineCount;

    /** 내 상품 기준 반품(REQUESTED·APPROVED·PICKUP_COMPLETED, 환불 전) */
    private long returnsOpenCount;

    /** 일별 추이 구간 길이(7~30) */
    private int trendDays;

    /** 결제 완료(paidAt) 기준 일별 주문 수·본인 라인 매출 */
    private List<DailyPaidTrendPointDto> dailyPaidTrend;

    /** 정산 생성 전, 정산 준비 완료 라인 기준 예상 정산액·판매액(백엔드 정산 모듈과 동일) */
    private long settlementReadyTotalSettlementAmountKrw;
    private long settlementReadyTotalSalesAmountKrw;
    private int settlementReadyLineCount;

    /** 이미 생성된 정산 건 중 미지급(PENDING) 배치 */
    private long pendingSettlementBatchCount;
    private long pendingSettlementBatchAmountKrw;

    @Getter
    @Builder
    public static class DailyPaidTrendPointDto {
        private LocalDate date;
        private long paidOrderCount;
        private long paidRevenueKrw;
    }
}
