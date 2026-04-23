package com.swimshop.swim_mall.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트너 거절 요청 DTO
 */
@Getter
@NoArgsConstructor
public class PartnerRejectionRequestDto {
    
    @NotBlank(message = "거절 사유는 필수입니다.")
    private String rejectionReason;
}
