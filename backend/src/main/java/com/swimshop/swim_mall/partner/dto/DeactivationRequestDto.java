package com.swimshop.swim_mall.partner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트너 휴업 신청 요청 DTO
 */
@Getter
@NoArgsConstructor
public class DeactivationRequestDto {

    @NotBlank(message = "휴업 사유는 필수입니다.")
    private String deactivationReason; // 휴업 신청 사유
}
