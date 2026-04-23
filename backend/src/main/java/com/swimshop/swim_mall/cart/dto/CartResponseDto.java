package com.swimshop.swim_mall.cart.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    
    private Long cartNo; // 장바구니 번호
    private List<CartItemResponseDto> items; // 장바구니 아이템 목록
    private Long totalPrice; // 전체 총 가격
}
