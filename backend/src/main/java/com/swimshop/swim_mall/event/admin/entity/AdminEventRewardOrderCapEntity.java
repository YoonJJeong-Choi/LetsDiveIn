package com.swimshop.swim_mall.event.admin.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.event.entity.EventEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "admin_event_reward_order_cap",
        uniqueConstraints = @UniqueConstraint(columnNames = { "event_no", "customer_id", "order_no" })
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminEventRewardOrderCapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long capNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_no", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "order_no", nullable = false)
    private Long orderNo;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AdminEventRewardOrderCapEntity(EventEntity event, CustomerEntity customer, Long orderNo) {
        this.event = event;
        this.customer = customer;
        this.orderNo = orderNo;
        this.createdAt = LocalDateTime.now();
    }
}
