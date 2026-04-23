package com.swimshop.swim_mall.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerChangeRequestRejectDto {
    @NotBlank(message = "거절 사유는 필수입니다.")
    private String rejectReason;
}
