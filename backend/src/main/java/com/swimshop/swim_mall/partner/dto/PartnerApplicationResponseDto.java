package com.swimshop.swim_mall.partner.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.PartnerStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 파트너 입점 신청 응답 DTO
 */
@Getter
@Builder
public class PartnerApplicationResponseDto {
    private Long partnerId;
    private String partnerName;
    private String partnerContact;
    private String partnerBankAccount;
    private String businessRegistrationNumber; // 사업자등록번호
    private String representativeBrandCode; // 대표 취급 브랜드 코드
    private Long businessRegistrationFileId; // 사업자등록증 사본 fileId
    private Long bankAccountFileId; // 통장 사본 fileId
    private String rejectionReason; // 입점 거절 사유
    private PartnerStatus partnerStatus;
    private LocalDateTime partnerApprovedAt;
    private String email;
    private String deactivationRequestReason; // 휴업 신청 사유 (null이면 신청 없음)
    private LocalDateTime deactivationRequestedAt; // 휴업 신청일시 (null이면 신청 없음)
    private String reactivationRequestReason; // 재활성화 신청 사유 (null이면 신청 없음)
    private LocalDateTime reactivationRequestedAt; // 재활성화 신청일시 (null이면 신청 없음)
}
