package com.swimshop.swim_mall.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscountType {
    PERCENT("퍼센트"),
    FIXED("정액");

    private final String label;
}

