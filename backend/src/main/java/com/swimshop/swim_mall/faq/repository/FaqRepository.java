package com.swimshop.swim_mall.faq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.faq.entity.FaqEntity;

@Repository
public interface FaqRepository extends JpaRepository<FaqEntity, Long> {
    
    // 카테고리별 FAQ 조회
    List<FaqEntity> findByFaqCategoryOrderByFaqNoDesc(String category);
    
    // 전체 FAQ 조회 (최신순)
    List<FaqEntity> findAllByOrderByFaqNoDesc();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FaqEntity f SET f.faqCategory = :newCategory WHERE f.faqCategory = :oldCategory")
    int replaceFaqCategory(@Param("oldCategory") String oldCategory, @Param("newCategory") String newCategory);
}
