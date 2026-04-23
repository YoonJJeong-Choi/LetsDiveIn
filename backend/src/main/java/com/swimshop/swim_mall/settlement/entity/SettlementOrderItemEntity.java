package com.swimshop.swim_mall.settlement.entity;

import com.swimshop.swim_mall.order.entity.OrderItemEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settlement_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SettlementOrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private SettlementEntity settlement; // 정산

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_no", nullable = false)
    private OrderItemEntity orderItem; // 주문 아이템

    /**
     * 정산-주문 아이템 관계 생성
     */
    public static SettlementOrderItemEntity create(SettlementEntity settlement, OrderItemEntity orderItem) {
        SettlementOrderItemEntity entity = new SettlementOrderItemEntity();
        entity.settlement = settlement;
        entity.orderItem = orderItem;
        return entity;
    }
}
