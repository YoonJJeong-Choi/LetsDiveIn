package com.swimshop.swim_mall.delivery.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "delivery")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryNo; //배송 고유식별자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_no", nullable = false, unique = true)
    private OrderItemEntity orderItem; //주문 상품 조인 (1:1)

    @Column(nullable = true)
    private LocalDateTime deliveryStartDate; //배송 시작일

    @Column(nullable = true)
    private LocalDateTime deliveryEndDate; //배송 완료일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.READY; //배송 상태 (기본값: READY)

    @Column(nullable = true)
    private String deliveryTrackingNumber; //배송 송장번호

    @Column(nullable = true)
    private String deliveryCourier; //배송 택배사
    
    /**
     * 배송 시작 (READY → SHIPPED)
     */
    public void startDelivery(String trackingNumber, String courier) {
        if (this.deliveryStatus != DeliveryStatus.READY) {
            throw new IllegalStateException("배송 준비 상태인 배송만 시작할 수 있습니다. 현재 상태: " + this.deliveryStatus.getLabel());
        }
        this.deliveryStatus = DeliveryStatus.SHIPPED;
        this.deliveryStartDate = LocalDateTime.now();
        this.deliveryTrackingNumber = trackingNumber;
        this.deliveryCourier = courier;
    }
    
    /**
     * 배송 완료 (SHIPPED → DELIVERED)
     */
    public void completeDelivery() {
        if (this.deliveryStatus != DeliveryStatus.SHIPPED) {
            throw new IllegalStateException("배송 중 상태인 배송만 완료할 수 있습니다. 현재 상태: " + this.deliveryStatus.getLabel());
        }
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveryEndDate = LocalDateTime.now();
    }
    
    /**
     * 배송 정보 수정 (송장번호, 택배사)
     */
    public void updateTrackingInfo(String trackingNumber, String courier) {
        if (this.deliveryStatus == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("배송 완료된 배송의 정보는 수정할 수 없습니다.");
        }
        this.deliveryTrackingNumber = trackingNumber;
        this.deliveryCourier = courier;
    }
}
