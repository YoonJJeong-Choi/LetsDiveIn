package com.swimshop.swim_mall.common.enums;

import java.util.Arrays;

public enum InquiryCategory {

    ORDER_PAYMENT("주문/결제"),
    DELIVERY("배송"),
    RETURN_EXCHANGE("취소/반품/교환"),
    MEMBER("회원정보"),
    PRODUCT("상품"),
    POINT("포인트"),
    ETC("기타");

    private final String label;

    InquiryCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static InquiryCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + code));
    }

    /** enum 코드 또는 한글 라벨(레거시) → enum 코드 문자열 */
    public static String resolveCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        String trimmed = raw.trim();
        try {
            return fromCode(trimmed).name();
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        if ("포인트/쿠폰".equals(trimmed)) {
            return POINT.name();
        }
        return Arrays.stream(values())
                .filter(c -> c.label.equals(trimmed))
                .findFirst()
                .map(InquiryCategory::name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + raw));
    }

    public static InquiryCategory fromStored(String stored) {
        return fromCode(resolveCode(stored));
    }

    public boolean requiresOrderItem() {
        return this == PRODUCT || this == DELIVERY;
    }

    public boolean isAdminOnlyCategory() {
        return this == ORDER_PAYMENT || this == MEMBER || this == POINT || this == ETC;
    }
}
