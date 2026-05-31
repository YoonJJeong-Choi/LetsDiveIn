package com.swimshop.swim_mall.partner.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

/**
 * 파트너 상품 조회 응답 DTO (상품 + 옵션 목록)
 */
@Getter
public class ProductWithOptionsDto {
    
    private Long productNo;
    private String productName;
    private String productType; // 대분류
    private String productSubType; // 소분류
    private String productPrice; // 기본 가격
    private String productDescription; // 상품 설명
    private String productImageUrl;
    private String sku;
    private String brandName;
    private String materialInfo;
    private String originCountry;
    private String manufactureCountry;
    private String careInstructions;
    private String sizeGuideText;
    private String sizeGuideJson;
    private LocalDateTime productCreatedAt;
    private LocalDateTime productUpdatedAt;
    private String productActiveStatus; // PENDING, ACTIVE, REJECTED, INACTIVE
    private String rejectionReason; // 거절 사유 (REJECTED 상태일 때만 값이 있음)
    
    private List<OptionDto> options; // 해당 파트너의 옵션 목록
    
    public ProductWithOptionsDto(
            Long productNo,
            String productName,
            String productType,
            String productSubType,
            String productPrice,
            String productDescription,
            String productImageUrl,
            String sku,
            String brandName,
            String materialInfo,
            String originCountry,
            String manufactureCountry,
            String careInstructions,
            String sizeGuideText,
            String sizeGuideJson,
            LocalDateTime productCreatedAt,
            LocalDateTime productUpdatedAt,
            String productActiveStatus,
            List<OptionDto> options
    ) {
        this(productNo, productName, productType, productSubType, productPrice,
             productDescription, productImageUrl, sku, brandName, materialInfo, originCountry, manufactureCountry, careInstructions, sizeGuideText, sizeGuideJson, productCreatedAt, productUpdatedAt,
             productActiveStatus, null, options);
    }
    
    public ProductWithOptionsDto(
            Long productNo,
            String productName,
            String productType,
            String productSubType,
            String productPrice,
            String productDescription,
            String productImageUrl,
            String sku,
            String brandName,
            String materialInfo,
            String originCountry,
            String manufactureCountry,
            String careInstructions,
            String sizeGuideText,
            String sizeGuideJson,
            LocalDateTime productCreatedAt,
            LocalDateTime productUpdatedAt,
            String productActiveStatus,
            String rejectionReason,
            List<OptionDto> options
    ) {
        this.productNo = productNo;
        this.productName = productName;
        this.productType = productType;
        this.productSubType = productSubType;
        this.productPrice = productPrice;
        this.productDescription = productDescription;
        this.productImageUrl = productImageUrl;
        this.sku = sku;
        this.brandName = brandName;
        this.materialInfo = materialInfo;
        this.originCountry = originCountry;
        this.manufactureCountry = manufactureCountry;
        this.careInstructions = careInstructions;
        this.sizeGuideText = sizeGuideText;
        this.sizeGuideJson = sizeGuideJson;
        this.productCreatedAt = productCreatedAt;
        this.productUpdatedAt = productUpdatedAt;
        this.productActiveStatus = productActiveStatus;
        this.rejectionReason = rejectionReason;
        this.options = options;
    }

    public ProductWithOptionsDto(
            Long productNo,
            String productName,
            String productType,
            String productSubType,
            String productPrice,
            String productDescription,
            String productImageUrl,
            String sku,
            String brandName,
            String materialInfo,
            String originCountry,
            String manufactureCountry,
            String careInstructions,
            String sizeGuideText,
            LocalDateTime productCreatedAt,
            LocalDateTime productUpdatedAt,
            String productActiveStatus,
            String rejectionReason,
            List<OptionDto> options
    ) {
        this(productNo, productName, productType, productSubType, productPrice, productDescription, productImageUrl,
                sku, brandName, materialInfo, originCountry, manufactureCountry, careInstructions, sizeGuideText, null,
                productCreatedAt, productUpdatedAt, productActiveStatus, rejectionReason, options);
    }
}
