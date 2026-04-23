package com.swimshop.swim_mall.ai.reviewanalysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.ai.reviewanalysis.entity.ReviewAnalysisLogEntity;

@Repository
public interface ReviewAnalysisLogRepository extends JpaRepository<ReviewAnalysisLogEntity, Long> {
}
