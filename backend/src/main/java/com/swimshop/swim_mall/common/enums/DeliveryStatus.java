package com.swimshop.swim_mall.common.enums;

public enum DeliveryStatus {

    READY("배송 준비"),
    SHIPPED("배송 중"),
    DELIVERED("배송 완료");

    private final String label;

    DeliveryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
