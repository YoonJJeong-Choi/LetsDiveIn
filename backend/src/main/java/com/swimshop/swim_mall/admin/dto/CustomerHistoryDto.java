package com.swimshop.swim_mall.admin.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.CustomerHistoryActionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 관리 작업 이력 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHistoryDto {
    
    private Long historyId; // 이력 ID
    private Long customerId; // 고객 ID
    private CustomerHistoryActionType actionType; // 작업 타입
    private String oldValue; // 변경 전 값 (JSON)
    private String newValue; // 변경 후 값 (JSON)
    private String reason; // 작업 사유
    private Long adminId; // 작업한 관리자 ID
    private String adminName; // 작업한 관리자 이름
    private LocalDateTime createdAt; // 작업 일시
    
    /**
     * 작업 타입 라벨 반환
     */
    public String getActionTypeLabel() {
        return actionType != null ? actionType.getLabel() : "";
    }
}
