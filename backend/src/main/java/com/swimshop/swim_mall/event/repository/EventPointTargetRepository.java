package com.swimshop.swim_mall.event.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.PointEventTargetType;
import com.swimshop.swim_mall.event.entity.EventPointTargetEntity;

@Repository
public interface EventPointTargetRepository extends JpaRepository<EventPointTargetEntity, Long> {

    List<EventPointTargetEntity> findByEvent_EventNo(Long eventNo);

    void deleteByEvent_EventNo(Long eventNo);

    boolean existsByEvent_EventNoAndTargetTypeAndTargetValue(Long eventNo, PointEventTargetType targetType, String targetValue);
}
