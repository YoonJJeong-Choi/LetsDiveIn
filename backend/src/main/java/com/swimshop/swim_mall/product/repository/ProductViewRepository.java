package com.swimshop.swim_mall.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.product.entity.ProductViewEntity;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductViewEntity, Long> {
}
