package com.swimshop.swim_mall.common.enums;

public enum OrderStatus {

    PENDING_PAYMENT("결제 대기중"), // 주문 생성 직후 상태 (결제 대기)
    PAID("결제 완료"), // 결제 성공 후 상태 (발주 확인 대기)
    PAYMENT_FAILED("결제 실패"), // 결제 실패 상태
    ACTIVE("주문 진행중"), // 주문 처리 중 (일부 또는 전체 상품 발주 확인됨, 배송 준비, 배송 중, 배송 완료)
    CANCELLED("주문 취소"); // 주문 전체 취소
    
    // 주의: 구매 확정은 OrderItem.completedAt으로 관리합니다.
    // Order.orderStatus는 주문 전체의 진행 단계만 관리합니다.

    private final String label;

    OrderStatus(String label){
        this.label = label;
    }

    public String getLabel(){
        return this.label;
    }
}

