package com.swimshop.swim_mall.ai.common.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.ai.common.entity.AiBlockedLogEntity;

@Repository
public interface AiBlockedLogRepository extends JpaRepository<AiBlockedLogEntity, Long> {

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @Query("""
            SELECT l.role, COUNT(l)
            FROM AiBlockedLogEntity l
            WHERE l.createdAt >= :createdAt
            GROUP BY l.role
            ORDER BY COUNT(l) DESC
            """)
    List<Object[]> countByRoleSince(LocalDateTime createdAt);

    @Query("""
            SELECT l.role, l.accountId, COUNT(l)
            FROM AiBlockedLogEntity l
            WHERE l.createdAt >= :createdAt
            GROUP BY l.role, l.accountId
            ORDER BY COUNT(l) DESC
            """)
    List<Object[]> countByRoleAndAccountSince(LocalDateTime createdAt);
}
