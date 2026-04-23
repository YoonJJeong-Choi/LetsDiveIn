package com.swimshop.swim_mall.sale.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.sale.entity.SalePolicyHistoryEntity;

@Repository
public interface SalePolicyHistoryRepository extends JpaRepository<SalePolicyHistoryEntity, Long> {
    Optional<SalePolicyHistoryEntity> findTopBySalePolicy_IdOrderByChangedAtDesc(Long salePolicyId);
}

