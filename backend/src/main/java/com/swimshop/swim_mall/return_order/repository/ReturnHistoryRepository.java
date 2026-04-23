package com.swimshop.swim_mall.return_order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.return_order.entity.ReturnHistoryEntity;

@Repository
public interface ReturnHistoryRepository extends JpaRepository<ReturnHistoryEntity, Long> {
    
    /**
     * 반품별 변경 이력 조회 (최신순)
     */
    @Query("SELECT h FROM ReturnHistoryEntity h " +
           "LEFT JOIN FETCH h.admin a " +
           "LEFT JOIN FETCH h.partner p " +
           "WHERE h.returnEntity.returnNo = :returnNo " +
           "ORDER BY h.changedAt DESC")
    List<ReturnHistoryEntity> findByReturnNoOrderByChangedAtDesc(
        @Param("returnNo") Long returnNo
    );
}
