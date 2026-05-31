package com.swimshop.swim_mall.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.common.enums.ProductType;
import com.swimshop.swim_mall.common.enums.ProductSubType;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

@Entity
@Table(name="product")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productNo; //상품 고유식별자

    @Column(nullable = false)
    private String productName; //상품 이름
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType; //상품 대분류 카테고리

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ProductSubType productSubType; //상품 소분류 (예: 원피스, 비키니 등)
    
    @Column(nullable = false)
    private String productPrice; //상품 가격
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String productDescription; //상품 설명

    @Column(nullable = false)
    private String productImageUrl; //상품 이미지

    @Column(nullable = true, length = 100)
    private String sku;

    @Column(nullable = true, length = 100)
    private String brandName;

    @Column(nullable = true, length = 1000)
    private String materialInfo;

    @Column(nullable = true, length = 100)
    private String originCountry;

    @Column(nullable = true, length = 100)
    private String manufactureCountry;

    @Column(nullable = true, length = 1000)
    private String careInstructions;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String sizeGuideText;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String sizeGuideJson;

    @Column(nullable = false)
    private LocalDateTime productCreatedAt; //상품 생성일

    @Column(nullable = true)
    private LocalDateTime productUpdatedAt; //상품 수정일

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActiveStatus productActiveStatus; //상품 삭제여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = true, referencedColumnName = "partner_id")
    private PartnerEntity partner; //입점 업체 (옵션이 없는 단일 상품의 경우 파트너 정보)

    @Column(nullable = true, length = 1000)
    private String rejectionReason; // 거절 사유 (관리자가 거절 시 입력)

    // 수정 취소를 위한 원본 데이터 저장 필드 (PENDING_UPDATE 상태일 때만 사용)
    @Column(nullable = true)
    private String originalProductName; // 원본 상품명
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ProductType originalProductType; // 원본 상품 대분류
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ProductSubType originalProductSubType; // 원본 상품 소분류
    
    @Column(nullable = true)
    private String originalProductPrice; // 원본 상품 가격
    
    @Column(nullable = true, columnDefinition = "TEXT")
    private String originalProductDescription; // 원본 상품 설명
    
    @Column(nullable = true)
    private String originalProductImageUrl; // 원본 상품 이미지

    @Column(nullable = true, length = 100)
    private String originalSku;

    @Column(nullable = true, length = 100)
    private String originalBrandName;

    @Column(nullable = true, length = 1000)
    private String originalMaterialInfo;

    @Column(nullable = true, length = 100)
    private String originalOriginCountry;

    @Column(nullable = true, length = 100)
    private String originalManufactureCountry;

    @Column(nullable = true, length = 1000)
    private String originalCareInstructions;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String originalSizeGuideText;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String originalSizeGuideJson;

    // 상품 정보 업데이트 메서드
    public void update(
            String productName,
            ProductType productType,
            ProductSubType productSubType,
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
            ActiveStatus productActiveStatus
    ) {
        // ACTIVE → PENDING_UPDATE로 변경될 때만 원본 데이터 저장
        // (PENDING_UPDATE 상태에서 다시 수정할 때는 원본 데이터를 덮어쓰지 않음)
        if (this.productActiveStatus == ActiveStatus.ACTIVE && productActiveStatus == ActiveStatus.PENDING_UPDATE) {
            // 원본 데이터가 아직 저장되지 않은 경우에만 저장
            if (this.originalProductName == null) {
                this.originalProductName = this.productName;
                this.originalProductType = this.productType;
                this.originalProductSubType = this.productSubType;
                this.originalProductPrice = this.productPrice;
                this.originalProductDescription = this.productDescription;
                this.originalProductImageUrl = this.productImageUrl;
                this.originalSku = this.sku;
                this.originalBrandName = this.brandName;
                this.originalMaterialInfo = this.materialInfo;
                this.originalOriginCountry = this.originCountry;
                this.originalManufactureCountry = this.manufactureCountry;
                this.originalCareInstructions = this.careInstructions;
                this.originalSizeGuideText = this.sizeGuideText;
                this.originalSizeGuideJson = this.sizeGuideJson;
            }
        }
        
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
        this.productActiveStatus = productActiveStatus;
        this.productUpdatedAt = LocalDateTime.now(); // 수정일시 업데이트
    }

    /**
     * 상품 비활성화 (상태를 INACTIVE로 변경)
     * 고객 목록에는 표시되지 않지만, 파트너는 관리 가능
     */
    public void deactivate() {
        this.productActiveStatus = ActiveStatus.INACTIVE;
        this.productUpdatedAt = LocalDateTime.now();
    }

    /**
     * 상품 활성화 (상태를 ACTIVE로 변경)
     * 승인 시 거절 사유도 초기화
     */
    public void activate() {
        this.productActiveStatus = ActiveStatus.ACTIVE;
        this.rejectionReason = null; // 승인 시 거절 사유 초기화
        this.productUpdatedAt = LocalDateTime.now();
    }

    /**
     * 상품 거절 (상태를 REJECTED로 변경)
     * 관리자가 승인 거절 시 사용
     * @param rejectionReason 거절 사유
     */
    public void reject(String rejectionReason) {
        this.productActiveStatus = ActiveStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.productUpdatedAt = LocalDateTime.now();
    }

    /**
     * 상품 재신청
     * 파트너가 거절된 상품을 수정 후 재신청할 때 사용
     * 상태는 update 메서드에서 이미 PENDING으로 설정됨
     * 거절 사유는 승인 시에만 초기화됨 (재신청 시에는 유지)
     */
    public void resubmit() {
        // 거절 사유는 승인 시에만 초기화 (재신청 시에는 유지하여 파트너가 확인 가능)
    }

    /**
     * 상품 수정 거절 (PENDING_UPDATE → ACTIVE)
     * 관리자가 수정 승인을 거절할 때 사용
     * 수정 내용은 거절되지만 원래 상품은 계속 판매 가능
     * 원본 데이터로 복구하여 수정 내용을 취소함
     * @param rejectionReason 거절 사유
     */
    public void rejectUpdate(String rejectionReason) {
        // 원본 데이터로 복구
        if (this.originalProductName != null) {
            this.productName = this.originalProductName;
            this.productType = this.originalProductType;
            this.productSubType = this.originalProductSubType;
            this.productPrice = this.originalProductPrice;
            this.productDescription = this.originalProductDescription;
            this.productImageUrl = this.originalProductImageUrl;
            this.sku = this.originalSku;
            this.brandName = this.originalBrandName;
            this.materialInfo = this.originalMaterialInfo;
            this.originCountry = this.originalOriginCountry;
            this.manufactureCountry = this.originalManufactureCountry;
            this.careInstructions = this.originalCareInstructions;
            this.sizeGuideText = this.originalSizeGuideText;
            this.sizeGuideJson = this.originalSizeGuideJson;
            
            // 원본 데이터 초기화
            this.originalProductName = null;
            this.originalProductType = null;
            this.originalProductSubType = null;
            this.originalProductPrice = null;
            this.originalProductDescription = null;
            this.originalProductImageUrl = null;
            this.originalSku = null;
            this.originalBrandName = null;
            this.originalMaterialInfo = null;
            this.originalOriginCountry = null;
            this.originalManufactureCountry = null;
            this.originalCareInstructions = null;
            this.originalSizeGuideText = null;
            this.originalSizeGuideJson = null;
        }
        this.productActiveStatus = ActiveStatus.ACTIVE; // 원래 ACTIVE 상태로 복구
        this.rejectionReason = rejectionReason; // 거절 사유 저장 (파트너가 확인 가능)
        this.productUpdatedAt = LocalDateTime.now();
    }

    /**
     * 파트너 설정 (더미 데이터 초기화용)
     */
    public void setPartner(PartnerEntity partner) {
        this.partner = partner;
    }

    /**
     * 재심사 필수 항목 변경 여부 확인
     * 재심사 필수 항목: 상품명, 카테고리, 소분류, 가격
     * 
     * @param newProductName 새로운 상품명
     * @param newProductType 새로운 카테고리
     * @param newProductSubType 새로운 소분류
     * @param newProductPrice 새로운 가격
     * @return 재심사 필수 항목이 변경되었으면 true
     */
    public boolean hasCriticalFieldsChanged(
            String newProductName,
            ProductType newProductType,
            ProductSubType newProductSubType,
            String newProductPrice
    ) {
        // 상품명 변경 확인
        if (!this.productName.equals(newProductName)) {
            return true;
        }
        
        // 카테고리 변경 확인
        if (this.productType != newProductType) {
            return true;
        }
        
        // 소분류 변경 확인 (null 처리 포함)
        if (this.productSubType != newProductSubType) {
            return true;
        }
        
        // 가격 변경 확인
        if (!this.productPrice.equals(newProductPrice)) {
            return true;
        }
        
        return false;
    }

    /**
     * 상품 수정 승인 대기 상태로 변경 (PENDING_UPDATE)
     * 활성 상품의 재심사 필수 항목 수정 시 사용
     * 원본 데이터는 update() 메서드에서 ACTIVE → PENDING_UPDATE로 변경될 때 저장됨
     */
    public void setPendingUpdate() {
        // 원본 데이터는 update() 메서드에서 이미 저장되었으므로 여기서는 상태만 변경
        this.productActiveStatus = ActiveStatus.PENDING_UPDATE;
        this.productUpdatedAt = LocalDateTime.now();
    }

    /**
     * 수정 신청 취소 (PENDING_UPDATE → ACTIVE)
     * 파트너가 수정 신청을 취소할 때 사용
     * 수정 내용은 취소되고 원래 상태로 복구됨
     */
    public void cancelUpdate() {
        if (this.productActiveStatus == ActiveStatus.PENDING_UPDATE) {
            // 원본 데이터로 복구
            if (this.originalProductName != null) {
                this.productName = this.originalProductName;
                this.productType = this.originalProductType;
                this.productSubType = this.originalProductSubType;
                this.productPrice = this.originalProductPrice;
                this.productDescription = this.originalProductDescription;
                this.productImageUrl = this.originalProductImageUrl;
                this.sku = this.originalSku;
                this.brandName = this.originalBrandName;
                this.materialInfo = this.originalMaterialInfo;
                this.originCountry = this.originalOriginCountry;
                this.manufactureCountry = this.originalManufactureCountry;
                this.careInstructions = this.originalCareInstructions;
                this.sizeGuideText = this.originalSizeGuideText;
                this.sizeGuideJson = this.originalSizeGuideJson;
                
                // 원본 데이터 초기화
                this.originalProductName = null;
                this.originalProductType = null;
                this.originalProductSubType = null;
                this.originalProductPrice = null;
                this.originalProductDescription = null;
                this.originalProductImageUrl = null;
                this.originalSku = null;
                this.originalBrandName = null;
                this.originalMaterialInfo = null;
                this.originalOriginCountry = null;
                this.originalManufactureCountry = null;
                this.originalCareInstructions = null;
                this.originalSizeGuideText = null;
                this.originalSizeGuideJson = null;
            }
            this.productActiveStatus = ActiveStatus.ACTIVE; // 원래 ACTIVE 상태로 복구
            this.rejectionReason = null; // 취소 시 거절 사유 초기화
            this.productUpdatedAt = LocalDateTime.now();
        }
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
