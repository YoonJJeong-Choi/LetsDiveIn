package com.swimshop.swim_mall.size.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.size.entity.SizeEntity;

@Repository
public interface SizeRepository extends JpaRepository<SizeEntity, String> {
    List<SizeEntity> findByIsActiveTrueOrderBySortOrderAscLabelAsc();
    Optional<SizeEntity> findByLabelIgnoreCase(String label);
}
