package com.swimshop.swim_mall.event.partner.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.partner.ParticipationScope;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "partner_event_participation",
        uniqueConstraints = @UniqueConstraint(columnNames = { "partner_id", "event_no" })
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartnerEventParticipationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_no", nullable = false)
    private EventEntity event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParticipationScope scope = ParticipationScope.STORE_LEVEL;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PartnerEventParticipationEntity(PartnerEntity partner, EventEntity event) {
        this.partner = partner;
        this.event = event;
        this.scope = ParticipationScope.STORE_LEVEL;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}

