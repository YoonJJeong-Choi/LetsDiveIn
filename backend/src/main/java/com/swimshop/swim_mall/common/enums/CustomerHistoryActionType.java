package com.swimshop.swim_mall.common.enums;

/**
 * 고객 관리 작업 이력 액션 타입
 */
public enum CustomerHistoryActionType {
    INFO_UPDATE("정보 수정"),           // 이름, 이메일 수정
    STATUS_ACTIVATE("계정 활성화"),      // 비활성화 → 활성화
    STATUS_DEACTIVATE("계정 비활성화"),  // 활성화 → 비활성화
    PASSWORD_RESET("비밀번호 초기화"),    // 비밀번호 초기화
    EMAIL_VERIFICATION_RESEND("이메일 인증 재발송"); // 이메일 인증 재발송
    
    private final String label;
    
    CustomerHistoryActionType(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
