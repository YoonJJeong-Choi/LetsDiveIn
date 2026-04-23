package com.swimshop.swim_mall.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.payment.entity.WebhookLogEntity;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLogEntity, Long> {
    Optional<WebhookLogEntity> findByEventId(String eventId);
}

