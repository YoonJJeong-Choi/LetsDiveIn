package com.swimshop.swim_mall.sale.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.SaleScope;
import com.swimshop.swim_mall.common.enums.SaleStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalePolicyRequestDto {
    private Long eventNo; // 선택
    @NotNull
    private SaleScope scope; // PRODUCT / OPTION
    private Long targetProductNo; // scope=PRODUCT 필수
    private Long targetOptionNo;  // scope=OPTION 필수
    @NotNull
    private DiscountType discountType; // PERCENT / FIXED
    @NotNull
    private Long discountValue; // 1~100 or 금액(원)
    private Long maxDiscountAmount; // 선택
    @NotNull
    private LocalDateTime startAt;
    @NotNull
    private LocalDateTime endAt;
    @NotNull
    private SaleStatus status; // ACTIVE / INACTIVE
}

