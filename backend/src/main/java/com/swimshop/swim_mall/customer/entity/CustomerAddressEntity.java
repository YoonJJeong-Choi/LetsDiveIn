package com.swimshop.swim_mall.customer.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_address")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressNo; // 주소 고유식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer; // 고객 조인

    @Column(nullable = false)
    private String recipientName; // 수령인 이름

    @Column(nullable = false)
    private String recipientPhone; // 수령인 전화번호

    @Column(nullable = false)
    private String deliveryAddress; // 배송지 주소

    @Column(nullable = true)
    private String deliveryAddressDetail; // 배송지 상세 주소

    @Column(nullable = true)
    private String deliveryZipCode; // 우편번호

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false; // 기본 주소 여부

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성일시

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(); // 수정일시

    // 기본 주소 설정 메서드
    public void setAsDefault() {
        this.isDefault = true;
    }

    // 기본 주소 해제 메서드
    public void unsetAsDefault() {
        this.isDefault = false;
    }

    // 주소 정보 업데이트 메서드
    public void updateAddress(String recipientName, String recipientPhone, 
                             String deliveryAddress, String deliveryAddressDetail, 
                             String deliveryZipCode) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.deliveryAddress = deliveryAddress;
        this.deliveryAddressDetail = deliveryAddressDetail;
        this.deliveryZipCode = deliveryZipCode;
        this.updatedAt = LocalDateTime.now();
    }
}
