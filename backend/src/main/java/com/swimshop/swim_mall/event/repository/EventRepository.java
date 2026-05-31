package com.swimshop.swim_mall.event.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.event.entity.EventEntity;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @Query("SELECT e FROM EventEntity e " +
           "WHERE (:status IS NULL OR e.eventStatus = :status) " +
           "AND LOWER(e.eventTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND e.customerEventEndAt >= :startAt " +
           "AND e.customerEventStartAt <= :endAt " +
           "ORDER BY e.createdAt DESC")
    List<EventEntity> searchForAdmin(
            @Param("status") EventStatus status,
            @Param("keyword") String keyword,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("SELECT e FROM EventEntity e " +
           "WHERE e.eventStatus IN :statuses " +
           "AND e.customerExposeAt IS NOT NULL " +
           "AND e.customerExposeAt <= :now " +
           "AND (e.eventStatus <> com.swimshop.swim_mall.common.enums.EventStatus.PUBLISHED " +
           "     OR e.customerEventEndAt >= :now) " +
           "ORDER BY e.customerEventStartAt DESC, e.eventNo DESC")
    List<EventEntity> findPublicVisibleEvents(
            @Param("statuses") List<EventStatus> statuses,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT e FROM EventEntity e " +
           "WHERE e.eventNo = :eventNo " +
           "AND e.eventStatus IN :statuses " +
           "AND e.customerExposeAt IS NOT NULL " +
           "AND e.customerExposeAt <= :now " +
           "AND (e.eventStatus <> com.swimshop.swim_mall.common.enums.EventStatus.PUBLISHED " +
           "     OR e.customerEventEndAt >= :now)")
    java.util.Optional<EventEntity> findPublicVisibleEventDetail(
            @Param("eventNo") Long eventNo,
            @Param("statuses") List<EventStatus> statuses,
            @Param("now") LocalDateTime now
    );

    List<EventEntity> findByEventStatusAndCustomerEventEndAtBefore(EventStatus eventStatus, LocalDateTime endAt);

    @Query("SELECT e FROM EventEntity e " +
           "WHERE e.eventStatus = :activeStatus " +
           "AND (e.eventMode = :mode OR (:mode = com.swimshop.swim_mall.common.enums.EventMode.ADMIN_ONLY AND e.eventMode IS NULL)) " +
           "AND e.customerEventStartAt <= :now " +
           "AND e.customerEventEndAt >= :now")
    List<EventEntity> findActiveEventsByMode(
            @Param("activeStatus") EventStatus activeStatus,
            @Param("mode") EventMode mode,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT e FROM EventEntity e " +
           "WHERE e.eventMode = :mode " +
           "AND e.eventStatus IN :statuses " +
           "ORDER BY e.customerEventStartAt ASC, e.createdAt DESC")
    List<EventEntity> findPartnerVisibleEvents(
            @Param("mode") EventMode mode,
            @Param("statuses") List<EventStatus> statuses
    );

    @Query("SELECT e FROM EventEntity e " +
           "WHERE e.eventMode = :mode " +
           "AND e.eventStatus IN :statuses " +
           "AND e.customerEventStartAt <= :latestStartAt " +
           "ORDER BY e.customerEventStartAt ASC, e.createdAt DESC")
    List<EventEntity> findPartnerVisibleEventsWithStartAtLimit(
            @Param("mode") EventMode mode,
            @Param("statuses") List<EventStatus> statuses,
            @Param("latestStartAt") LocalDateTime latestStartAt
    );

    // 종료 상태 전체 조회 (조기 종료 포함)
    List<EventEntity> findByEventStatus(EventStatus status);
}

