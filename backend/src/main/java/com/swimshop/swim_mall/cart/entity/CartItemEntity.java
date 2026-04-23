package com.swimshop.swim_mall.cart.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.product.entity.ProductEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_item")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemNo; // 장바구니 아이템 고유식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_no", nullable = false)
    private CartEntity cart; // 장바구니

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_no", nullable = false)
    private ProductEntity product; // 상품

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_no", nullable = true)
    private OptionEntity option; // 옵션 (nullable)

    @Column(nullable = false)
    private Integer quantity; // 수량

    @Column(nullable = false)
    private Long itemPrice; // 장바구니 추가 시점의 가격 (옵션 포함)

    @Column(nullable = false)
    private LocalDateTime cartItemCreatedAt; // 생성일시

    @Column(nullable = true)
    private LocalDateTime cartItemUpdatedAt; // 수정일시

    @PrePersist
    protected void onCreate() {
        this.cartItemCreatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.cartItemUpdatedAt = LocalDateTime.now();
    }

    /**
     * 수량 업데이트
     */
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 옵션 및 가격 업데이트
     */
    public void updateOptionAndPrice(OptionEntity option, Long itemPrice) {
        this.option = option;
        this.itemPrice = itemPrice;
    }
}
