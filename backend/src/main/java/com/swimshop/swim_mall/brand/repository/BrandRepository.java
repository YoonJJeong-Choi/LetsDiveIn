package com.swimshop.swim_mall.brand.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.brand.entity.BrandEntity;

@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, String> {
    List<BrandEntity> findByIsActiveTrueOrderBySortOrderAscDisplayNameAsc();
    Optional<BrandEntity> findByCanonicalNameIgnoreCase(String name);
    Optional<BrandEntity> findByDisplayNameIgnoreCase(String name);
}
