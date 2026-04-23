package com.swimshop.swim_mall.partner.dto;

import com.swimshop.swim_mall.common.enums.PartnerChangeRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PartnerChangeRequestResponseDto {
    private Long requestId;
    private Long partnerId;
    private String partnerName;
    private String requestedPartnerName;
    private String requestedPartnerBankAccount;
    private String requestedBusinessRegistrationNumber;
    private String requestedRepresentativeBrandCode;
    private PartnerChangeRequestStatus status;
    private String requestReason;
    private String rejectReason;
    private Long processedAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
