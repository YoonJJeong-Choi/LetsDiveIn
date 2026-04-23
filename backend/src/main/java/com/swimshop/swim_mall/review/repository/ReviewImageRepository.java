package com.swimshop.swim_mall.review.repository;

import com.swimshop.swim_mall.review.entity.ReviewEntity;
import com.swimshop.swim_mall.review.entity.ReviewImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImageEntity, Long> {
    List<ReviewImageEntity> findByReviewOrderByIdAsc(ReviewEntity review);
}
