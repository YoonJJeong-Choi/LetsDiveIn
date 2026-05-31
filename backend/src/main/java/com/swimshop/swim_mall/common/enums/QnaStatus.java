package com.swimshop.swim_mall.common.enums;

public enum QnaStatus {

    PENDING("답변 대기"),
    ANSWERED("답변 완료");

    private final String label;

    QnaStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
