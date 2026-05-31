package com.swimshop.swim_mall.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 기본 대시보드 — 처리 큐(알림형) 집계
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardQueueDto {

    /** 입점 승인 대기 파트너 (PartnerStatus.PENDING) */
    private long pendingPartnerApplications;

    /** 상품 신규 승인 대기 (ActiveStatus.PENDING) */
    private long productsPendingNewApproval;

    /** 상품 수정 승인 대기 (ActiveStatus.PENDING_UPDATE) */
    private long productsPendingUpdateApproval;

    /** 옵션 신규 승인 대기 */
    private long optionsPendingNewApproval;

    /** 옵션 수정 승인 대기 */
    private long optionsPendingUpdateApproval;

    /** 반품·교환 등 처리 중 (REQUESTED, APPROVED, PICKUP_COMPLETED) */
    private long returnsPendingProcessing;

    /** 결제 대기 주문 */
    private long ordersPendingPayment;

    /** 결제 실패 주문 */
    private long ordersPaymentFailed;

    /** 발주 확인 대기 주문 상품 (취소 제외, 주문 PAID·ACTIVE) */
    private long orderItemsPendingConfirmation;

    /** 배송 지연 집계에 사용한 일수 (발주 확인 후 READY가 이보다 오래 지속) */
    private long deliveriesReadyDelayedDaysThreshold;

    /** 위 임계일수보다 오래 READY인 배송 건수 */
    private long deliveriesReadyDelayed;
}
