package com.swimshop.swim_mall.event.partner.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.partner.entity.PartnerEventParticipationEntity;
import com.swimshop.swim_mall.common.enums.EventStatus;

@Repository
public interface PartnerEventParticipationRepository extends JpaRepository<PartnerEventParticipationEntity, Long> {

    PartnerEventParticipationEntity findByPartner_PartnerIdAndEvent_EventNo(Long partnerId, Long eventNo);

    List<PartnerEventParticipationEntity> findByEvent_EventNoOrderByCreatedAtDesc(Long eventNo);

    @Query("""
            select p.event.eventNo
            from PartnerEventParticipationEntity p
            where p.partner.partnerId = :partnerId
              and p.active = true
              and p.event.eventNo in :eventNos
            """)
    Set<Long> findActiveParticipatingEventNos(
            @Param("partnerId") Long partnerId,
            @Param("eventNos") List<Long> eventNos
    );

    @Query("""
            select p.event
            from PartnerEventParticipationEntity p
            join p.event e
            where p.partner.partnerId = :partnerId
              and p.active = true
              and e.eventStatus = :activeStatus
              and e.partnerApplyEnabled = true
              and :at between e.partnerApplyStartAt and e.partnerApplyEndAt
              and :at between e.customerEventStartAt and e.customerEventEndAt
            """)
    List<EventEntity> findEligibleEventsForPartnerAt(
            @Param("partnerId") Long partnerId,
            @Param("at") LocalDateTime at,
            @Param("activeStatus") EventStatus activeStatus
    );

    @Query("""
            select p.event.eventNo
            from PartnerEventParticipationEntity p
            join p.event e
            where p.partner.partnerId = :partnerId
              and p.active = true
              and e.eventStatus = :activeStatus
              and e.eventMode = com.swimshop.swim_mall.common.enums.EventMode.PARTNER_PARTICIPATION
              and :at between e.customerEventStartAt and e.customerEventEndAt
            """)
    List<Long> findSnapshotEligibleEventNosForPartnerAt(
            @Param("partnerId") Long partnerId,
            @Param("at") LocalDateTime at,
            @Param("activeStatus") EventStatus activeStatus
    );
}

