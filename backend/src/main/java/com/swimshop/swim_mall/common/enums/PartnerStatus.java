package com.swimshop.swim_mall.common.enums;

public enum PartnerStatus {

    PENDING("입점 신청"),                    // 입점 신청 상태
    APPROVED("승인 · 운영 중"),              // 승인받음 + 현재 운영 중
    REJECTED("거절"),                        // 거절됨 (Account 없음, 재신청 가능)
    INACTIVE("승인 · 비활성");               // 승인받음 + 지금은 비활성화됨 (Account 있음, 재활성화 가능)

    private final String label;

    PartnerStatus(String label){
        this.label = label;
    }

    public String getLabel(){
        return this.label;
    }
}
