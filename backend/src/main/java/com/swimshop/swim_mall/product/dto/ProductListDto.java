package com.swimshop.swim_mall.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

/**
 * 고객용 상품 목록 조회 응답 DTO
 */
@Getter
public class ProductListDto {
    
    private Long productNo;
    private String productName;
    private String productType; // 대분류
    private String productSubType; // 소분류
    private String productPrice; // 기본 가격
    private String productDescription; // 상품 설명
    private String productImageUrl;
    private LocalDateTime productCreatedAt;
    
    // 옵션 정보 (최소 가격 계산용)
    private Long minPrice; // 최소 가격 (기본 가격 + 최소 옵션 추가 가격)
    private Long maxPrice; // 최대 가격 (기본 가격 + 최대 옵션 추가 가격)
    private List<ProductOptionDto> options; // 옵션 목록
    // 공개용 이미지 배열 (대표/정렬 포함)
    private List<ProductImageDto> images;
    
    // 관리자용: 상품 상태 (고객용 API에서는 null)
    private String productActiveStatus; // PENDING, ACTIVE, REJECTED, INACTIVE
    
    // 파트너용: 거절 사유 (REJECTED 상태일 때만 값이 있음)
    private String rejectionReason;
    
    // 관리자용: 파트너 정보 (상품 승인 시 필요)
    private Long partnerId;
    private String partnerName;
    private String partnerContact;
    private String partnerEmail;
    
    public ProductListDto(
            Long productNo,
            String productName,
            String productType,
            String productSubType,
            String productPrice,
            String productDescription,
            String productImageUrl,
            LocalDateTime productCreatedAt,
            Long minPrice,
            Long maxPrice,
            List<ProductOptionDto> options,
            List<ProductImageDto> images
    ) {
        this(productNo, productName, productType, productSubType, productPrice, 
             productDescription, productImageUrl, productCreatedAt, 
             minPrice, maxPrice, options, images, null, null);
    }
    
    // 관리자용 생성자 (상태 포함)
    public ProductListDto(
            Long productNo,
            String productName,
            String productType,
            String productSubType,
            String productPrice,
            String productDescription,
            String productImageUrl,
            LocalDateTime productCreatedAt,
            Long minPrice,
            Long maxPrice,
            List<ProductOptionDto> options,
            List<ProductImageDto> images,
            String productActiveStatus
    ) {
        this(productNo, productName, productType, productSubType, productPrice,
             productDescription, productImageUrl, productCreatedAt,
             minPrice, maxPrice, options, images, productActiveStatus, null);
    }
    
    // 관리자/파트너용 생성자 (상태 및 거절 사유 포함)
    public ProductListDto(
            Long productNo,
            String productName,
            String productType,
            String productSubType,
            String productPrice,
            String productDescription,
            String productImageUrl,
            LocalDateTime productCreatedAt,
            Long minPrice,
            Long maxPrice,
            List<ProductOptionDto> options,
            List<ProductImageDto> images,
            String productActiveStatus,
            String rejectionReason
    ) {
        this(productNo, productName, productType, productSubType, productPrice,
             productDescription, productImageUrl, productCreatedAt,
             minPrice, maxPrice, options, images, productActiveStatus, rejectionReason,
             null, null, null, null);
    }
    
    // 관리자용 생성자 (상태, 거절 사유, 파트너 정보 포함)
    public ProductListDto(
            Long productNo,
            String productName,
            String productType,
            String productSubType,
            String productPrice,
            String productDescription,
            String productImageUrl,
            LocalDateTime productCreatedAt,
            Long minPrice,
            Long maxPrice,
            List<ProductOptionDto> options,
            List<ProductImageDto> images,
            String productActiveStatus,
            String rejectionReason,
            Long partnerId,
            String partnerName,
            String partnerContact,
            String partnerEmail
    ) {
        this.productNo = productNo;
        this.productName = productName;
        this.productType = productType;
        this.productSubType = productSubType;
        this.productPrice = productPrice;
        this.productDescription = productDescription;
        this.productImageUrl = productImageUrl;
        this.productCreatedAt = productCreatedAt;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.options = options;
        this.images = images;
        this.productActiveStatus = productActiveStatus;
        this.rejectionReason = rejectionReason;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.partnerContact = partnerContact;
        this.partnerEmail = partnerEmail;
    }
}
