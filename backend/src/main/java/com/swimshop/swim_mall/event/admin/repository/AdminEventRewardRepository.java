package com.swimshop.swim_mall.event.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.event.admin.entity.AdminEventRewardEntity;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminEventRewardRepository extends JpaRepository<AdminEventRewardEntity, Long> {

    boolean existsByEvent_EventNoAndOrderItemNo(Long eventNo, Long orderItemNo);

    long countByEvent_EventNoAndCustomer_CustomerId(Long eventNo, Long customerId);

    // 기간 미적용 합계 (조인 없이)
    @Query("""
        select coalesce(sum(r.pointAmount),0)
        from AdminEventRewardEntity r
        where r.event.eventNo = :eventNo
    """)
    Long sumPointAmountByEventNo(@Param("eventNo") Long eventNo);

    // 기간 미적용 order_item_no 목록
    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        where r.event.eventNo = :eventNo
    """)
    List<Long> findOrderItemNosByEventNo(@Param("eventNo") Long eventNo);

    @Query("""
        select coalesce(sum(r.pointAmount),0)
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo = :eventNo
          and (:fromAt is null or oi.completedAt >= :fromAt)
          and (:toAt is null or oi.completedAt <= :toAt)
    """)
    Long sumPointAmountByEventNoAndCompletedBetween(@Param("eventNo") Long eventNo,
                                                    @Param("fromAt") LocalDateTime fromAt,
                                                    @Param("toAt") LocalDateTime toAt);

    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo = :eventNo
          and (:fromAt is null or oi.completedAt >= :fromAt)
          and (:toAt is null or oi.completedAt <= :toAt)
    """)
    List<Long> findOrderItemNosByEventNoAndCompletedBetween(@Param("eventNo") Long eventNo,
                                                            @Param("fromAt") LocalDateTime fromAt,
                                                            @Param("toAt") LocalDateTime toAt);

    // 단일 이벤트용 (NULL 파라미터 회피)
    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        where r.event.eventNo = :eventNo
    """)
    List<Long> findOrderItemNosByEventNoOnly(@Param("eventNo") Long eventNo);

    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo = :eventNo
          and oi.completedAt >= :fromAt
          and oi.completedAt <= :toAt
    """)
    List<Long> findOrderItemNosByEventNoBetweenRequired(@Param("eventNo") Long eventNo,
                                                        @Param("fromAt") LocalDateTime fromAt,
                                                        @Param("toAt") LocalDateTime toAt);

    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo = :eventNo
          and oi.completedAt >= :fromAt
    """)
    List<Long> findOrderItemNosByEventNoFrom(@Param("eventNo") Long eventNo,
                                             @Param("fromAt") LocalDateTime fromAt);

    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo = :eventNo
          and oi.completedAt <= :toAt
    """)
    List<Long> findOrderItemNosByEventNoTo(@Param("eventNo") Long eventNo,
                                           @Param("toAt") LocalDateTime toAt);
    // 여러 이벤트 집합용 (NULL 파라미터 회피를 위해 조건별 메서드 분리)
    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        where r.event.eventNo in :eventNos
    """)
    List<Long> findOrderItemNosByEventNos(@Param("eventNos") List<Long> eventNos);

    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo in :eventNos
          and oi.completedAt >= :fromAt
          and oi.completedAt <= :toAt
    """)
    List<Long> findOrderItemNosByEventNosAndCompletedBetweenRequired(@Param("eventNos") List<Long> eventNos,
                                                                     @Param("fromAt") LocalDateTime fromAt,
                                                                     @Param("toAt") LocalDateTime toAt);

    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo in :eventNos
          and oi.completedAt >= :fromAt
    """)
    List<Long> findOrderItemNosByEventNosAndCompletedFrom(@Param("eventNos") List<Long> eventNos,
                                                          @Param("fromAt") LocalDateTime fromAt);

    @Query("""
        select r.orderItemNo
        from AdminEventRewardEntity r
        join com.swimshop.swim_mall.order.entity.OrderItemEntity oi on oi.orderItemNo = r.orderItemNo
        where r.event.eventNo in :eventNos
          and oi.completedAt <= :toAt
    """)
    List<Long> findOrderItemNosByEventNosAndCompletedTo(@Param("eventNos") List<Long> eventNos,
                                                        @Param("toAt") LocalDateTime toAt);
}

