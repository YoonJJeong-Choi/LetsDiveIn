package com.swimshop.swim_mall.ai.returnrisk.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.ai.returnrisk.entity.ReturnRiskLogEntity;

@Repository
public interface ReturnRiskLogRepository extends JpaRepository<ReturnRiskLogEntity, Long> {

    List<ReturnRiskLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ReturnRiskLogEntity> findBySuccessOrderByCreatedAtDesc(Boolean success, Pageable pageable);
}
