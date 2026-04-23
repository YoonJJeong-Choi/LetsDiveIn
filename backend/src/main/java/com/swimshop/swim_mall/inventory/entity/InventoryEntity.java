package com.swimshop.swim_mall.inventory.entity;

import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.product.entity.ProductEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryNo; //재고 고유식별자

    @Column(nullable = false)
    private Integer inventoryStock; // 재고수량

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_no", nullable = true)
    private OptionEntity option; //옵션 조인 (옵션이 있는 상품의 경우)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_no", nullable = true)
    private ProductEntity product; //상품 조인 (옵션이 없는 상품의 경우)

    /**
     * 엔티티 저장/수정 전 검증: option과 product 중 하나는 반드시 존재해야 함
     */
    @PrePersist
    @PreUpdate
    private void validate() {
        if (option == null && product == null) {
            throw new IllegalStateException("재고는 옵션이나 상품 중 하나는 반드시 연결되어야 합니다.");
        }
        if (option != null && product != null) {
            throw new IllegalStateException("재고는 옵션이나 상품 중 하나만 연결되어야 합니다.");
        }
    }

    /**
     * 재고 수량 업데이트
     * @param newStock 새로운 재고 수량 (0 이상)
     */
    public void updateStock(Integer newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
        this.inventoryStock = newStock;
    }

    /**
     * 재고 증가
     * @param quantity 증가할 수량
     */
    public void increaseStock(Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("증가 수량은 0 이상이어야 합니다.");
        }
        this.inventoryStock += quantity;
    }

    /**
     * 재고 감소
     * @param quantity 감소할 수량
     */
    public void decreaseStock(Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("감소 수량은 0 이상이어야 합니다.");
        }
        if (this.inventoryStock < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + this.inventoryStock);
        }
        this.inventoryStock -= quantity;
    }

    /**
     * 재고가 있는지 확인
     * @return 재고가 1 이상이면 true
     */
    public boolean isInStock() {
        return this.inventoryStock > 0;
    }
}
