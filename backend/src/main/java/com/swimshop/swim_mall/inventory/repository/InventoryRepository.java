package com.swimshop.swim_mall.inventory.repository;

import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.inventory.entity.InventoryEntity;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    /**
     * 옵션별 재고 조회
     * @param optionNo 옵션 번호
     * @return 재고 엔티티 (없으면 Optional.empty())
     */
    Optional<InventoryEntity> findByOption_OptionNo(Long optionNo);

    /**
     * 상품별 재고 조회 (옵션이 없는 상품의 경우)
     * @param productNo 상품 번호
     * @return 재고 엔티티 (없으면 Optional.empty())
     */
    Optional<InventoryEntity> findByProduct_ProductNo(Long productNo);

    /**
     * 파트너별 재고 목록 조회
     * 옵션이 있는 상품: 옵션 정보 포함
     * 옵션이 없는 상품: 상품 정보 포함
     * @param partnerId 파트너 ID
     * @return 재고 목록
     */
    @Query("SELECT DISTINCT i FROM InventoryEntity i " +
           "LEFT JOIN FETCH i.option o " +
           "LEFT JOIN FETCH i.product p " +
           "LEFT JOIN FETCH o.product op " +
           "WHERE (o IS NOT NULL AND o.partner.partnerId = :partnerId) OR " +
           "      (p IS NOT NULL AND p.partner.partnerId = :partnerId) " +
           "ORDER BY i.inventoryNo")
    List<InventoryEntity> findByPartnerId(@Param("partnerId") Long partnerId);

    /**
     * 옵션으로 재고 존재 여부 확인
     * @param option 옵션 엔티티
     * @return 재고가 존재하면 true
     */
    boolean existsByOption(OptionEntity option);

    /**
     * 파트너 소유·판매중(ACTIVE) 옵션/단일상품 재고 중, 수량이 임계 이하인 행 개수(품절·품절 임박)
     */
    @Query("SELECT COUNT(i) FROM InventoryEntity i "
            + "LEFT JOIN i.option opt "
            + "LEFT JOIN i.product prod "
            + "WHERE i.inventoryStock <= :threshold "
            + "AND ((opt IS NOT NULL AND opt.partner.partnerId = :partnerId AND opt.optionStatus = :activeStatus) "
            + "OR (prod IS NOT NULL AND prod.partner.partnerId = :partnerId AND prod.productActiveStatus = :activeStatus))")
    long countLowStockLinesForPartner(
            @Param("partnerId") Long partnerId,
            @Param("threshold") int threshold,
            @Param("activeStatus") ActiveStatus activeStatus);

    /** 파트너 소유·판매중(ACTIVE) 재고 중 수량 0 이하(품절) */
    @Query("SELECT COUNT(i) FROM InventoryEntity i "
            + "LEFT JOIN i.option opt "
            + "LEFT JOIN i.product prod "
            + "WHERE i.inventoryStock <= 0 "
            + "AND ((opt IS NOT NULL AND opt.partner.partnerId = :partnerId AND opt.optionStatus = :activeStatus) "
            + "OR (prod IS NOT NULL AND prod.partner.partnerId = :partnerId AND prod.productActiveStatus = :activeStatus))")
    long countOutOfStockLinesForPartner(
            @Param("partnerId") Long partnerId,
            @Param("activeStatus") ActiveStatus activeStatus);
}
