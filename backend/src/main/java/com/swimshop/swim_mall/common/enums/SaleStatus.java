package com.swimshop.swim_mall.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SaleStatus {
    PENDING_APPROVAL("승인대기"),
    ACTIVE("활성"),
    INACTIVE("비활성"),
    REJECTED("거절"),
    EXPIRED("종료"),
    CANCELLED("취소");

    private final String label;
}

