package com.swimshop.swim_mall.size.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.size.entity.SizeSynonymEntity;

@Repository
public interface SizeSynonymRepository extends JpaRepository<SizeSynonymEntity, Long> {
    Optional<SizeSynonymEntity> findBySynonymIgnoreCase(String synonym);
    List<SizeSynonymEntity> findBySize_Code(String code);
}
