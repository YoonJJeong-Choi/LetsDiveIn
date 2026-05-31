package com.swimshop.swim_mall.qna.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.enums.QnaScope;
import com.swimshop.swim_mall.common.enums.QnaStatus;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;

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
@Table(name = "qna")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QnaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qna_no")
    private Long qnaNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InquiryCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private QnaScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QnaStatus status;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_no")
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_no")
    private OrderItemEntity orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private PartnerEntity partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_no")
    private ReturnEntity returnOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime answeredAt;

    public void markAnswered(LocalDateTime at) {
        this.status = QnaStatus.ANSWERED;
        this.answeredAt = at;
        this.updatedAt = at;
    }

    public void touchUpdated(LocalDateTime at) {
        this.updatedAt = at;
    }

    public Long getPartnerId() {
        return partner != null ? partner.getPartnerId() : null;
    }
}
