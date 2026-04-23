package com.swimshop.swim_mall.common.enums;

/**
 * 활성 상태 enum
 * - PENDING: 승인 대기 중 (상품 등록 시 초기 상태)
 * - ACTIVE: 활성 (승인되어 판매 중)
 * - REJECTED: 거절됨 (관리자가 거절, 파트너가 수정 후 재신청 필요)
 * - INACTIVE: 비활성 (승인되었지만 파트너가 일시 중지)
 * - PENDING_UPDATE: 수정 승인 대기 중 (활성 상품의 재심사 필수 항목 수정 시)
 */
public enum ActiveStatus{

    PENDING("승인 대기"),
    ACTIVE("활성"),
    REJECTED("거절됨"),
    INACTIVE("비활성"),
    PENDING_UPDATE("수정 승인 대기");

    private final String label;

    ActiveStatus(String label){
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
