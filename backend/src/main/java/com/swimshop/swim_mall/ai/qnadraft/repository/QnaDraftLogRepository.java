package com.swimshop.swim_mall.ai.qnadraft.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.ai.qnadraft.entity.QnaDraftLogEntity;

@Repository
public interface QnaDraftLogRepository extends JpaRepository<QnaDraftLogEntity, Long> {

    long countByAdminIdAndCreatedAtGreaterThanEqual(Long adminId, LocalDateTime createdAt);

    long countByPartnerIdAndCreatedAtGreaterThanEqual(Long partnerId, LocalDateTime createdAt);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @Query("""
            SELECT l
            FROM QnaDraftLogEntity l
            WHERE l.success = false
              AND l.errorMessage IS NOT NULL
            ORDER BY l.createdAt DESC
            """)
    List<QnaDraftLogEntity> findRecentFailures();
}
