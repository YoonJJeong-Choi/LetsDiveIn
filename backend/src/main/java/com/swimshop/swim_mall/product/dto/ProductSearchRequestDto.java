package com.swimshop.swim_mall.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 검색/필터링 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductSearchRequestDto {
    
    private String keyword; // 검색 키워드 (상품명, 설명)
    private String productType; // 대분류 (ProductType enum name)
    private String productSubType; // 소분류 (ProductSubType enum name)
    private Long minPrice; // 최소 가격
    private Long maxPrice; // 최대 가격
    private String optionName; // 옵션명 (색상, 사이즈 등)
    private String partnerBrandCode; // 파트너 대표 브랜드 코드 필터
    private Boolean saleOnly; // 세일 상품만 조회 여부
    private Boolean inStock; // 재고 상태 필터 (true: 재고 있음, false: 품절)
    private String sortBy; // 정렬 기준 (price, popularity, latest)
    private String sortDir; // 정렬 방향 (asc, desc)
    
    public ProductSearchRequestDto(String keyword, String productType, String productSubType,
                                   Long minPrice, Long maxPrice, String optionName, String partnerBrandCode, Boolean saleOnly,
                                   Boolean inStock, String sortBy, String sortDir) {
        this.keyword = keyword;
        this.productType = productType;
        this.productSubType = productSubType;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.optionName = optionName;
        this.partnerBrandCode = partnerBrandCode;
        this.saleOnly = saleOnly;
        this.inStock = inStock;
        this.sortBy = sortBy;
        this.sortDir = sortDir;
    }
}
