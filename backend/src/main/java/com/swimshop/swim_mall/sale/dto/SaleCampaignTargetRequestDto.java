package com.swimshop.swim_mall.sale.dto;

import com.swimshop.swim_mall.common.enums.DiscountType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleCampaignTargetRequestDto {

    // 캠페인 scope에 따라 하나만 사용됩니다.
    private Long targetProductNo;
    private Long targetOptionNo;

    @NotNull
    private DiscountType discountType; // PERCENT / FIXED

    @NotNull
    private Long discountValue; // PERCENT이면 1~100, FIXED이면 금액(원)

    private Long maxDiscountAmount; // 선택
}

