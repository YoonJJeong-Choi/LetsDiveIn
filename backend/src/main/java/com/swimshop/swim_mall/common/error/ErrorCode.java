package com.swimshop.swim_mall.common.error;
//에러 코드 enum
public enum ErrorCode {

    //이메일 형식 오류
    //비번 길이 오류
    //이메일 중복
    //고객 없음(로그인 만들면)
    //서버 오류
    
    //클라이언트
    DUPLICATE_EMAIL(400, "이미 존재하는 이메일입니다"),
    EMAIL_ALREADY_EXISTS(400, "이미 사용 중인 이메일입니다"),
    CUSTOMER_NOT_FOUND(404, "고객을 찾을 수 없습니다"),
    ADMIN_NOT_FOUND(404, "관리자를 찾을 수 없습니다"),
    PARTNER_NOT_FOUND(404, "파트너를 찾을 수 없습니다"),
    ACCOUNT_NOT_FOUND(404, "계정을 찾을 수 없습니다"),
    EMAIL_ALREADY_VERIFIED(400, "이미 이메일 인증이 완료된 고객입니다"),
    EMAIL_SEND_FAILED(500, "이메일 발송에 실패했습니다"),
    PRODUCT_NOT_FOUND(404, "상품을 찾을 수 없습니다"),
    OPTION_NOT_FOUND(404, "옵션을 찾을 수 없습니다"),
    CART_ITEM_NOT_FOUND(404, "장바구니 아이템을 찾을 수 없습니다"),
    ORDER_NOT_FOUND(404, "주문을 찾을 수 없습니다"),
    ORDER_ITEM_NOT_FOUND(404, "주문 아이템을 찾을 수 없습니다"),
    PAYMENT_NOT_FOUND(404, "결제 정보를 찾을 수 없습니다"),
    DELIVERY_NOT_FOUND(404, "배송 정보를 찾을 수 없습니다"),
    RETURN_NOT_FOUND(404, "반품 정보를 찾을 수 없습니다"),
    RETURN_ALREADY_EXISTS(400, "이미 반품 신청이 존재합니다"),
    RETURN_NOT_ELIGIBLE(400, "반품 신청이 불가능한 상태입니다"),
    REVIEW_NOT_FOUND(404, "리뷰를 찾을 수 없습니다"),
    EVENT_NOT_FOUND(404, "이벤트를 찾을 수 없습니다"),
    GRADE_NOT_FOUND(404, "등급을 찾을 수 없습니다"),
    FAQ_NOT_FOUND(404, "FAQ를 찾을 수 없습니다"),
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다"),
    EMAIL_NOT_VERIFIED(403, "이메일 인증이 완료되지 않았습니다"),
    ACCOUNT_INACTIVE(403, "비활성화된 계정입니다. 관리자에게 문의하세요"),
    UNAUTHORIZED(401, "로그인이 필요합니다"),
    FORBIDDEN(403, "접근 권한이 없습니다"),
    PARTNER_INACTIVE(400, "비활성화된 파트너는 해당 기능을 사용할 수 없습니다. 조회만 가능합니다"),
    INVALID_QUANTITY(400, "수량은 1 이상이어야 합니다"),
    INVALID_REQUEST(400, "잘못된 요청입니다"),
    OPTION_REQUIRED(400, "이 상품은 옵션 선택이 필수입니다"),
    INSUFFICIENT_POINT(400, "포인트 잔액이 부족합니다"),
    
    //서버
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
