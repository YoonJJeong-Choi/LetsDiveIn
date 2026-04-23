package com.swimshop.swim_mall.color.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.color.entity.ColorSynonymEntity;

@Repository
public interface ColorSynonymRepository extends JpaRepository<ColorSynonymEntity, Long> {
    Optional<ColorSynonymEntity> findBySynonymIgnoreCase(String synonym);
    List<ColorSynonymEntity> findByColor_Code(String code);
}
