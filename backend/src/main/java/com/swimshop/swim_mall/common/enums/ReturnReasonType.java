package com.swimshop.swim_mall.common.enums;

/**
 * 고객 반품 신청 시 선택하는 반품 사유 유형.
 */
public enum ReturnReasonType {
    /** 단순 변심 (정책 충족 등) */
    CHANGE_OF_MIND,
    /** 상품 하자·불량 */
    DEFECT,
    /** 쇼핑몰 측 오배송 (다른 상품·옵션 수령) */
    WRONG_ITEM,
    /** 고객 주문 실수 (색/사이즈 등 잘못 주문) */
    ORDER_MISTAKE,
    /** 기타 (상세 서술 필요) */
    OTHER
}
