package com.swimshop.swim_mall.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.common.enums.ProductType;
import com.swimshop.swim_mall.common.enums.ProductSubType;
import com.swimshop.swim_mall.product.entity.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 활성 상태인 상품 목록 조회
     */
    List<ProductEntity> findByProductActiveStatus(ActiveStatus status);
    
    /**
     * 여러 상태의 상품 목록 조회
     */
    List<ProductEntity> findByProductActiveStatusIn(List<ActiveStatus> statuses);
    
    /**
     * 카테고리 필터링으로 상품 조회
     */
    @Query("SELECT p FROM ProductEntity p " +
           "WHERE p.productActiveStatus = :status " +
           "AND (:productTypeEnum IS NULL OR p.productType = :productTypeEnum) " +
           "AND (:productSubTypeEnum IS NULL OR p.productSubType = :productSubTypeEnum) " +
           "ORDER BY p.productCreatedAt DESC")
    List<ProductEntity> findByCategory(
            @Param("status") ActiveStatus status,
            @Param("productTypeEnum") ProductType productTypeEnum,
            @Param("productSubTypeEnum") ProductSubType productSubTypeEnum
    );

    /**
     * 파트너 ID로 상품 목록 조회
     */
    List<ProductEntity> findByPartner_PartnerId(Long partnerId);
}
