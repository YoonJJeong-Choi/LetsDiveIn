package com.swimshop.swim_mall.ai.reviewanalysis.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_analysis_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewAnalysisLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = 100)
    private String partnerId;

    @Column(nullable = true)
    private LocalDateTime fromAt;

    @Column(nullable = true)
    private LocalDateTime toAt;

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    private String productNosPayload;

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    private String requestPayload;

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    private String responsePayload;

    @Column(nullable = false)
    private Boolean success;

    @Column(nullable = true, length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private Long latencyMs;

    @Column(nullable = true, length = 50)
    private String model;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
