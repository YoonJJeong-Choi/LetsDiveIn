package com.swimshop.swim_mall.sale.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleRejectRequestDto {
    @NotBlank(message = "거절 사유를 입력해주세요.")
    private String rejectionReason;
}
