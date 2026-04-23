package com.swimshop.swim_mall.event.partner.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.event.entity.EventEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "partner_event_reward",
        uniqueConstraints = @UniqueConstraint(columnNames = { "event_no", "order_item_no" })
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartnerEventRewardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rewardNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_no", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(nullable = false)
    private Long partnerId;

    @Column(name = "order_item_no", nullable = false)
    private Long orderItemNo;

    @Column(nullable = false)
    private Long pointAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PartnerEventRewardEntity(EventEntity event, CustomerEntity customer, Long partnerId, Long orderItemNo, Long pointAmount) {
        this.event = event;
        this.customer = customer;
        this.partnerId = partnerId;
        this.orderItemNo = orderItemNo;
        this.pointAmount = pointAmount;
    }
}

