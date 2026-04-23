package com.swimshop.swim_mall.customer.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.entity.CustomerGradeEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerGradeRepository;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 고객 등급 관리 서비스
 * 등급 자동 업데이트 로직 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerGradeService {

    private final CustomerRepository customerRepository;
    private final CustomerGradeRepository customerGradeRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 고객 등급 자동 업데이트
     * 구매 확정된 주문상품 기준으로 누적 구매액과 주문 건수를 계산하여 등급을 업데이트
     * 
     * @param customerId 고객 ID
     */
    @Transactional
    public void updateCustomerGrade(Long customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다: " + customerId));

        // 구매 확정된 주문상품 기준으로 누적 구매액 계산 (DB 집계)
        Long totalPurchaseAmount = orderItemRepository.sumCompletedItemAmountByCustomerId(customerId);

        // 구매 확정된 주문 상품 기준으로 주문 건수(=완료된 주문상품 수) 계산 (DB 집계)
        Long totalOrderCount = orderItemRepository.countCompletedItemsByCustomerId(customerId);

        // CustomerEntity 업데이트
        customer.updateTotalPurchaseAmount(totalPurchaseAmount);
        customer.updateTotalOrderCount(totalOrderCount.intValue());

        // 적합한 등급 찾기
        List<CustomerGradeEntity> activeGrades = customerGradeRepository.findByIsActiveTrueOrderByGradeLevelAsc();
        
        if (activeGrades.isEmpty()) {
            log.warn("활성화된 등급이 없습니다. 고객 ID: {}", customerId);
            return;
        }

        // 조건을 만족하는 등급 중 가장 높은 등급 찾기
        Optional<CustomerGradeEntity> newGrade = activeGrades.stream()
                .filter(grade -> 
                    totalPurchaseAmount >= grade.getMinPurchaseAmount() &&
                    totalOrderCount >= grade.getMinOrderCount()
                )
                .max(Comparator.comparing(CustomerGradeEntity::getGradeLevel));

        if (newGrade.isPresent()) {
            CustomerGradeEntity grade = newGrade.get();
            CustomerGradeEntity currentGrade = customer.getCustomerGrade();
            
            // 등급이 변경된 경우에만 업데이트
            if (currentGrade == null || !currentGrade.getGradeId().equals(grade.getGradeId())) {
                customer.setCustomerGrade(grade);
                log.info("고객 등급 업데이트: 고객 ID={}, 이전 등급={}, 새 등급={}, 누적 구매액={}, 주문 건수={}",
                    customerId,
                    currentGrade != null ? currentGrade.getGradeName().getCode() : "없음",
                    grade.getGradeName().getCode(),
                    totalPurchaseAmount,
                    totalOrderCount
                );
            }
        } else {
            // 조건을 만족하는 등급이 없으면 기본 등급(가장 낮은 등급) 설정
            CustomerGradeEntity defaultGrade = activeGrades.get(0);
            if (customer.getCustomerGrade() == null || 
                !customer.getCustomerGrade().getGradeId().equals(defaultGrade.getGradeId())) {
                customer.setCustomerGrade(defaultGrade);
                log.info("고객 기본 등급 설정: 고객 ID={}, 등급={}", customerId, defaultGrade.getGradeName().getCode());
            }
        }

        customerRepository.save(customer);
    }

    /**
     * 고객 등급 수동 업데이트 (관리자용)
     * 
     * @param customerId 고객 ID
     * @param gradeId 등급 ID
     */
    @Transactional
    public void updateCustomerGradeManually(Long customerId, Long gradeId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다: " + customerId));

        CustomerGradeEntity grade = customerGradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("등급을 찾을 수 없습니다: " + gradeId));

        if (!grade.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 등급은 설정할 수 없습니다: " + grade.getGradeName());
        }

        customer.setCustomerGrade(grade);
        customerRepository.save(customer);
        
        log.info("고객 등급 수동 업데이트: 고객 ID={}, 등급={}", customerId, grade.getGradeName().getCode());
    }
}
