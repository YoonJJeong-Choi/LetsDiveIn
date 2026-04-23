package com.swimshop.swim_mall.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointManualRequestDto {
    
    @NotNull(message = "포인트 금액은 필수입니다.")
    @Min(value = 1, message = "포인트 금액은 1 이상이어야 합니다.")
    private Long pointAmount;
    
    @NotBlank(message = "설명은 필수입니다.")
    private String description;
}
