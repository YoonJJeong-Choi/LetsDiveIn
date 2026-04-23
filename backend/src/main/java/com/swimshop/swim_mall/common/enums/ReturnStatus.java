package com.swimshop.swim_mall.common.enums;

public enum ReturnStatus {

    REQUESTED("반품신청"),        // 고객이 반품 신청
    APPROVED("반품승인"),         // 관리자/파트너가 반품 승인
    REJECTED("반품거절"),         // 관리자/파트너가 반품 거절
    PICKUP_COMPLETED("수거완료"), // 반품 상품 수거 완료
    REFUNDED("환불완료");         // 환불 처리 완료

    private final String label;

    ReturnStatus(String label){
        this.label = label;
    }

    public String getLabel(){
        return this.label;
    }
}

