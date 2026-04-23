package com.swimshop.swim_mall.payment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_webhook_log", indexes = {
        @Index(name = "idx_webhook_event_id", columnList = "eventId", unique = true)
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WebhookLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String eventId;

    @Column(nullable = true, length = 100)
    private String type;

    @Column(nullable = true, length = 500)
    private String signature;

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    private String payload; // raw body

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(nullable = true, length = 500)
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime processedAt;

    public void markProcessed() {
        this.processed = true;
        this.errorMessage = null;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.processed = false;
        this.errorMessage = error;
        this.processedAt = LocalDateTime.now();
    }
}

