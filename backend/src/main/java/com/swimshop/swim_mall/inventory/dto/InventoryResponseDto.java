package com.swimshop.swim_mall.inventory.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InventoryResponseDto {

    private Long inventoryNo; // 재고 고유식별자
    private Long productNo; // 상품 번호
    private String productName; // 상품명
    private Long optionNo; // 옵션 번호
    private String color; // 색상
    private String size; // 사이즈
    private Integer stockQuantity; // 재고 수량
    private Boolean inStock; // 판매 가능 여부 (재고 1 이상이면 true)
}
