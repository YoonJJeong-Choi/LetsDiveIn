package com.swimshop.swim_mall.common.enums;

/**
 * 파트너 이력 액션 타입
 * 상태 변경 및 중요한 액션(신청, 거절)을 모두 기록
 */
public enum PartnerHistoryActionType {
    
    /**
     * 입점 신청 (null → PENDING)
     */
    APPLICATION("입점 신청"),
    
    /**
     * 입점 승인 (PENDING → APPROVED)
     */
    APPROVAL("입점 승인"),
    
    /**
     * 입점 거절 (PENDING → REJECTED)
     */
    REJECTION("입점 거절"),
    
    /**
     * 휴업 신청 (APPROVED → APPROVED, 상태 변경 없음)
     */
    DEACTIVATION_REQUEST("휴업 신청"),
    
    /**
     * 휴업 승인 (APPROVED → INACTIVE)
     */
    DEACTIVATION_APPROVED("휴업 승인"),
    
    /**
     * 휴업 신청 거절 (APPROVED → APPROVED, 상태 변경 없음)
     */
    DEACTIVATION_REJECTION("휴업 신청 거절"),
    
    /**
     * 재활성화 신청 (INACTIVE → INACTIVE, 상태 변경 없음)
     */
    REACTIVATION_REQUEST("재활성화 신청"),
    
    /**
     * 재활성화 승인 (INACTIVE → APPROVED)
     */
    REACTIVATION_APPROVED("재활성화 승인"),
    
    /**
     * 재활성화 신청 거절 (INACTIVE → INACTIVE, 상태 변경 없음)
     */
    REACTIVATION_REJECTION("재활성화 신청 거절"),
    
    /**
     * 관리자 직접 비활성화 (APPROVED → INACTIVE)
     */
    DEACTIVATED("비활성화"),
    
    /**
     * 관리자 직접 재활성화 (INACTIVE → APPROVED)
     */
    ACTIVATED("재활성화");

    private final String label;

    PartnerHistoryActionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
