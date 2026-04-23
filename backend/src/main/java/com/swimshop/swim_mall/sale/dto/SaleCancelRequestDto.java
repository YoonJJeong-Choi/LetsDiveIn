package com.swimshop.swim_mall.sale.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleCancelRequestDto {
    @NotBlank(message = "중단 사유를 입력해주세요.")
    private String cancelReason;
}

