package com.swimshop.swim_mall.common.enums;

public enum SettlementHistoryAction {
    CREATE("정산 생성"),
    STATUS_CHANGE("상태 변경"),
    PAID_DATE_UPDATE("지급일 수정");

    private final String label;

    SettlementHistoryAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

