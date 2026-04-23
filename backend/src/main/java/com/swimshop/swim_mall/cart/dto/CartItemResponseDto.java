package com.swimshop.swim_mall.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {
    
    private Long cartItemNo; // 장바구니 아이템 번호
    private Long productNo; // 상품 번호
    private String productName; // 상품명
    private String productImageUrl; // 상품 이미지
    private Long optionNo; // 옵션 번호 (nullable)
    private String color; // 색상 (nullable)
    private String size; // 사이즈 (nullable)
    private Integer quantity; // 수량
    private Long itemPrice; // 가격 (옵션 포함)
    private Long totalPrice; // 총 가격 (수량 * 가격)
}
