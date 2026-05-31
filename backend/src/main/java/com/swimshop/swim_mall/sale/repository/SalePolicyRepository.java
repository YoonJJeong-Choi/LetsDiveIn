package com.swimshop.swim_mall.sale.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.SaleStatus;
import com.swimshop.swim_mall.sale.entity.SalePolicyEntity;

@Repository
public interface SalePolicyRepository extends JpaRepository<SalePolicyEntity, Long> {
    List<SalePolicyEntity> findByCreatedByPartnerIdOrderByCreatedAtDesc(Long partnerId);
    List<SalePolicyEntity> findByCreatedByPartnerIdAndCampaignIdOrderByCreatedAtDesc(Long partnerId, String campaignId);
    List<SalePolicyEntity> findByEvent_EventNoOrderByCreatedAtDesc(Long eventNo);
    List<SalePolicyEntity> findByStatusAndEndAtBefore(SaleStatus status, LocalDateTime endAt);
    List<SalePolicyEntity> findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            SaleStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    @Query("""
            select count(s) from SalePolicyEntity s
            where s.createdByPartnerId = :partnerId
              and s.event.eventNo = :eventNo
              and s.scope = :scope
              and s.status in :statuses
              and (
                    (s.scope = com.swimshop.swim_mall.common.enums.SaleScope.PRODUCT and s.targetProductNo = :targetProductNo)
                 or (s.scope = com.swimshop.swim_mall.common.enums.SaleScope.OPTION and s.targetOptionNo = :targetOptionNo)
              )
            """)
    long countEventLinkedByPartnerAndTarget(
            @Param("partnerId") Long partnerId,
            @Param("eventNo") Long eventNo,
            @Param("scope") com.swimshop.swim_mall.common.enums.SaleScope scope,
            @Param("targetProductNo") Long targetProductNo,
            @Param("targetOptionNo") Long targetOptionNo,
            @Param("statuses") List<SaleStatus> statuses
    );

    /**
     * 파트너가 특정 이벤트(SALE 연동 세일)에 대해 실제로 세일 정책(sale_policy)을 등록했는지 판단용.
     * - CANCELLED/REJECTED는 제외
     * - 나머지(ACTIVE, PENDING_APPROVAL, INACTIVE, EXPIRED 등)는 "등록됨"으로 취급
     */
    @Query("""
            select distinct s.event.eventNo
            from SalePolicyEntity s
            where s.createdByPartnerId = :partnerId
              and s.event.eventNo in :eventNos
              and s.status not in :excludedStatuses
            """)
    List<Long> findRegisteredSaleEventNosForPartnerAt(
            @Param("partnerId") Long partnerId,
            @Param("eventNos") List<Long> eventNos,
            @Param("excludedStatuses") List<SaleStatus> excludedStatuses
    );

    @Query("""
            select s from SalePolicyEntity s
            where s.status = :active
              and s.startAt <= :now
              and s.endAt >= :now
              and (
                    (
                      s.scope = com.swimshop.swim_mall.common.enums.SaleScope.OPTION
                      and :optionNo is null
                      and exists (
                        select 1 from com.swimshop.swim_mall.option.entity.OptionEntity o
                        where o.optionNo = s.targetOptionNo
                          and o.product.productNo = :productNo
                      )
                    )
                 or (
                      s.scope = com.swimshop.swim_mall.common.enums.SaleScope.OPTION
                      and :optionNo is not null
                      and s.targetOptionNo = :optionNo
                    )
                 or (
                      s.scope = com.swimshop.swim_mall.common.enums.SaleScope.PRODUCT
                      and s.targetProductNo = :productNo
                    )
              )
            order by 
              case when s.scope = com.swimshop.swim_mall.common.enums.SaleScope.OPTION then 0 else 1 end,
              s.createdAt desc
            """)
    List<SalePolicyEntity> findEligibleFor(
            @Param("productNo") Long productNo,
            @Param("optionNo") Long optionNo,
            @Param("active") SaleStatus active,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select count(s) from SalePolicyEntity s
            where s.scope = :scope
              and (:excludeId is null or s.id <> :excludeId)
              and s.status in :statuses
              and (
                    (s.scope = com.swimshop.swim_mall.common.enums.SaleScope.PRODUCT and s.targetProductNo = :targetProductNo)
                 or (s.scope = com.swimshop.swim_mall.common.enums.SaleScope.OPTION and s.targetOptionNo = :targetOptionNo)
              )
              and s.startAt <= :endAt
              and s.endAt >= :startAt
            """)
    long countOverlappingByTarget(
            @Param("scope") com.swimshop.swim_mall.common.enums.SaleScope scope,
            @Param("targetProductNo") Long targetProductNo,
            @Param("targetOptionNo") Long targetOptionNo,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("statuses") List<SaleStatus> statuses,
            @Param("excludeId") Long excludeId
    );

    @Query("""
            select count(s) from SalePolicyEntity s
            where s.scope = com.swimshop.swim_mall.common.enums.SaleScope.OPTION
              and (:excludeId is null or s.id <> :excludeId)
              and s.status in :statuses
              and s.startAt <= :endAt
              and s.endAt >= :startAt
              and exists (
                    select 1 from com.swimshop.swim_mall.option.entity.OptionEntity o
                    where o.optionNo = s.targetOptionNo
                      and o.product.productNo = :productNo
              )
            """)
    long countOverlappingOptionSalesForProduct(
            @Param("productNo") Long productNo,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("statuses") List<SaleStatus> statuses,
            @Param("excludeId") Long excludeId
    );

    @Query("""
            select count(s) from SalePolicyEntity s
            where s.scope = com.swimshop.swim_mall.common.enums.SaleScope.PRODUCT
              and (:excludeId is null or s.id <> :excludeId)
              and s.status in :statuses
              and s.startAt <= :endAt
              and s.endAt >= :startAt
              and s.targetProductNo = :productNo
            """)
    long countOverlappingProductSalesForProduct(
            @Param("productNo") Long productNo,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("statuses") List<SaleStatus> statuses,
            @Param("excludeId") Long excludeId
    );
}

