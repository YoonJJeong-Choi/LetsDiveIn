package com.swimshop.swim_mall.product.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.product.entity.ProductViewLogEntity;

@Repository
public interface ProductViewLogRepository extends JpaRepository<ProductViewLogEntity, Long> {
    boolean existsByProductNoAndCustomerIdAndViewedAtAfter(Long productNo, Long customerId, LocalDateTime viewedAt);
    boolean existsByProductNoAndGuestIdAndViewedAtAfter(Long productNo, String guestId, LocalDateTime viewedAt);
    long countByProductNoAndViewedAtAfter(Long productNo, LocalDateTime viewedAt);
}
