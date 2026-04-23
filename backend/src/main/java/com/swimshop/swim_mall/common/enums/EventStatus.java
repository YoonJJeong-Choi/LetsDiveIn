package com.swimshop.swim_mall.common.enums;

public enum EventStatus {
    DRAFT("임시 저장"),
    SCHEDULED("오픈 예정"),
    ACTIVE("진행 중"),
    ENDED("종료"),
    INACTIVE("비활성화");

    private final String label;

    EventStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
