package com.swimshop.swim_mall.customer.reopository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerGradeEntity;

@Repository
public interface CustomerGradeRepository extends JpaRepository<CustomerGradeEntity, Long> {
    
    /**
     * 활성화된 등급 목록 조회 (등급 순서 오름차순)
     */
    List<CustomerGradeEntity> findByIsActiveTrueOrderByGradeLevelAsc();
    
    /**
     * 등급명(Enum)으로 조회
     */
    Optional<CustomerGradeEntity> findByGradeName(com.swimshop.swim_mall.common.enums.CustomerGradeEnum gradeName);
    
    /**
     * 기본 등급 조회 (가장 낮은 등급 레벨)
     */
    Optional<CustomerGradeEntity> findFirstByIsActiveTrueOrderByGradeLevelAsc();
}
