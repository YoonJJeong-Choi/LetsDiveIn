package com.swimshop.swim_mall.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.OrderStatus;

import java.util.Collection;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    @Query("SELECT oi FROM OrderItemEntity oi " +
           "LEFT JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.product p LEFT JOIN FETCH p.partner " +
           "LEFT JOIN FETCH oi.option opt LEFT JOIN FETCH opt.partner " +
           "WHERE oi.orderItemNo = :orderItemNo")
    Optional<OrderItemEntity> findByIdWithPartners(@Param("orderItemNo") Long orderItemNo);

    /**
     * 발주 확인 대기: 취소 아님, confirmedAt 없음, 주문이 결제 완료 또는 진행 중
     */
    @Query("SELECT COUNT(oi) FROM OrderItemEntity oi WHERE oi.isCancelled = false AND oi.confirmedAt IS NULL AND oi.order.orderStatus IN :statuses")
    long countPendingConfirmation(@Param("statuses") List<OrderStatus> statuses);

    // 주문별 아이템 목록 조회
    List<OrderItemEntity> findByOrder(OrderEntity order);

    /**
     * 상품별 판매 수량 집계 (인기순 정렬용)
     * - 취소되지 않은 주문상품만 집계
     * - 결제 완료/진행중 주문만 반영
     * 결과: [productNo, soldQuantity]
     */
    @Query("SELECT oi.product.productNo, COALESCE(SUM(oi.itemQuantity), 0) " +
           "FROM OrderItemEntity oi " +
           "WHERE oi.isCancelled = false " +
           "AND oi.order.orderStatus IN :statuses " +
           "GROUP BY oi.product.productNo")
    List<Object[]> findSoldQuantityByProduct(@Param("statuses") List<OrderStatus> statuses);
    
    /**
     * 차트/Top N 집계를 위해 옵션/상품/파트너를 함께 로드
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH opt.partner p " +
           "LEFT JOIN FETCH oi.product prod " +
           "LEFT JOIN FETCH prod.partner pp " +
           "WHERE oi.orderItemNo IN :ids")
    List<OrderItemEntity> findAllWithOptionAndProductPartnerByIdIn(@Param("ids") List<Long> ids);
    
    /**
     * 고객의 구매확정된 주문상품 총 금액 합계
     * - 취소되지 않음
     * - 구매확정됨 (completedAt != null)
     */
    @Query("SELECT COALESCE(SUM(oi.itemTotalPrice), 0) FROM OrderItemEntity oi " +
           "WHERE oi.isCancelled = false " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.order.customer.customerId = :customerId")
    Long sumCompletedItemAmountByCustomerId(@Param("customerId") Long customerId);
    
    /**
     * 고객의 구매확정된 주문상품 개수
     * - 취소되지 않음
     * - 구매확정됨 (completedAt != null)
     */
    @Query("SELECT COUNT(oi) FROM OrderItemEntity oi " +
           "WHERE oi.isCancelled = false " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.order.customer.customerId = :customerId")
    Long countCompletedItemsByCustomerId(@Param("customerId") Long customerId);
    
    /**
     * 주문 아이템 조회 (Delivery와 함께 로드)
     * 반품 신청 시 배송 상태 확인을 위해 사용
     */
    @Query("SELECT oi FROM OrderItemEntity oi " +
           "LEFT JOIN FETCH oi.delivery d " +
           "LEFT JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH o.customer c " +
           "WHERE oi.orderItemNo = :orderItemNo")
    java.util.Optional<OrderItemEntity> findByIdWithDelivery(@Param("orderItemNo") Long orderItemNo);
    
    /**
     * 자동 구매 확정 대상 주문 상품 조회
     * - 배송 완료(DELIVERED) 상태
     * - 배송 완료일이 7일 이상 경과
     * - 아직 구매 확정되지 않음 (completedAt이 null)
     * - 반품 진행 중이거나 환불 완료되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE d.deliveryStatus = :deliveryStatus " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND d.deliveryEndDate <= :sevenDaysAgo " +
           "AND oi.completedAt IS NULL " +
           "AND (r IS NULL OR r.returnStatus NOT IN ('REQUESTED', 'APPROVED', 'PICKUP_COMPLETED', 'REFUNDED'))")
    List<OrderItemEntity> findAutoCompleteTargets(
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo
    );
    
    /**
     * 정산 대상 주문 아이템 조회 (파트너용) - 기간 필터 없음
     * - 배송 완료(DELIVERED) 상태
     * - 구매 확정됨 (completedAt != null)
     * - 취소되지 않음 (isCancelled = false)
     * - 반품/환불 완료되지 않음 (ReturnEntity가 없거나 ReturnStatus != REFUNDED)
     * - 본인 파트너의 상품 (옵션이 있으면 Option.partner.partnerId, 없으면 Product.partner.partnerId)
     * - 이미 정산에 포함되지 않음 (SettlementOrderItemEntity에 없음)
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId = :partnerId) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId = :partnerId)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findSettlementReadyItems(
        @Param("partnerId") Long partnerId,
        @Param("deliveryStatus") DeliveryStatus deliveryStatus
    );
    
    /**
     * 정산 대상 주문 아이템 조회 (파트너용) - 시작일 필터만
     * - 이미 정산에 포함되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId = :partnerId) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId = :partnerId)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND d.deliveryEndDate >= :startDate " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findSettlementReadyItemsWithStartDate(
        @Param("partnerId") Long partnerId,
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("startDate") LocalDateTime startDate
    );
    
    /**
     * 정산 대상 주문 아이템 조회 (파트너용) - 종료일 필터만
     * - 이미 정산에 포함되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId = :partnerId) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId = :partnerId)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND d.deliveryEndDate <= :endDate " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findSettlementReadyItemsWithEndDate(
        @Param("partnerId") Long partnerId,
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 정산 대상 주문 아이템 조회 (파트너용) - 시작일 + 종료일 필터
     * - 이미 정산에 포함되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId = :partnerId) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId = :partnerId)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND d.deliveryEndDate >= :startDate " +
           "AND d.deliveryEndDate <= :endDate " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findSettlementReadyItemsWithDateRange(
        @Param("partnerId") Long partnerId,
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 전체 파트너의 정산 대상 주문 아이템 조회 - 기간 필터 없음
     * - 이미 정산에 포함되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId IS NOT NULL) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId IS NOT NULL)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findAllPartnersSettlementReadyItems(
        @Param("deliveryStatus") DeliveryStatus deliveryStatus
    );
    
    /**
     * 전체 파트너의 정산 대상 주문 아이템 조회 - 시작일 필터만
     * - 이미 정산에 포함되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId IS NOT NULL) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId IS NOT NULL)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND d.deliveryEndDate >= :startDate " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findAllPartnersSettlementReadyItemsWithStartDate(
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("startDate") LocalDateTime startDate
    );
    
    /**
     * 전체 파트너의 정산 대상 주문 아이템 조회 - 종료일 필터만
     * - 이미 정산에 포함되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId IS NOT NULL) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId IS NOT NULL)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND d.deliveryEndDate <= :endDate " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findAllPartnersSettlementReadyItemsWithEndDate(
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 전체 파트너의 정산 대상 주문 아이템 조회 - 시작일 + 종료일 필터
     * - 이미 정산에 포함되지 않음
     */
    @Query("SELECT DISTINCT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.delivery d " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH oi.returnEntity r " +
           "WHERE ((opt IS NOT NULL AND opt.partner.partnerId IS NOT NULL) " +
           "OR (opt IS NULL AND oi.product.partner.partnerId IS NOT NULL)) " +
           "AND d.deliveryStatus = :deliveryStatus " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.isCancelled = false " +
           "AND (r IS NULL OR r.returnStatus != 'REFUNDED') " +
           "AND d.deliveryEndDate IS NOT NULL " +
           "AND d.deliveryEndDate >= :startDate " +
           "AND d.deliveryEndDate <= :endDate " +
           "AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId WHERE soi.orderItem.orderItemNo = oi.orderItemNo AND s.settlementStatus != 'CANCELLED') " +
           "ORDER BY d.deliveryEndDate DESC, oi.orderItemNo DESC")
    List<OrderItemEntity> findAllPartnersSettlementReadyItemsWithDateRange(
        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 파트너 라인이 포함된 주문 중, 기간 내 결제 완료(paidAt) 건수(주문 번호 기준 중복 제거)
     */
    @Query("SELECT COUNT(DISTINCT o.orderNo) FROM OrderItemEntity oi "
            + "JOIN oi.order o JOIN o.payment p "
            + "WHERE oi.isCancelled = false "
            + "AND p.paymentCancelYn = false AND p.paidAt IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId))")
    long countDistinctPartnerOrdersPaidBetween(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses);

    /**
     * 위와 동일 조건의 본인 주문상품 라인 매출 합계(원)
     */
    @Query("SELECT COALESCE(SUM(oi.itemTotalPrice), 0) FROM OrderItemEntity oi "
            + "JOIN oi.order o JOIN o.payment p "
            + "WHERE oi.isCancelled = false "
            + "AND p.paymentCancelYn = false AND p.paidAt IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId))")
    long sumPartnerLineRevenuePaidBetween(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses);

    /**
     * 발송 전 주문: 본인 라인이 발주 미확인이거나, 배송이 배송준비(READY)인 주문(주문 번호 기준 중복 제거)
     */
    @Query("SELECT COUNT(DISTINCT o.orderNo) FROM OrderItemEntity oi JOIN oi.order o "
            + "WHERE oi.isCancelled = false "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId)) "
            + "AND (oi.confirmedAt IS NULL OR (oi.delivery IS NOT NULL AND oi.delivery.deliveryStatus = :ready))")
    long countDistinctPartnerOrdersPreShipment(
            @Param("partnerId") Long partnerId,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses,
            @Param("ready") DeliveryStatus ready);

    /**
     * 본인 라인이 배송중(SHIPPED)인 주문(주문 번호 기준 중복 제거)
     */
    @Query("SELECT COUNT(DISTINCT o.orderNo) FROM OrderItemEntity oi JOIN oi.order o "
            + "WHERE oi.isCancelled = false "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId)) "
            + "AND oi.delivery IS NOT NULL AND oi.delivery.deliveryStatus = :shipped")
    long countDistinctPartnerOrdersInDelivery(
            @Param("partnerId") Long partnerId,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses,
            @Param("shipped") DeliveryStatus shipped);

    @Query("SELECT COUNT(DISTINCT o.orderNo) FROM OrderItemEntity oi "
            + "JOIN oi.order o JOIN o.payment p "
            + "WHERE oi.isCancelled = false "
            + "AND p.paymentCancelYn = false AND p.paidAt IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND oi.product.productNo = :productNo "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId))")
    long countDistinctPartnerOrdersPaidBetweenForProduct(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses,
            @Param("productNo") Long productNo);

    @Query("SELECT COALESCE(SUM(oi.itemTotalPrice), 0) FROM OrderItemEntity oi "
            + "JOIN oi.order o JOIN o.payment p "
            + "WHERE oi.isCancelled = false "
            + "AND p.paymentCancelYn = false AND p.paidAt IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND oi.product.productNo = :productNo "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId))")
    long sumPartnerLineRevenuePaidBetweenForProduct(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses,
            @Param("productNo") Long productNo);

    @Query("SELECT oi.product.productNo, oi.product.productName, oi.product.sku, "
            + "COALESCE(SUM(oi.itemQuantity), 0L), COALESCE(SUM(oi.itemTotalPrice), 0L) FROM OrderItemEntity oi "
            + "JOIN oi.order o JOIN o.payment p "
            + "WHERE oi.isCancelled = false "
            + "AND p.paymentCancelYn = false AND p.paidAt IS NOT NULL "
            + "AND p.paidAt >= :start AND p.paidAt < :end "
            + "AND o.orderStatus IN :orderStatuses "
            + "AND ((oi.option IS NOT NULL AND oi.option.partner.partnerId = :partnerId) "
            + "OR (oi.option IS NULL AND oi.product.partner.partnerId = :partnerId)) "
            + "GROUP BY oi.product.productNo, oi.product.productName, oi.product.sku "
            + "ORDER BY SUM(oi.itemTotalPrice) DESC")
    List<Object[]> aggregatePartnerLineSalesByProductBetween(
            @Param("partnerId") Long partnerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orderStatuses") Collection<OrderStatus> orderStatuses);

}
