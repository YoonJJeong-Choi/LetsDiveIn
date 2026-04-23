package com.swimshop.swim_mall.common.enums;

/**
 * 주문 상품 상태
 * 
 * 주의: 이 enum은 computed 필드로 사용되며, 실제 DB에는 저장되지 않습니다.
 * OrderItem의 여러 필드(confirmedAt, completedAt, deliveryStatus, returnStatus)를 기반으로 계산됩니다.
 */
public enum OrderItemStatus {

    PENDING_CONFIRMATION("발주 확인 대기"), // 발주 확인 전
    CONFIRMED("발주 확인됨"), // 발주 확인 완료 (confirmedAt 설정됨)
    READY("배송 준비"), // 발주 확인 후 배송 준비 상태
    SHIPPED("배송 중"), // 배송 시작됨
    DELIVERED("배송 완료"), // 배송 완료됨
    COMPLETED("구매 확정"), // 구매 확정됨 (completedAt 설정됨)
    RETURN_IN_PROGRESS("반품 진행 중"), // 반품 신청/승인/수거 완료
    RETURN_REJECTED("반품 거절"), // 반품 거절됨
    REFUNDED("환불 완료"), // 환불 완료됨
    CANCELLED("취소됨"); // 취소됨 (isCancelled = true)

    private final String label;

    OrderItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
