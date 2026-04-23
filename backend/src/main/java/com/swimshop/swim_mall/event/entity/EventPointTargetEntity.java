package com.swimshop.swim_mall.event.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "event_point_target",
        uniqueConstraints = @UniqueConstraint(columnNames = { "event_no", "target_type", "target_value" })
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventPointTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_no", nullable = false)
    private EventEntity event;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PointEventTargetType targetType;

    @Column(name = "target_value", nullable = false, length = 100)
    private String targetValue;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EventPointTargetEntity(EventEntity event, PointEventTargetType targetType, String targetValue) {
        this.event = event;
        this.targetType = targetType;
        this.targetValue = targetValue;
        this.createdAt = LocalDateTime.now();
    }
}
