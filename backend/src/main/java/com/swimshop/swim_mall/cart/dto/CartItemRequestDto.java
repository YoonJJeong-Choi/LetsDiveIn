package com.swimshop.swim_mall.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequestDto {
    
    private Long productNo; // 상품 번호
    private Long optionNo; // 옵션 번호 (nullable)
    private Integer quantity; // 수량
    private Long itemPrice; // 가격 (옵션 포함)
}
