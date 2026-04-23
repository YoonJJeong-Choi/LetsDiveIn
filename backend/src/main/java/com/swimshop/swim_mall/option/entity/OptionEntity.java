package com.swimshop.swim_mall.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.product.entity.ProductEntity;

@Entity
@Table(name = "option")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long optionNo; //옵션 고유식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_no", nullable = false)
    private ProductEntity product; //상품 조인

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false, referencedColumnName = "partner_id")
    private PartnerEntity partner; //입점 업체

    @Column(nullable = true)
    private String color; // 색상 (예: "빨강", "파랑", "블랙")

    @Column(nullable = true)
    private String size; // 사이즈 (예: "S", "M", "L", "XL", "일반")

    @Column(nullable = true)
    private Long optionAddPrice; // 추가 가격 (null이면 기본 가격, 원 단위)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ActiveStatus optionStatus = ActiveStatus.ACTIVE; // 옵션 상태 (ACTIVE, INACTIVE)

    // 수정 취소를 위한 원본 데이터 저장 필드 (PENDING_UPDATE 상태일 때만 사용)
    @Column(nullable = true)
    private String originalColor; // 원본 색상
    
    @Column(nullable = true)
    private String originalSize; // 원본 사이즈
    
    @Column(nullable = true)
    private Long originalOptionAddPrice; // 원본 추가 가격

    /**
     * 엔티티 저장 전 optionStatus 자동 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.optionStatus == null) {
            this.optionStatus = ActiveStatus.ACTIVE;
        }
    }

    /**
     * 옵션 정보 업데이트
     */
    public void update(String color, String size, Long optionAddPrice, ActiveStatus optionStatus) {
        // ACTIVE → PENDING_UPDATE로 변경될 때만 원본 데이터 저장
        // (PENDING_UPDATE 상태에서 다시 수정할 때는 원본 데이터를 덮어쓰지 않음)
        if (this.optionStatus == ActiveStatus.ACTIVE && optionStatus == ActiveStatus.PENDING_UPDATE) {
            // 원본 데이터가 아직 저장되지 않은 경우에만 저장
            if (this.originalColor == null && this.originalSize == null && this.originalOptionAddPrice == null) {
                this.originalColor = this.color;
                this.originalSize = this.size;
                this.originalOptionAddPrice = this.optionAddPrice;
            }
        }
        
        this.color = color;
        this.size = size;
        this.optionAddPrice = optionAddPrice;
        this.optionStatus = optionStatus;
    }

    /**
     * 옵션 비활성화 (상태를 INACTIVE로 변경)
     * 고객 목록에는 표시되지 않지만, 파트너는 관리 가능
     */
    public void deactivate() {
        this.optionStatus = ActiveStatus.INACTIVE;
    }

    /**
     * 옵션 활성화 (상태를 ACTIVE로 변경)
     * PENDING_UPDATE에서 승인 시 원본 데이터도 초기화
     */
    public void activate() {
        this.optionStatus = ActiveStatus.ACTIVE;
        
        // PENDING_UPDATE에서 승인된 경우 원본 데이터 초기화
        if (this.originalColor != null || this.originalSize != null || this.originalOptionAddPrice != null) {
            this.originalColor = null;
            this.originalSize = null;
            this.originalOptionAddPrice = null;
        }
    }
    
    /**
     * 옵션 수정 승인 대기 상태로 변경 (PENDING_UPDATE)
     * 원본 데이터는 update() 메서드에서 ACTIVE → PENDING_UPDATE로 변경될 때 저장됨
     */
    public void setPendingUpdate() {
        // 원본 데이터는 update() 메서드에서 이미 저장되었으므로 여기서는 상태만 변경
        this.optionStatus = ActiveStatus.PENDING_UPDATE;
    }
    
    /**
     * 옵션 수정 취소 (PENDING_UPDATE → ACTIVE)
     * 원본 데이터로 복구
     */
    public void cancelUpdate() {
        if (this.optionStatus == ActiveStatus.PENDING_UPDATE) {
            // 원본 데이터로 복구
            if (this.originalColor != null || this.originalSize != null || this.originalOptionAddPrice != null) {
                this.color = this.originalColor;
                this.size = this.originalSize;
                this.optionAddPrice = this.originalOptionAddPrice;
                
                // 원본 데이터 초기화
                this.originalColor = null;
                this.originalSize = null;
                this.originalOptionAddPrice = null;
            }
            this.optionStatus = ActiveStatus.ACTIVE;
        }
    }
    
    /**
     * 옵션 수정 거절 (PENDING_UPDATE → ACTIVE)
     * 원본 데이터로 복구
     */
    public void rejectUpdate() {
        // 원본 데이터로 복구
        if (this.originalColor != null || this.originalSize != null || this.originalOptionAddPrice != null) {
            this.color = this.originalColor;
            this.size = this.originalSize;
            this.optionAddPrice = this.originalOptionAddPrice;
            
            // 원본 데이터 초기화
            this.originalColor = null;
            this.originalSize = null;
            this.originalOptionAddPrice = null;
        }
        this.optionStatus = ActiveStatus.ACTIVE;
    }

    /**
     * 옵션 거절 (상태를 REJECTED로 변경)
     * 관리자가 승인 거절 시 사용
     */
    public void reject() {
        this.optionStatus = ActiveStatus.REJECTED;
    }

    /**
     * 옵션의 재심사 필수 항목이 변경되었는지 확인
     * (색상, 사이즈, 추가 가격)
     */
    public boolean hasCriticalFieldsChanged(
            String newColor,
            String newSize,
            Long newOptionAddPrice
    ) {
        // null 안전 비교
        boolean colorChanged = (this.color == null && newColor != null) ||
                               (this.color != null && !this.color.equals(newColor));
        boolean sizeChanged = (this.size == null && newSize != null) ||
                              (this.size != null && !this.size.equals(newSize));
        boolean priceChanged = (this.optionAddPrice == null && newOptionAddPrice != null) ||
                               (this.optionAddPrice != null && !this.optionAddPrice.equals(newOptionAddPrice));
        
        return colorChanged || sizeChanged || priceChanged;
    }
}
