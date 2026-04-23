package com.swimshop.swim_mall.sale.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.SalePolicyHistoryAction;
import com.swimshop.swim_mall.common.enums.SaleStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sale_policy_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SalePolicyHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_policy_id", nullable = false)
    private SalePolicyEntity salePolicy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SalePolicyHistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole changedByRole;

    @Column(nullable = false)
    private Long changedById;

    @Column(nullable = true, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private SaleStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private SaleStatus afterStatus;

    @Column(nullable = true)
    private Long beforeEventNo;

    @Column(nullable = true)
    private Long afterEventNo;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}

