package com.swimshop.swim_mall.settlement.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.swimshop.swim_mall.common.enums.SettlementHistoryAction;

/**
 * 정산 변경 이력 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementHistoryDto {
    
    private Long historyId; // 이력 ID
    private Long settlementId; // 정산 ID
    private Long adminId; // 변경한 관리자 ID
    private String adminName; // 변경한 관리자 이름
    private LocalDateTime changedAt; // 변경 일시
    private SettlementHistoryAction actionType; // 변경 타입
    private String oldValue; // 변경 전 값
    private String newValue; // 변경 후 값
    private String reason; // 변경 사유
    
    // 변경 타입별 설명
    public String getActionTypeLabel() {
        return actionType != null ? actionType.getLabel() : "-";
    }
}
