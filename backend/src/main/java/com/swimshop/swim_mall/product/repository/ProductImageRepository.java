package com.swimshop.swim_mall.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.entity.ProductImageEntity;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {
	List<ProductImageEntity> findByProductOrderBySortOrderAscIdAsc(ProductEntity product);
	void deleteByProduct(ProductEntity product);
}

