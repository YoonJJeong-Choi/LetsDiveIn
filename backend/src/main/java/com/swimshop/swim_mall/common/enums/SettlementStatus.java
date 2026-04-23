package com.swimshop.swim_mall.common.enums;

public enum SettlementStatus {

    PENDING("대기"),
    @Deprecated
    PROCESSING("처리중"), // 더 이상 사용하지 않음 (하위 호환성을 위해 유지)
    COMPLETED("완료"),
    CANCELLED("취소");

    private final String label;

    SettlementStatus(String label){
        this.label = label;
    }

    public String getLabel(){
        return this.label;
    }
}

