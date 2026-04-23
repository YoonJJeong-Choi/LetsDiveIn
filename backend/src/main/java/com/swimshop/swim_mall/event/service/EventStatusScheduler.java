package com.swimshop.swim_mall.event.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final EventRepository eventRepository;

    /**
     * 고객 이벤트 종료시점이 지난 ACTIVE 이벤트를 ENDED로 자동 전환합니다.
     * - 수동 상태 변경은 계속 가능
     * - 자동 전환은 운영 편의성 보완용
     */
    @Scheduled(cron = "0 0 * * * ?") // 매시 정각
    @Transactional
    public void autoEndExpiredActiveEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<EventEntity> targets = eventRepository.findByEventStatusAndCustomerEventEndAtBefore(
                EventStatus.ACTIVE,
                now
        );

        if (targets.isEmpty()) {
            return;
        }

        for (EventEntity event : targets) {
            event.changeStatus(EventStatus.ENDED);
        }

        log.info("이벤트 자동 종료 처리 완료: {}건", targets.size());
    }
}
