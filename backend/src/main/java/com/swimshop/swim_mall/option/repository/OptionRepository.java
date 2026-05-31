package com.swimshop.swim_mall.option.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.option.entity.OptionEntity;

@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, Long> {

    long countByOptionStatus(ActiveStatus optionStatus);
    
    /**
     * 파트너 ID로 옵션 목록 조회 (상품 정보 포함, 모든 상태)
     * 파트너가 자신의 상품을 관리할 때 사용 (INACTIVE 상태도 포함)
     */
    @Query("SELECT o FROM OptionEntity o " +
           "JOIN FETCH o.product p " +
           "JOIN FETCH o.partner " +
           "WHERE o.partner.partnerId = :partnerId " +
           "ORDER BY p.productNo, o.optionNo")
    List<OptionEntity> findByPartnerIdWithProduct(@Param("partnerId") Long partnerId);
    
    /**
     * 파트너 ID로 옵션 목록 조회 (간단 버전, ACTIVE 상태만)
     */
    List<OptionEntity> findByPartner_PartnerIdAndOptionStatus(Long partnerId, com.swimshop.swim_mall.common.enums.ActiveStatus status);

    long countByPartner_PartnerIdAndOptionStatus(Long partnerId, ActiveStatus optionStatus);

    /**
     * 상품 번호로 옵션 목록 조회 (ACTIVE 상태만) - 고객용
     */
    @Query("SELECT o FROM OptionEntity o " +
           "WHERE o.product.productNo = :productNo " +
           "AND o.optionStatus = com.swimshop.swim_mall.common.enums.ActiveStatus.ACTIVE")
    List<OptionEntity> findByProduct_ProductNo(@Param("productNo") Long productNo);
    
    /**
     * 상품 번호로 옵션 목록 조회 (모든 상태) - 관리자용
     */
    @Query("SELECT o FROM OptionEntity o " +
           "WHERE o.product.productNo = :productNo " +
           "ORDER BY o.optionNo")
    List<OptionEntity> findByProduct_ProductNoAllStatus(@Param("productNo") Long productNo);
}
