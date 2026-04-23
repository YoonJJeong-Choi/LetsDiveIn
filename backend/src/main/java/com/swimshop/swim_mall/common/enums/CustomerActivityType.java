package com.swimshop.swim_mall.common.enums;

/**
 * 고객 활동 유형
 */
public enum CustomerActivityType {
    LOGIN("로그인"),
    LOGOUT("로그아웃"),
    ORDER_CREATED("주문 생성"),
    ORDER_CANCELLED("주문 취소"),
    ORDER_COMPLETED("주문 완료"),
    REVIEW_CREATED("리뷰 작성"),
    REVIEW_UPDATED("리뷰 수정"),
    REVIEW_DELETED("리뷰 삭제"),
    RETURN_REQUESTED("반품 신청"),
    RETURN_APPROVED("반품 승인"),
    RETURN_REJECTED("반품 거절"),
    RETURN_COMPLETED("반품 완료"),
    PROFILE_UPDATED("프로필 수정"),
    PASSWORD_CHANGED("비밀번호 변경"),
    EMAIL_VERIFIED("이메일 인증"),
    ADDRESS_ADDED("배송지 추가"),
    ADDRESS_UPDATED("배송지 수정"),
    ADDRESS_DELETED("배송지 삭제");

    private final String label;

    CustomerActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
