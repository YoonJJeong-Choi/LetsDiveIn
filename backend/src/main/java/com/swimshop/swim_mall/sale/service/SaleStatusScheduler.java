package com.swimshop.swim_mall.sale.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.enums.SaleStatus;
import com.swimshop.swim_mall.sale.entity.SalePolicyEntity;
import com.swimshop.swim_mall.sale.repository.SalePolicyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaleStatusScheduler {

    private final SalePolicyRepository salePolicyRepository;

    /**
     * 만료 시각이 지난 ACTIVE 세일을 자동으로 EXPIRED로 전환합니다.
     */
    @Scheduled(cron = "0 5 * * * ?") // 매시 5분
    @Transactional
    public void autoExpireSales() {
        LocalDateTime now = LocalDateTime.now();
        List<SalePolicyEntity> targets = salePolicyRepository.findByStatusAndEndAtBefore(SaleStatus.ACTIVE, now);
        if (targets.isEmpty()) return;

        for (SalePolicyEntity policy : targets) {
            policy.changeStatus(SaleStatus.EXPIRED);
        }

        log.info("세일 자동 종료(EXPIRED) 처리 완료: {}건", targets.size());
    }
}

