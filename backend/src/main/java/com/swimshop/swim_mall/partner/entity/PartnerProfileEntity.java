package com.swimshop.swim_mall.partner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_profile")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PartnerProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false, unique = true)
    private PartnerEntity partner;

    @Column(nullable = true, length = 100)
    private String contactPersonName;

    @Column(nullable = true, length = 20)
    private String customerServicePhone;

    @Column(nullable = true, length = 500)
    private String profileImageUrl;

    @Column(nullable = true, length = 2000)
    private String introduction;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
