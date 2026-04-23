package com.swimshop.swim_mall.order.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.customer.service.CustomerGradeService;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.point.service.PointService;
import com.swimshop.swim_mall.event.partner.service.PartnerEventService;
import com.swimshop.swim_mall.event.admin.service.AdminEventRewardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 상품 자동 구매 확정 스케줄러
 * 배송 완료 후 7일 경과 시 자동으로 구매 확정 처리 (주문 상품 단위)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteScheduler {

    private final OrderItemRepository orderItemRepository;
    private final CustomerGradeService customerGradeService;
    private final CustomerRepository customerRepository;
    private final PointService pointService;
    private final PartnerEventService partnerEventService;
    private final AdminEventRewardService adminEventRewardService;

    /**
     * 매일 새벽 2시에 실행 (배송 완료 후 7일 경과한 주문 상품 자동 구매 확정)
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    @Transactional
    public void autoCompleteOrderItems() {
        log.info("자동 구매 확정 스케줄러 시작 (주문 상품 단위)...");
        
        try {
            // 배송 완료 후 7일 경과한 주문 상품 조회
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<OrderItemEntity> targetOrderItems = orderItemRepository.findAutoCompleteTargets(
                DeliveryStatus.DELIVERED,
                sevenDaysAgo
            );
            
            int completedCount = 0;
            java.util.Set<Long> updatedCustomerIds = new java.util.HashSet<>();
            
            for (OrderItemEntity orderItem : targetOrderItems) {
                try {
                    // 자동 구매 확정 시도
                    orderItem.autoCompleteOrderItem();
                    
                    // 구매 확정되었는지 확인
                    if (orderItem.getCompletedAt() != null) {
                        orderItemRepository.save(orderItem);
                        completedCount++;
                        
                        // 등급 및 포인트 적립 처리
                        Long customerId = orderItem.getOrder().getCustomer().getCustomerId();
                        try {
                            // 1) 고객 등급 최신화 (구매확정 반영)
                            customerGradeService.updateCustomerGrade(customerId);
                            
                            // 2) 최신 고객 정보 재조회 (등급 반영된 상태로 가져오기)
                            CustomerEntity customer = customerRepository.findById(customerId)
                                    .orElse(orderItem.getOrder().getCustomer());
                            
                            // 3) 등급별 적립률로 포인트 계산 및 적립
                            if (customer.getCustomerGrade() != null) {
                                Double rate = customer.getCustomerGrade().getPointAccumulationRate();
                                Long amount = orderItem.getItemTotalPrice();
                                Long point = (long) (amount * rate / 100.0);
                                if (point > 0) {
                                    pointService.accumulatePoint(customer, orderItem, point);
                                    partnerEventService.handleOrderItemCompleted(orderItem, customer, point);
                                    adminEventRewardService.handleOrderItemCompleted(orderItem, customer, point);
                                }
                            }
                        } catch (Exception e) {
                            // 포인트 적립/등급 업데이트 실패는 구매확정 성공과 분리
                            log.warn("자동 구매확정 후 포인트/등급 처리 실패 (orderItemNo: {}, customerId: {}): {}",
                                orderItem.getOrderItemNo(), customerId, e.getMessage());
                        }
                        
                        // 후속 집계용 고객 ID 수집
                        updatedCustomerIds.add(customerId);
                        
                        log.info("주문 상품 {} 자동 구매 확정 완료 (주문 번호: {})", 
                            orderItem.getOrderItemNo(), 
                            orderItem.getOrder().getOrderNo());
                    }
                } catch (Exception e) {
                    log.warn("주문 상품 {} 자동 구매 확정 실패: {}", 
                        orderItem.getOrderItemNo(), e.getMessage());
                }
            }
            
            // 구매 확정된 고객들의 등급 업데이트
            for (Long customerId : updatedCustomerIds) {
                try {
                    customerGradeService.updateCustomerGrade(customerId);
                } catch (Exception e) {
                    log.warn("고객 등급 업데이트 실패 (고객 ID: {}): {}", customerId, e.getMessage());
                }
            }
            
            log.info("자동 구매 확정 완료: {}건, 등급 업데이트: {}명", completedCount, updatedCustomerIds.size());
        } catch (Exception e) {
            log.error("자동 구매 확정 스케줄러 실행 중 오류 발생", e);
        }
    }
}
