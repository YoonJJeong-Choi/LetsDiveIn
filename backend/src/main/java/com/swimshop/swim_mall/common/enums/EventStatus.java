package com.swimshop.swim_mall.common.enums;

public enum EventStatus {
    PRIVATE("비공개"),
    PUBLISHED("공개"),
    ENDED("종료");

    private final String label;

    EventStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
