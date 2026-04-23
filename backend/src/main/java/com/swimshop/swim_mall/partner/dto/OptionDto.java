package com.swimshop.swim_mall.partner.dto;

import lombok.Getter;

/**
 * 파트너 상품 조회 시 옵션 정보 DTO
 */
@Getter
public class OptionDto {
    
    private Long optionNo;
    private String color; // 색상
    private String size; // 사이즈
    private Long optionAddPrice; // 추가 가격 (null이면 기본 가격)
    
    public OptionDto(Long optionNo, String color, String size, Long optionAddPrice) {
        this.optionNo = optionNo;
        this.color = color;
        this.size = size;
        this.optionAddPrice = optionAddPrice;
    }
}
