package com.swimshop.swim_mall.point.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.PointType;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.point.entity.PointHistoryEntity;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistoryEntity, Long> {
    
    /**
     * 고객의 포인트 내역 조회 (최신순)
     */
    List<PointHistoryEntity> findByCustomerOrderByCreatedAtDesc(CustomerEntity customer);
    
    /**
     * 고객의 포인트 내역 조회 (페이징 지원)
     */
    List<PointHistoryEntity> findByCustomer_CustomerIdOrderByCreatedAtDesc(Long customerId);
    
    /**
     * 고객과 주문번호로 포인트 내역 조회
     */
    List<PointHistoryEntity> findByCustomerAndOrderNo(CustomerEntity customer, Long orderNo);
    
    /**
     * 고객, 주문번호, 포인트 타입으로 포인트 내역 조회
     */
    List<PointHistoryEntity> findByCustomerAndOrderNoAndPointType(
        CustomerEntity customer, 
        Long orderNo, 
        PointType pointType
    );

    /**
     * 특정 주문상품 기본 적립이 이미 존재하는지 확인 (중복 적립 방지)
     */
    boolean existsByCustomerAndOrderItemNoAndPointType(
        CustomerEntity customer,
        Long orderItemNo,
        PointType pointType
    );
}
