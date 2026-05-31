package com.swimshop.swim_mall.common.enums;

public enum AuthPortal {
    CUSTOMER("고객몰"),
    ADMIN("관리자 포털");

    private final String label;

    AuthPortal(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean supports(AccountRole role) {
        return switch (this) {
            case CUSTOMER -> role == AccountRole.CUSTOMER;
            case ADMIN -> role == AccountRole.ADMIN || role == AccountRole.PARTNER;
        };
    }

    public String getAllowedRoleLabel() {
        return switch (this) {
            case CUSTOMER -> "고객 계정";
            case ADMIN -> "관리자 또는 파트너 계정";
        };
    }
}
