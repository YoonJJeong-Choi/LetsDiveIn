package com.swimshop.swim_mall.cart.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartNo; // 장바구니 고유식별자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true, nullable = false)
    private CustomerEntity customer; // 고객 (1:1 관계)

    @Column(nullable = false)
    private LocalDateTime cartCreatedAt; // 장바구니 생성일시

    @Column(nullable = true)
    private LocalDateTime cartUpdatedAt; // 장바구니 수정일시

    @PrePersist
    protected void onCreate() {
        this.cartCreatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.cartUpdatedAt = LocalDateTime.now();
    }
}
