package com.swimshop.swim_mall.event.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.PointEventTargetType;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventNo;

    @Column(nullable = false, length = 200)
    private String eventTitle;

    @Column(nullable = false, length = 4000)
    private String eventContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus eventStatus;

    @Column(nullable = false)
    private LocalDateTime customerEventStartAt;

    @Column(nullable = false)
    private LocalDateTime customerEventEndAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean partnerApplyEnabled = false;

    @Column(nullable = true)
    private LocalDateTime partnerApplyStartAt;

    @Column(nullable = true)
    private LocalDateTime partnerApplyEndAt;

    @Column(nullable = true, length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 30)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private DiscountType saleDiscountType;

    @Column(nullable = true)
    private Long saleDiscountValue;

    @Column(nullable = true)
    private Long saleMaxDiscountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 30)
    private PointEventTargetType pointEventTargetType;

    @Column(nullable = true, length = 1000)
    private String pointEventTargetValue;

    @Column(nullable = true)
    private Long pointEventMinOrderAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 30)
    @Builder.Default
    private EventMode eventMode = EventMode.ADMIN_ONLY;

    @Column(nullable = true, length = 1000)
    private String adminMemo;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_no", nullable = false)
    private AdminEntity admin;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.eventMode == null) this.eventMode = EventMode.ADMIN_ONLY;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            String eventTitle,
            String eventContent,
            EventStatus eventStatus,
            LocalDateTime customerEventStartAt,
            LocalDateTime customerEventEndAt,
            Boolean partnerApplyEnabled,
            LocalDateTime partnerApplyStartAt,
            LocalDateTime partnerApplyEndAt,
            String thumbnailUrl,
            EventType eventType,
            DiscountType saleDiscountType,
            Long saleDiscountValue,
            Long saleMaxDiscountAmount,
            PointEventTargetType pointEventTargetType,
            String pointEventTargetValue,
            Long pointEventMinOrderAmount,
            EventMode eventMode,
            String adminMemo) {
        this.eventTitle = eventTitle;
        this.eventContent = eventContent;
        this.eventStatus = eventStatus;
        this.customerEventStartAt = customerEventStartAt;
        this.customerEventEndAt = customerEventEndAt;
        this.partnerApplyEnabled = partnerApplyEnabled != null ? partnerApplyEnabled : false;
        this.partnerApplyStartAt = partnerApplyStartAt;
        this.partnerApplyEndAt = partnerApplyEndAt;
        this.thumbnailUrl = thumbnailUrl;
        this.eventType = eventType;
        this.saleDiscountType = saleDiscountType;
        this.saleDiscountValue = saleDiscountValue;
        this.saleMaxDiscountAmount = saleMaxDiscountAmount;
        this.pointEventTargetType = pointEventTargetType;
        this.pointEventTargetValue = pointEventTargetValue;
        this.pointEventMinOrderAmount = pointEventMinOrderAmount;
        this.eventMode = eventMode != null ? eventMode : EventMode.ADMIN_ONLY;
        this.adminMemo = adminMemo;
    }

    public void changeStatus(EventStatus eventStatus) {
        this.eventStatus = eventStatus;
    }
}
