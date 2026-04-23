package com.swimshop.swim_mall.product.dto;

import lombok.Getter;

import com.swimshop.swim_mall.common.enums.ActiveStatus;

/**
 * 상품 옵션 DTO (고객용/관리자용)
 */
@Getter
public class ProductOptionDto {
    
    private Long optionNo;
    private String color; // 색상
    private String size; // 사이즈
    private Long optionAddPrice; // 추가 가격 (null이면 기본 가격)
    private Long totalPrice; // 총 가격 (기본 가격 + 추가 가격)
    private ActiveStatus optionStatus; // 옵션 상태 (관리자용: ACTIVE, INACTIVE, PENDING_UPDATE, REJECTED)
    
    // 재고 정보 (고객용/관리자용)
    private Integer stockQuantity; // 재고 수량 (null이면 재고 정보 없음)
    private Boolean inStock; // 재고 있음 여부 (true: 재고 있음, false: 품절, null: 재고 정보 없음)
    
    // 고객용 생성자 (상태 없음, 재고 정보 없음)
    public ProductOptionDto(Long optionNo, String color, String size, Long optionAddPrice, Long totalPrice) {
        this(optionNo, color, size, optionAddPrice, totalPrice, null, null, null);
    }
    
    // 고객용 생성자 (상태 없음, 재고 정보 포함)
    public ProductOptionDto(Long optionNo, String color, String size, Long optionAddPrice, Long totalPrice, Integer stockQuantity, Boolean inStock) {
        this(optionNo, color, size, optionAddPrice, totalPrice, null, stockQuantity, inStock);
    }
    
    // 관리자용 생성자 (상태 포함, 재고 정보 없음)
    public ProductOptionDto(Long optionNo, String color, String size, Long optionAddPrice, Long totalPrice, ActiveStatus optionStatus) {
        this(optionNo, color, size, optionAddPrice, totalPrice, optionStatus, null, null);
    }
    
    // 관리자용 생성자 (상태 및 재고 정보 포함)
    public ProductOptionDto(Long optionNo, String color, String size, Long optionAddPrice, Long totalPrice, ActiveStatus optionStatus, Integer stockQuantity, Boolean inStock) {
        this.optionNo = optionNo;
        this.color = color;
        this.size = size;
        this.optionAddPrice = optionAddPrice;
        this.totalPrice = totalPrice;
        this.optionStatus = optionStatus;
        this.stockQuantity = stockQuantity;
        this.inStock = inStock;
    }
}
