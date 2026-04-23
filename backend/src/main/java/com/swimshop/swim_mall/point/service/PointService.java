package com.swimshop.swim_mall.point.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.enums.PointType;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.point.entity.PointHistoryEntity;
import com.swimshop.swim_mall.point.repository.PointHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final CustomerRepository customerRepository;

    /**
     * 포인트 적립 (구매 확정 시)
     * @param customer 고객
     * @param orderItem 주문 상품
     * @param pointAmount 적립할 포인트 금액
     */
    @Transactional
    public void accumulatePoint(CustomerEntity customer, OrderItemEntity orderItem, Long pointAmount) {
        if (pointAmount <= 0) {
            return; // 0 이하 포인트는 적립하지 않음
        }

        // 중복 적립 방지: 동일 고객+주문상품에 대해 ACCUMULATE 내역이 이미 있으면 건너뜀
        if (orderItem != null && orderItem.getOrderItemNo() != null) {
            boolean exists = pointHistoryRepository.existsByCustomerAndOrderItemNoAndPointType(
                customer, orderItem.getOrderItemNo(), PointType.ACCUMULATE
            );
            if (exists) {
                return;
            }
        }

        // 1. 고객 포인트 잔액 증가
        customer.addPoint(pointAmount);
        
        // 2. 포인트 내역 기록
        String description = "구매 확정 적립";
        if (customer.getCustomerGrade() != null) {
            description = String.format("구매 확정 적립 (등급: %s, 적립률: %.1f%%)", 
                customer.getCustomerGrade().getGradeName().getLabel(),
                customer.getCustomerGrade().getPointAccumulationRate());
        }
        
        PointHistoryEntity history = PointHistoryEntity.builder()
            .customer(customer)
            .pointType(PointType.ACCUMULATE)
            .pointAmount(pointAmount)
            .pointBalanceAfter(customer.getPointBalance())
            .orderItemNo(orderItem.getOrderItemNo())
            .description(description)
            .expireDate(LocalDate.now().plusYears(1)) // 1년 후 만료
            .createdAt(LocalDateTime.now())
            .build();
        
        pointHistoryRepository.save(history);
        customerRepository.save(customer);
    }

    /**
     * 이벤트 보상 포인트 적립 (MVP)
     * - 기존 구매확정 적립 로직과 동일한 방식으로 포인트를 누적하되,
     *   description만 이벤트 보상으로 고정합니다.
     */
    @Transactional
    public void accumulateEventPoint(CustomerEntity customer, OrderItemEntity orderItem, Long pointAmount, String description) {
        if (customer == null || orderItem == null) return;
        if (pointAmount == null || pointAmount <= 0) return;

        customer.addPoint(pointAmount);

        PointHistoryEntity history = PointHistoryEntity.builder()
                .customer(customer)
                .pointType(PointType.ACCUMULATE)
                .pointAmount(pointAmount)
                .pointBalanceAfter(customer.getPointBalance())
                .orderItemNo(orderItem.getOrderItemNo())
                .description(description != null ? description : "이벤트 보상")
                .expireDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .build();

        pointHistoryRepository.save(history);
        customerRepository.save(customer);
    }

    /**
     * 리뷰 작성 기본 포인트 적립.
     * 리뷰는 order_item_no 유니크 제약으로 1회만 작성 가능하므로 중복 적립이 발생하지 않습니다.
     */
    @Transactional
    public void accumulateReviewPoint(CustomerEntity customer, Long orderItemNo, Long pointAmount) {
        if (customer == null || orderItemNo == null) return;
        if (pointAmount == null || pointAmount <= 0) return;

        customer.addPoint(pointAmount);

        PointHistoryEntity history = PointHistoryEntity.builder()
                .customer(customer)
                .pointType(PointType.ACCUMULATE)
                .pointAmount(pointAmount)
                .pointBalanceAfter(customer.getPointBalance())
                .orderItemNo(orderItemNo)
                .description("리뷰 작성 적립")
                .expireDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .build();

        pointHistoryRepository.save(history);
        customerRepository.save(customer);
    }

    /**
     * 포인트 사용 (주문 결제 시)
     * @param customer 고객
     * @param order 주문
     * @param usePointAmount 사용할 포인트 금액
     */
    @Transactional
    public void usePoint(CustomerEntity customer, OrderEntity order, Long usePointAmount) {
        if (usePointAmount <= 0) {
            return; // 0 이하 포인트는 사용하지 않음
        }

        // 1. 포인트 잔액 확인
        if (customer.getPointBalance() < usePointAmount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        // 2. 고객 포인트 잔액 차감
        customer.deductPoint(usePointAmount);
        
        // 3. 포인트 내역 기록
        PointHistoryEntity history = PointHistoryEntity.builder()
            .customer(customer)
            .pointType(PointType.USE)
            .pointAmount(-usePointAmount) // 음수로 기록
            .pointBalanceAfter(customer.getPointBalance())
            .orderNo(order.getOrderNo())
            .description(String.format("주문 결제 사용 (주문번호: %d)", order.getOrderNo()))
            .createdAt(LocalDateTime.now())
            .build();
        
        pointHistoryRepository.save(history);
        customerRepository.save(customer);
    }

    /**
     * 관리자 수동 포인트 지급
     * @param customer 고객
     * @param pointAmount 지급할 포인트 금액
     * @param description 설명
     * @param admin 관리자
     */
    @Transactional
    public void manualAddPoint(CustomerEntity customer, Long pointAmount, String description, 
                               com.swimshop.swim_mall.admin.entity.AdminEntity admin) {
        if (pointAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "포인트 금액은 0보다 커야 합니다.");
        }

        // 1. 고객 포인트 잔액 증가
        customer.addPoint(pointAmount);
        
        // 2. 포인트 내역 기록
        PointHistoryEntity history = PointHistoryEntity.builder()
            .customer(customer)
            .pointType(PointType.MANUAL_ADD)
            .pointAmount(pointAmount)
            .pointBalanceAfter(customer.getPointBalance())
            .description(description != null ? description : "관리자 수동 지급")
            .admin(admin)
            .createdAt(LocalDateTime.now())
            .build();
        
        pointHistoryRepository.save(history);
        customerRepository.save(customer);
    }

    /**
     * 관리자 수동 포인트 차감
     * @param customer 고객
     * @param pointAmount 차감할 포인트 금액
     * @param description 설명
     * @param admin 관리자
     */
    @Transactional
    public void manualDeductPoint(CustomerEntity customer, Long pointAmount, String description,
                                 com.swimshop.swim_mall.admin.entity.AdminEntity admin) {
        if (pointAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "포인트 금액은 0보다 커야 합니다.");
        }

        // 1. 포인트 잔액 확인
        if (customer.getPointBalance() < pointAmount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        // 2. 고객 포인트 잔액 차감
        customer.deductPoint(pointAmount);
        
        // 3. 포인트 내역 기록
        PointHistoryEntity history = PointHistoryEntity.builder()
            .customer(customer)
            .pointType(PointType.MANUAL_DEDUCT)
            .pointAmount(-pointAmount) // 음수로 기록
            .pointBalanceAfter(customer.getPointBalance())
            .description(description != null ? description : "관리자 수동 차감")
            .admin(admin)
            .createdAt(LocalDateTime.now())
            .build();
        
        pointHistoryRepository.save(history);
        customerRepository.save(customer);
    }

    /**
     * 고객의 포인트 내역 조회
     * @param customer 고객
     * @return 포인트 내역 목록
     */
    public List<PointHistoryEntity> getPointHistory(CustomerEntity customer) {
        return pointHistoryRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    /**
     * 고객의 포인트 내역 조회 (고객 ID로)
     * @param customerId 고객 ID
     * @return 포인트 내역 목록
     */
    public List<PointHistoryEntity> getPointHistoryByCustomerId(Long customerId) {
        return pointHistoryRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * 포인트 복구 (주문 취소 또는 반품/환불 시)
     * @param customer 고객
     * @param orderNo 주문 번호
     * @param reason 복구 사유 (예: "주문 취소", "반품/환불 완료")
     */
    @Transactional
    public void restorePoint(CustomerEntity customer, Long orderNo, String reason) {
        // 해당 주문에서 사용된 포인트 내역 찾기 (USE 타입, 음수 금액)
        List<PointHistoryEntity> useHistories = pointHistoryRepository
                .findByCustomerAndOrderNoAndPointType(
                    customer, 
                    orderNo, 
                    PointType.USE
                );
        
        if (useHistories.isEmpty()) {
            // 해당 주문에서 포인트를 사용하지 않았으면 복구할 필요 없음
            return;
        }
        
        // 사용된 포인트 내역 중 음수인 것만 필터링 (실제 사용 내역)
        List<PointHistoryEntity> actualUseHistories = useHistories.stream()
                .filter(history -> history.getPointAmount() < 0)
                .toList();
        
        if (actualUseHistories.isEmpty()) {
            return; // 실제 사용 내역이 없음
        }
        
        // 사용된 포인트 총액 계산 (음수로 저장되어 있으므로 절댓값 사용)
        Long totalUsedPoint = actualUseHistories.stream()
                .mapToLong(history -> Math.abs(history.getPointAmount()))
                .sum();
        
        if (totalUsedPoint <= 0) {
            return; // 복구할 포인트가 없음
        }
        
        // 이미 복구된 포인트인지 확인 (복구 내역이 있는지 확인)
        // 복구 내역은 USE 타입이지만 양수로 기록됨
        boolean alreadyRestored = pointHistoryRepository
                .findByCustomerAndOrderNo(customer, orderNo)
                .stream()
                .anyMatch(history -> 
                    history.getPointType() == PointType.USE 
                    && history.getPointAmount() > 0 // 양수면 복구 내역
                    && history.getDescription() != null 
                    && history.getDescription().contains("복구")
                );
        
        if (alreadyRestored) {
            // 이미 복구된 주문이면 중복 복구 방지
            return;
        }
        
        // 1. 고객 포인트 잔액 복구 (증가)
        customer.addPoint(totalUsedPoint);
        
        // 2. 포인트 복구 내역 기록
        PointHistoryEntity restoreHistory = PointHistoryEntity.builder()
                .customer(customer)
                .pointType(PointType.USE) // USE 타입이지만 양수로 기록하여 복구를 나타냄
                .pointAmount(totalUsedPoint) // 양수로 기록 (복구)
                .pointBalanceAfter(customer.getPointBalance())
                .orderNo(orderNo)
                .description(String.format("%s (주문번호: %d)", reason, orderNo))
                .createdAt(LocalDateTime.now())
                .build();
        
        pointHistoryRepository.save(restoreHistory);
        customerRepository.save(customer);
    }
}
