package com.swimshop.swim_mall.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointType {
    ACCUMULATE("적립"),           // 구매 확정 시 자동 적립
    USE("사용"),                  // 주문 결제 시 사용
    MANUAL_ADD("수동 지급"),      // 관리자 수동 지급
    MANUAL_DEDUCT("수동 차감"),   // 관리자 수동 차감
    EXPIRE("만료");               // 포인트 만료

    private final String label;
}
