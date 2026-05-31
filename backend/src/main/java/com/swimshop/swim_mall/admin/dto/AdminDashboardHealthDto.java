package com.swimshop.swim_mall.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 기본 대시보드 — 전체 헬스(요약)
 * 주문/매출은 결제 승인 시각(paidAt) 기준.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardHealthDto {

    /** 오늘(자정~익일 자정) 결제 완료된 주문 건수(고유 주문 번호) */
    private long paidOrdersTodayCount;

    /** 최근 7일(오늘 포함 7일) 결제 완료된 주문 건수 */
    private long paidOrdersLast7DaysCount;

    /** 오늘 결제 완료 매출 합계(원, paymentAmount) */
    private long paidRevenueTodayKrw;

    /** 최근 7일 결제 완료 매출 합계(원) */
    private long paidRevenueLast7DaysKrw;

    /** 오늘 가입(고객 생성일 기준) */
    private long newCustomersTodayCount;

    /** 최근 7일 가입 */
    private long newCustomersLast7DaysCount;

    /** 이메일 인증 완료 고객 수(전체 스냅샷) */
    private long emailVerifiedCustomersTotal;

    /** 승인된 파트너 수 (PartnerStatus.APPROVED) */
    private long activePartnersCount;

    /** 전체 상품 건수 */
    private long totalProductsCount;

    /** 판매 가능 상태 상품(ActiveStatus.ACTIVE) */
    private long activeProductsCount;
}
