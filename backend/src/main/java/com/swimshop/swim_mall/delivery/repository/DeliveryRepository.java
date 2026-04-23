package com.swimshop.swim_mall.delivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.delivery.entity.DeliveryEntity;

@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {
    
    /**
     * 주문 아이템 번호로 배송 조회
     */
    @Query("SELECT d FROM DeliveryEntity d WHERE d.orderItem.orderItemNo = :orderItemNo")
    Optional<DeliveryEntity> findByOrderItemNo(@Param("orderItemNo") Long orderItemNo);
    
    /**
     * 주문 번호로 배송 목록 조회
     */
    @Query("SELECT d FROM DeliveryEntity d WHERE d.orderItem.order.orderNo = :orderNo")
    List<DeliveryEntity> findByOrderNo(@Param("orderNo") Long orderNo);
    
    /**
     * 관리자용: 모든 배송 조회 (JOIN FETCH 사용)
     */
    @Query("SELECT DISTINCT d FROM DeliveryEntity d " +
           "JOIN FETCH d.orderItem oi " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH opt.partner p " +
           "LEFT JOIN FETCH oi.order o")
    List<DeliveryEntity> findAllWithOrderItem();
    
    /**
     * 파트너 ID로 배송 목록 조회 (파트너의 상품이 포함된 배송만)
     * Option이 있으면 Option의 Partner, 없으면 Product의 Partner 확인
     * JOIN FETCH를 사용하여 연관 엔티티를 한 번에 로딩
     */
    @Query("SELECT DISTINCT d FROM DeliveryEntity d " +
           "JOIN FETCH d.orderItem oi " +
           "LEFT JOIN FETCH oi.option opt " +
           "LEFT JOIN FETCH opt.partner optPartner " +
           "LEFT JOIN FETCH oi.product prod " +
           "LEFT JOIN FETCH prod.partner prodPartner " +
           "LEFT JOIN FETCH oi.order o " +
           "WHERE (opt IS NOT NULL AND optPartner.partnerId = :partnerId) " +
           "   OR (opt IS NULL AND prodPartner.partnerId = :partnerId)")
    List<DeliveryEntity> findByPartnerId(@Param("partnerId") Long partnerId);
}
