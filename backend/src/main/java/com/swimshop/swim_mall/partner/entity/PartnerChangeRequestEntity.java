package com.swimshop.swim_mall.partner.entity;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.PartnerChangeRequestStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_change_request")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PartnerChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    @Column(nullable = true, length = 100)
    private String requestedPartnerName;

    @Column(nullable = true, length = 50)
    private String requestedPartnerBankAccount;

    @Column(nullable = true, length = 12)
    private String requestedBusinessRegistrationNumber;

    @Column(nullable = true, length = 100)
    private String requestedRepresentativeBrandCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartnerChangeRequestStatus status;

    @Column(nullable = true, length = 1000)
    private String requestReason;

    @Column(nullable = true, length = 1000)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_admin_id")
    private AdminEntity processedByAdmin;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
