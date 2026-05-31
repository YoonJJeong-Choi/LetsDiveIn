package com.swimshop.swim_mall.common.enums;

import java.util.Arrays;

public enum QnaScope {

    POLICY("이용 방법·기간·정책"),
    ORDER_ITEM("주문 상품 건");

    private final String label;

    QnaScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static QnaScope fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("scope is required");
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown scope: " + code));
    }
}
