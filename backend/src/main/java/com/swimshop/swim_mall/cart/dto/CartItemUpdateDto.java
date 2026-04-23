package com.swimshop.swim_mall.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemUpdateDto {
    private Integer quantity; // 수량 (null이면 수정 안 함)
    private Long optionNo; // 옵션 번호 (null이면 옵션 변경 안 함)
}
