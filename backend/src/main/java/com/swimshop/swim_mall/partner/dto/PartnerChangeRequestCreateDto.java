package com.swimshop.swim_mall.partner.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerChangeRequestCreateDto {
    private String requestedPartnerName;
    private String requestedPartnerBankAccount;
    private String requestedBusinessRegistrationNumber;
    private String requestedRepresentativeBrandCode;
    private String requestReason;
}
