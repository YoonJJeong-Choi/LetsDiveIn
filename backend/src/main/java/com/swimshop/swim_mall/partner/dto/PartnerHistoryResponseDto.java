package com.swimshop.swim_mall.partner.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.PartnerHistoryActionType;
import com.swimshop.swim_mall.common.enums.PartnerStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 파트너 이력 응답 DTO
 * 실제 상태 변경이 발생한 경우만 기록
 */
@Getter
@Builder
public class PartnerHistoryResponseDto {
    private Long historyId;
    private Long partnerId;
    private String partnerName;
    private PartnerHistoryActionType actionType; // 액션 타입 (실제 상태 변경만 기록)
    private PartnerStatus fromStatus; // 변경 전 상태
    private PartnerStatus toStatus; // 변경 후 상태
    private String reason; // 사유
    private Long adminId; // 처리한 관리자 ID (null 가능)
    private String adminName; // 처리한 관리자 이름 (null 가능)
    private LocalDateTime createdAt; // 이력 생성일시
}
