package com.swimshop.swim_mall.partner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트너 재활성화 신청 요청 DTO
 */
@Getter
@NoArgsConstructor
public class ReactivationRequestDto {

    @NotBlank(message = "재활성화 사유는 필수입니다.")
    private String reactivationReason; // 재활성화 신청 사유
}
