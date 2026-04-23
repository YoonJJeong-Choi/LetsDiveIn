package com.swimshop.swim_mall.common.enums;

/**
 * 관리자 운영 상태
 * ACTIVE: 활성 상태 (정상 운영)
 * INACTIVE: 비활성 상태 (계정 정지)
 */
public enum AdminStatus {

    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String label;

    AdminStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
