package com.swimshop.swim_mall.color.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.color.entity.ColorEntity;

@Repository
public interface ColorRepository extends JpaRepository<ColorEntity, String> {
    List<ColorEntity> findByIsActiveTrueOrderBySortOrderAscLabelAsc();
    Optional<ColorEntity> findByLabelIgnoreCase(String label);
}
