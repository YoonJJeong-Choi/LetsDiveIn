package com.swimshop.swim_mall.ai.reviewanalysis.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.ai.reviewanalysis.entity.ReviewAnalysisLogEntity;

@Repository
public interface ReviewAnalysisLogRepository extends JpaRepository<ReviewAnalysisLogEntity, Long> {

    long countByPartnerIdAndCreatedAtGreaterThanEqual(String partnerId, LocalDateTime createdAt);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @Query("""
            SELECT l
            FROM ReviewAnalysisLogEntity l
            WHERE l.success = false
              AND l.errorMessage IS NOT NULL
            ORDER BY l.createdAt DESC
            """)
    List<ReviewAnalysisLogEntity> findRecentFailures();
}
