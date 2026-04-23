package com.swimshop.swim_mall.return_order.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 반품 변경 이력 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnHistoryDto {
    
    private Long historyId; // 이력 ID
    private Long returnNo; // 반품 번호
    private Long adminId; // 변경한 관리자 ID (null 가능)
    private String adminName; // 변경한 관리자 이름 (null 가능)
    private Long partnerId; // 변경한 파트너 ID (null 가능)
    private String partnerName; // 변경한 파트너 이름 (null 가능)
    private LocalDateTime changedAt; // 변경 일시
    private String actionType; // 변경 타입 (CREATE, APPROVE, REJECT, STATUS_CHANGE, TRACKING_UPDATE)
    private String oldValue; // 변경 전 값
    private String newValue; // 변경 후 값
    private String reason; // 변경 사유
    
    // 변경 타입별 설명
    public String getActionTypeLabel() {
        switch (actionType) {
            case "CREATE":
                return "반품 신청";
            case "APPROVE":
                return "반품 승인";
            case "REJECT":
                return "반품 거절";
            case "STATUS_CHANGE":
                return "상태 변경";
            case "TRACKING_UPDATE":
                return "송장번호 수정";
            default:
                return actionType;
        }
    }
    
    // 변경한 사용자 이름 (관리자 또는 파트너)
    public String getChangedByName() {
        if (adminName != null) {
            return adminName;
        } else if (partnerName != null) {
            return partnerName;
        } else {
            return "시스템";
        }
    }
}
