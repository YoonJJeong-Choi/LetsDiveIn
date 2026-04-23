package com.swimshop.swim_mall.settlement.service;

import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.settlement.dto.AdminSettlementItemDto;
import com.swimshop.swim_mall.settlement.dto.AdminSettlementResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementCreateRequestDto;
import com.swimshop.swim_mall.settlement.dto.SettlementDetailResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementItemDto;
import com.swimshop.swim_mall.settlement.dto.SettlementListResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementOrderItemDto;
import com.swimshop.swim_mall.settlement.dto.SettlementResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementSummaryDto;
import com.swimshop.swim_mall.settlement.dto.SettlementDashboardDto;
import com.swimshop.swim_mall.settlement.dto.PartnerSettlementRankDto;
import com.swimshop.swim_mall.settlement.dto.MonthlySettlementDto;
import com.swimshop.swim_mall.settlement.entity.SettlementEntity;
import com.swimshop.swim_mall.settlement.entity.SettlementOrderItemEntity;
import com.swimshop.swim_mall.settlement.entity.SettlementHistoryEntity;
import com.swimshop.swim_mall.settlement.repository.SettlementRepository;
import com.swimshop.swim_mall.settlement.repository.SettlementOrderItemRepository;
import com.swimshop.swim_mall.settlement.repository.SettlementHistoryRepository;
import com.swimshop.swim_mall.settlement.dto.SettlementHistoryDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final OrderItemRepository orderItemRepository;
    private final AuthService authService;
    private final SettlementRepository settlementRepository;
    private final PartnerRepository partnerRepository;
    private final AdminRepository adminRepository;
    private final SettlementOrderItemRepository settlementOrderItemRepository;
    private final SettlementHistoryRepository settlementHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final double COMMISSION_RATE = 0.10; // 수수료율 10%

    /**
     * 파트너의 정산 대상 목록 조회
     * 
     * @param session HTTP 세션
     * @param startDate 정산 기간 시작일 (선택)
     * @param endDate 정산 기간 종료일 (선택)
     * @param status 필터 상태 ("SETTLEMENT_READY": 정산 가능만, "ALL": 전체)
     * @return 정산 대상 목록 및 합계 정보
     */
    public SettlementResponseDto getSettlementItems(
            HttpSession session,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();
        
        // 정산 대상 주문 아이템 조회
        List<OrderItemEntity> orderItems = findSettlementReadyOrderItems(
                partnerId, 
                startDate, 
                endDate
        );
        
        // DTO 변환
        // Product는 LAZY 로딩이므로 트랜잭션 내에서 필요한 필드만 접근
        List<SettlementItemDto> items = orderItems.stream()
                .map(this::toSettlementItemDto)
                .collect(Collectors.toList());
        
        // 상태 필터 적용
        if ("SETTLEMENT_READY".equals(status)) {
            items = items.stream()
                    .filter(SettlementItemDto::getIsSettlementReady)
                    .collect(Collectors.toList());
        }
        
        // 합계 계산
        SettlementSummaryDto summary = calculateSummary(items);
        
        return SettlementResponseDto.builder()
                .items(items)
                .summary(summary)
                .build();
    }
    
    /**
     * 정산 대상 주문 아이템 조회
     * 
     * 조건:
     * - 배송 완료 (DELIVERED)
     * - 구매 확정 (completedAt != null)
     * - 취소되지 않음 (isCancelled = false)
     * - 반품/환불 완료되지 않음 (ReturnEntity가 없거나 ReturnStatus != REFUNDED)
     * - 본인 파트너의 상품
     */
    private List<OrderItemEntity> findSettlementReadyOrderItems(
            Long partnerId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // 기간 필터를 위한 LocalDateTime 변환
        LocalDateTime startDateTime = startDate != null 
                ? startDate.atStartOfDay() 
                : null;
        LocalDateTime endDateTime = endDate != null 
                ? endDate.atTime(LocalTime.MAX) 
                : null;
        
        // 기간 필터에 따라 적절한 메서드 호출
        if (startDateTime != null && endDateTime != null) {
            // 시작일 + 종료일 모두 있는 경우
            return orderItemRepository.findSettlementReadyItemsWithDateRange(
                    partnerId,
                    DeliveryStatus.DELIVERED,
                    startDateTime,
                    endDateTime
            );
        } else if (startDateTime != null) {
            // 시작일만 있는 경우
            return orderItemRepository.findSettlementReadyItemsWithStartDate(
                    partnerId,
                    DeliveryStatus.DELIVERED,
                    startDateTime
            );
        } else if (endDateTime != null) {
            // 종료일만 있는 경우
            return orderItemRepository.findSettlementReadyItemsWithEndDate(
                    partnerId,
                    DeliveryStatus.DELIVERED,
                    endDateTime
            );
        } else {
            // 기간 필터가 없는 경우
            return orderItemRepository.findSettlementReadyItems(
                    partnerId,
                    DeliveryStatus.DELIVERED
            );
        }
    }
    
    /**
     * OrderItemEntity를 SettlementItemDto로 변환
     */
    private SettlementItemDto toSettlementItemDto(OrderItemEntity orderItem) {
        Long salesAmount = orderItem.getItemTotalPrice();
        Long commissionAmount = calculateCommission(salesAmount);
        Long settlementAmount = salesAmount - commissionAmount;
        
        // 정산 가능 여부 판단
        boolean isSettlementReady = isSettlementReady(orderItem);
        
        // Product 초기화 (LOB 필드 접근 방지)
        String productName = null;
        if (orderItem.getProduct() != null) {
            // Product 엔티티를 명시적으로 초기화하지 않고 상품명만 가져옴
            // Hibernate.initialize()를 사용하지 않고 직접 접근
            productName = orderItem.getProduct().getProductName();
        }
        
        return SettlementItemDto.builder()
                .orderId(orderItem.getOrder().getOrderNo())
                .orderItemId(orderItem.getOrderItemNo())
                .productName(productName)
                .quantity(orderItem.getItemQuantity())
                .salesAmount(salesAmount)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .orderDate(orderItem.getOrder().getOrderCreatedAt())
                .deliveryCompletedDate(
                        orderItem.getDelivery() != null 
                                ? orderItem.getDelivery().getDeliveryEndDate() 
                                : null
                )
                .isSettlementReady(isSettlementReady)
                .build();
    }
    
    /**
     * 정산 가능 여부 판단
     */
    private boolean isSettlementReady(OrderItemEntity orderItem) {
        // 배송 완료 확인
        if (orderItem.getDelivery() == null || 
            orderItem.getDelivery().getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            return false;
        }
        
        // 구매 확정 확인
        if (orderItem.getCompletedAt() == null) {
            return false;
        }
        
        // 취소 확인
        if (orderItem.getIsCancelled()) {
            return false;
        }
        
        // 반품/환불 확인
        if (orderItem.getReturnEntity() != null) {
            ReturnStatus returnStatus = orderItem.getReturnEntity().getReturnStatus();
            if (returnStatus == ReturnStatus.REFUNDED) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 수수료 계산 (판매금액 × 10%)
     */
    private Long calculateCommission(Long salesAmount) {
        return Math.round(salesAmount * COMMISSION_RATE);
    }
    
    /**
     * 합계 계산
     */
    private SettlementSummaryDto calculateSummary(List<SettlementItemDto> items) {
        // 정산 가능한 항목만 합계 계산
        List<SettlementItemDto> readyItems = items.stream()
                .filter(SettlementItemDto::getIsSettlementReady)
                .collect(Collectors.toList());
        
        long totalSalesAmount = readyItems.stream()
                .mapToLong(SettlementItemDto::getSalesAmount)
                .sum();
        
        long totalCommissionAmount = readyItems.stream()
                .mapToLong(SettlementItemDto::getCommissionAmount)
                .sum();
        
        long totalSettlementAmount = readyItems.stream()
                .mapToLong(SettlementItemDto::getSettlementAmount)
                .sum();
        
        return SettlementSummaryDto.builder()
                .totalSalesAmount(totalSalesAmount)
                .totalCommissionAmount(totalCommissionAmount)
                .totalSettlementAmount(totalSettlementAmount)
                .settlementReadyCount(readyItems.size())
                .build();
    }
    
    /**
     * 관리자용 정산 대상 목록 조회 (전체 파트너 또는 특정 파트너)
     * 
     * @param session HTTP 세션
     * @param partnerId 파트너 ID (선택, null이면 전체 파트너)
     * @param startDate 정산 기간 시작일 (선택)
     * @param endDate 정산 기간 종료일 (선택)
     * @param status 필터 상태 ("SETTLEMENT_READY": 정산 가능만, "ALL": 전체)
     * @return 정산 대상 목록 및 합계 정보
     */
    public AdminSettlementResponseDto getAdminSettlementItems(
            HttpSession session,
            Long partnerId,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 정산 대상 주문 아이템 조회
        List<OrderItemEntity> orderItems;
        if (partnerId != null) {
            // 특정 파트너의 정산 대상 조회
            orderItems = findSettlementReadyOrderItems(partnerId, startDate, endDate);
        } else {
            // 전체 파트너의 정산 대상 조회
            orderItems = findAllPartnersSettlementReadyOrderItems(startDate, endDate);
        }
        
        // DTO 변환
        // Product는 LAZY 로딩이므로 트랜잭션 내에서 필요한 필드만 접근
        List<AdminSettlementItemDto> items = orderItems.stream()
                .map(this::toAdminSettlementItemDto)
                .collect(Collectors.toList());
        
        // 상태 필터 적용
        if ("SETTLEMENT_READY".equals(status)) {
            items = items.stream()
                    .filter(AdminSettlementItemDto::getIsSettlementReady)
                    .collect(Collectors.toList());
        }
        
        // 합계 계산
        SettlementSummaryDto summary = calculateAdminSummary(items);
        
        // 파트너 수 계산 (전체 조회 시)
        Integer totalPartnerCount = null;
        if (partnerId == null) {
            Set<Long> uniquePartnerIds = items.stream()
                    .map(AdminSettlementItemDto::getPartnerId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            totalPartnerCount = uniquePartnerIds.size();
        }
        
        return AdminSettlementResponseDto.builder()
                .items(items)
                .summary(summary)
                .totalPartnerCount(totalPartnerCount)
                .build();
    }
    
    /**
     * 전체 파트너의 정산 대상 주문 아이템 조회
     */
    private List<OrderItemEntity> findAllPartnersSettlementReadyOrderItems(
            LocalDate startDate,
            LocalDate endDate
    ) {
        // 기간 필터를 위한 LocalDateTime 변환
        LocalDateTime startDateTime = startDate != null 
                ? startDate.atStartOfDay() 
                : null;
        LocalDateTime endDateTime = endDate != null 
                ? endDate.atTime(LocalTime.MAX) 
                : null;
        
        // 전체 파트너의 정산 대상 조회 쿼리
        if (startDateTime != null && endDateTime != null) {
            return orderItemRepository.findAllPartnersSettlementReadyItemsWithDateRange(
                    DeliveryStatus.DELIVERED,
                    startDateTime,
                    endDateTime
            );
        } else if (startDateTime != null) {
            return orderItemRepository.findAllPartnersSettlementReadyItemsWithStartDate(
                    DeliveryStatus.DELIVERED,
                    startDateTime
            );
        } else if (endDateTime != null) {
            return orderItemRepository.findAllPartnersSettlementReadyItemsWithEndDate(
                    DeliveryStatus.DELIVERED,
                    endDateTime
            );
        } else {
            return orderItemRepository.findAllPartnersSettlementReadyItems(
                    DeliveryStatus.DELIVERED
            );
        }
    }
    
    /**
     * OrderItemEntity를 AdminSettlementItemDto로 변환
     */
    private AdminSettlementItemDto toAdminSettlementItemDto(OrderItemEntity orderItem) {
        Long salesAmount = orderItem.getItemTotalPrice();
        Long commissionAmount = calculateCommission(salesAmount);
        Long settlementAmount = salesAmount - commissionAmount;
        
        // 정산 가능 여부 판단
        boolean isSettlementReady = isSettlementReady(orderItem);
        
        // 파트너 정보 추출
        Long partnerId = null;
        String partnerName = null;
        String productName = null;
        
        if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
            partnerId = orderItem.getOption().getPartner().getPartnerId();
            partnerName = orderItem.getOption().getPartner().getPartnerName();
        } else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
            partnerId = orderItem.getProduct().getPartner().getPartnerId();
            partnerName = orderItem.getProduct().getPartner().getPartnerName();
        }
        
        // Product 초기화 (LOB 필드 접근 방지)
        if (orderItem.getProduct() != null) {
            productName = orderItem.getProduct().getProductName();
        }
        
        return AdminSettlementItemDto.builder()
                .partnerId(partnerId)
                .partnerName(partnerName)
                .orderId(orderItem.getOrder().getOrderNo())
                .orderItemId(orderItem.getOrderItemNo())
                .productName(productName)
                .quantity(orderItem.getItemQuantity())
                .salesAmount(salesAmount)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .orderDate(orderItem.getOrder().getOrderCreatedAt())
                .deliveryCompletedDate(
                        orderItem.getDelivery() != null 
                                ? orderItem.getDelivery().getDeliveryEndDate() 
                                : null
                )
                .isSettlementReady(isSettlementReady)
                .build();
    }
    
    /**
     * 관리자용 합계 계산
     */
    private SettlementSummaryDto calculateAdminSummary(List<AdminSettlementItemDto> items) {
        // 정산 가능한 항목만 합계 계산
        List<AdminSettlementItemDto> readyItems = items.stream()
                .filter(AdminSettlementItemDto::getIsSettlementReady)
                .collect(Collectors.toList());
        
        long totalSalesAmount = readyItems.stream()
                .mapToLong(AdminSettlementItemDto::getSalesAmount)
                .sum();
        
        long totalCommissionAmount = readyItems.stream()
                .mapToLong(AdminSettlementItemDto::getCommissionAmount)
                .sum();
        
        long totalSettlementAmount = readyItems.stream()
                .mapToLong(AdminSettlementItemDto::getSettlementAmount)
                .sum();
        
        return SettlementSummaryDto.builder()
                .totalSalesAmount(totalSalesAmount)
                .totalCommissionAmount(totalCommissionAmount)
                .totalSettlementAmount(totalSettlementAmount)
                .settlementReadyCount(readyItems.size())
                .build();
    }
    
    /**
     * 정산 생성 (관리자용)
     * 정산 가능한 주문 아이템들을 선택하여 정산 엔티티 생성
     * 
     * @param session HTTP 세션
     * @param request 정산 생성 요청
     * @return 생성된 정산 정보
     */
    @Transactional
    public SettlementDetailResponseDto createSettlement(
            HttpSession session,
            SettlementCreateRequestDto request
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long adminId = currentUser.getSubjectId();
        
        // 관리자 엔티티 조회
        AdminEntity admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        
        // 파트너 엔티티 조회
        PartnerEntity partner = partnerRepository.findById(request.getPartnerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 정산 처리할 주문 아이템 조회 및 검증
        List<OrderItemEntity> orderItems = request.getOrderItemIds().stream()
                .map(orderItemId -> orderItemRepository.findById(orderItemId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND)))
                .collect(Collectors.toList());
        
        // 정산 가능 여부 검증
        for (OrderItemEntity orderItem : orderItems) {
            if (!isSettlementReady(orderItem)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                        "정산 불가능한 주문 아이템이 포함되어 있습니다: " + orderItem.getOrderItemNo());
            }
            
            // 파트너 소유 확인
            Long itemPartnerId = null;
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                itemPartnerId = orderItem.getOption().getPartner().getPartnerId();
            } else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                itemPartnerId = orderItem.getProduct().getPartner().getPartnerId();
            }
            
            if (itemPartnerId == null || !itemPartnerId.equals(request.getPartnerId())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                        "파트너 소유가 아닌 주문 아이템이 포함되어 있습니다: " + orderItem.getOrderItemNo());
            }
        }
        
        // 정산 금액 계산
        long totalSalesAmount = orderItems.stream()
                .mapToLong(OrderItemEntity::getItemTotalPrice)
                .sum();
        
        long totalCommissionAmount = calculateCommission(totalSalesAmount);
        long totalSettlementAmount = totalSalesAmount - totalCommissionAmount;
        
        // 정산 기간 자동 계산: 선택한 주문 아이템들의 실제 주문일 범위
        // 정산 기간 = 이 정산에 포함된 주문들이 실제로 발생한 기간 (주문일 기준)
        List<LocalDate> orderDates = orderItems.stream()
                .map(orderItem -> orderItem.getOrder().getOrderCreatedAt().toLocalDate())
                .collect(Collectors.toList());
        
        LocalDate calculatedPeriodStart;
        LocalDate calculatedPeriodEnd;
        
        if (!orderDates.isEmpty()) {
            // 선택한 주문 아이템들의 주문일 중 가장 빠른 날짜와 가장 늦은 날짜
            calculatedPeriodStart = orderDates.stream()
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());
            calculatedPeriodEnd = orderDates.stream()
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
        } else {
            // 주문일이 없는 경우 정산 생성일로 설정
            calculatedPeriodStart = LocalDate.now();
            calculatedPeriodEnd = LocalDate.now();
        }
        
        // 정산 엔티티 생성
        SettlementEntity settlement = SettlementEntity.create(
                totalSalesAmount,
                totalCommissionAmount,
                totalSettlementAmount,
                LocalDate.now(),
                SettlementStatus.PENDING,
                calculatedPeriodStart,
                calculatedPeriodEnd,
                admin,
                partner
        );
        
        SettlementEntity savedSettlement = settlementRepository.save(settlement);
        
        // 정산-주문 아이템 관계 저장
        List<SettlementOrderItemEntity> settlementOrderItems = orderItems.stream()
                .map(orderItem -> SettlementOrderItemEntity.create(savedSettlement, orderItem))
                .collect(Collectors.toList());
        settlementOrderItemRepository.saveAll(settlementOrderItems);
        
        // 정산 생성 이력 기록
        try {
            Map<String, Object> createData = new HashMap<>();
            createData.put("status", SettlementStatus.PENDING.name());
            createData.put("totalSalesAmount", totalSalesAmount);
            createData.put("commissionAmount", totalCommissionAmount);
            createData.put("settlementAmount", totalSettlementAmount);
            createData.put("itemCount", orderItems.size());
            String newValueJson = objectMapper.writeValueAsString(createData);
            
            SettlementHistoryEntity history = SettlementHistoryEntity.create(
                    savedSettlement,
                    admin,
                    "CREATE",
                    null,
                    newValueJson,
                    "정산 생성"
            );
            settlementHistoryRepository.save(history);
        } catch (Exception e) {
            // 이력 기록 실패해도 정산 생성은 계속 진행
            // 로그만 남기고 예외는 던지지 않음
        }
        
        // 응답 DTO 생성
        return SettlementDetailResponseDto.builder()
                .settlementId(savedSettlement.getSettlementId())
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .totalSalesAmount(totalSalesAmount)
                .commissionAmount(totalCommissionAmount)
                .settlementAmount(totalSettlementAmount)
                .settlementCreatedAt(savedSettlement.getSettlementCreatedAt())
                .settlementStatus(savedSettlement.getSettlementStatus().name())
                .settlementPeriodStart(savedSettlement.getSettlementPeriodStart())
                .settlementPeriodEnd(savedSettlement.getSettlementPeriodEnd())
                .settlementPaidDate(savedSettlement.getSettlementPaidDate())
                .itemCount(orderItems.size())
                .build();
    }
    
    /**
     * 정산 상세 조회 (관리자용)
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return 정산 상세 정보 (포함된 주문 아이템 목록 포함)
     */
    public SettlementDetailResponseDto getSettlementDetail(
            HttpSession session,
            Long settlementId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 정산 엔티티 조회
        SettlementEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
        
        return toSettlementDetailDto(settlement);
    }
    
    /**
     * 생성된 정산 목록 조회 (관리자용)
     * 
     * @param session HTTP 세션
     * @param partnerId 파트너 ID (선택, null이면 전체 파트너)
     * @param status 정산 상태 필터 (선택, null이면 전체)
     * @return 생성된 정산 목록
     */
    public SettlementListResponseDto getSettlementList(
            HttpSession session,
            Long partnerId,
            SettlementStatus status
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 정산 목록 조회
        List<SettlementEntity> settlements;
        if (partnerId != null) {
            settlements = settlementRepository.findByPartner_PartnerId(partnerId);
        } else {
            settlements = settlementRepository.findAll();
        }
        
        // PROCESSING 상태인 정산이 있으면 PENDING으로 자동 변환 (마이그레이션)
        @SuppressWarnings("deprecation")
        List<SettlementEntity> settlementsToUpdate = settlements.stream()
                .filter(s -> s.getSettlementStatus() == SettlementStatus.PROCESSING)
                .collect(Collectors.toList());
        if (!settlementsToUpdate.isEmpty()) {
            for (SettlementEntity settlement : settlementsToUpdate) {
                settlement.updateStatus(SettlementStatus.PENDING);
            }
            settlementRepository.saveAll(settlementsToUpdate);
        }
        
        // 상태 필터 적용
        if (status != null) {
            settlements = settlements.stream()
                    .filter(s -> s.getSettlementStatus() == status)
                    .collect(Collectors.toList());
        }
        
        // 생성일 기준 내림차순 정렬
        settlements = settlements.stream()
                .sorted((a, b) -> b.getSettlementCreatedAt().compareTo(a.getSettlementCreatedAt()))
                .collect(Collectors.toList());
        
        // DTO 변환
        List<SettlementDetailResponseDto> settlementDtos = settlements.stream()
                .map(this::toSettlementDetailDto)
                .collect(Collectors.toList());
        
        return SettlementListResponseDto.builder()
                .settlements(settlementDtos)
                .totalCount(settlementDtos.size())
                .build();
    }
    
    /**
     * 파트너용 생성된 정산 목록 조회
     * 
     * @param session HTTP 세션
     * @param status 정산 상태 필터 (선택, null이면 전체)
     * @return 생성된 정산 목록
     */
    public SettlementListResponseDto getPartnerSettlementList(
            HttpSession session,
            SettlementStatus status
    ) {
        // 파트너 권한 확인 및 파트너 ID 가져오기
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너 권한이 필요합니다.");
        }
        Long partnerId = currentUser.getSubjectId();
        
        // 파트너 존재 확인
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 정산 목록 조회
        List<SettlementEntity> settlements = settlementRepository.findByPartner_PartnerId(partnerId);
        
        // PROCESSING 상태인 정산이 있으면 PENDING으로 자동 변환 (마이그레이션)
        @SuppressWarnings("deprecation")
        List<SettlementEntity> settlementsToUpdate = settlements.stream()
                .filter(s -> s.getSettlementStatus() == SettlementStatus.PROCESSING)
                .collect(Collectors.toList());
        if (!settlementsToUpdate.isEmpty()) {
            for (SettlementEntity settlement : settlementsToUpdate) {
                settlement.updateStatus(SettlementStatus.PENDING);
            }
            settlementRepository.saveAll(settlementsToUpdate);
        }
        
        // 상태 필터 적용
        if (status != null) {
            settlements = settlements.stream()
                    .filter(s -> s.getSettlementStatus() == status)
                    .collect(Collectors.toList());
        }
        
        // 생성일 기준 내림차순 정렬
        settlements = settlements.stream()
                .sorted((a, b) -> b.getSettlementCreatedAt().compareTo(a.getSettlementCreatedAt()))
                .collect(Collectors.toList());
        
        // DTO 변환
        List<SettlementDetailResponseDto> settlementDtos = settlements.stream()
                .map(this::toSettlementDetailDto)
                .collect(Collectors.toList());
        
        return SettlementListResponseDto.builder()
                .settlements(settlementDtos)
                .totalCount(settlementDtos.size())
                .build();
    }
    
    /**
     * 파트너용 정산 상세 조회
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return 정산 상세 정보 (포함된 주문 아이템 목록 포함)
     */
    public SettlementDetailResponseDto getPartnerSettlementDetail(
            HttpSession session,
            Long settlementId
    ) {
        // 파트너 권한 확인 및 파트너 ID 가져오기
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너 권한이 필요합니다.");
        }
        Long partnerId = currentUser.getSubjectId();
        
        // 정산 엔티티 조회
        SettlementEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
        
        // 파트너 소유 확인
        if (!settlement.getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 정산만 조회할 수 있습니다.");
        }
        
        return toSettlementDetailDto(settlement);
    }
    
    /**
     * SettlementEntity를 SettlementDetailResponseDto로 변환
     */
    private SettlementDetailResponseDto toSettlementDetailDto(SettlementEntity settlement) {
        // 정산에 포함된 주문 아이템 목록 조회
        List<SettlementOrderItemEntity> settlementOrderItems = 
                settlementOrderItemRepository.findBySettlement_SettlementId(settlement.getSettlementId());
        
        List<SettlementOrderItemDto> items = settlementOrderItems.stream()
                .map(soi -> toSettlementOrderItemDto(soi.getOrderItem()))
                .collect(Collectors.toList());
        
        return SettlementDetailResponseDto.builder()
                .settlementId(settlement.getSettlementId())
                .partnerId(settlement.getPartner().getPartnerId())
                .partnerName(settlement.getPartner().getPartnerName())
                .totalSalesAmount(settlement.getTotalSalesAmount())
                .commissionAmount(settlement.getCommissionAmount())
                .settlementAmount(settlement.getSettlementAmount())
                .settlementCreatedAt(settlement.getSettlementCreatedAt())
                .settlementStatus(settlement.getSettlementStatus().name())
                .settlementPeriodStart(settlement.getSettlementPeriodStart())
                .settlementPeriodEnd(settlement.getSettlementPeriodEnd())
                .settlementPaidDate(settlement.getSettlementPaidDate())
                .itemCount(items.size())
                .items(items)
                .build();
    }
    
    /**
     * OrderItemEntity를 SettlementOrderItemDto로 변환
     */
    private SettlementOrderItemDto toSettlementOrderItemDto(OrderItemEntity orderItem) {
        Long salesAmount = orderItem.getItemTotalPrice();
        Long commissionAmount = calculateCommission(salesAmount);
        Long settlementAmount = salesAmount - commissionAmount;
        
        // Product 초기화 (LOB 필드 접근 방지)
        String productName = null;
        if (orderItem.getProduct() != null) {
            productName = orderItem.getProduct().getProductName();
        }
        
        return SettlementOrderItemDto.builder()
                .orderItemId(orderItem.getOrderItemNo())
                .orderId(orderItem.getOrder().getOrderNo())
                .productName(productName)
                .quantity(orderItem.getItemQuantity())
                .salesAmount(salesAmount)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .orderDate(orderItem.getOrder().getOrderCreatedAt())
                .deliveryCompletedDate(
                        orderItem.getDelivery() != null 
                                ? orderItem.getDelivery().getDeliveryEndDate() 
                                : null
                )
                .build();
    }
    
    /**
     * 정산 상태 변경 (관리자용)
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @param status 변경할 상태
     * @param paidDate 정산 지급일 (선택, null 가능)
     * @return 변경된 정산 정보
     */
    @Transactional
    public SettlementDetailResponseDto updateSettlementStatus(
            HttpSession session,
            Long settlementId,
            SettlementStatus status,
            LocalDate paidDate
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long adminId = currentUser.getSubjectId();
        AdminEntity admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        
        // 정산 엔티티 조회
        SettlementEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
        
        // 변경 전 값 저장
        SettlementStatus oldStatus = settlement.getSettlementStatus();
        LocalDate oldPaidDate = settlement.getSettlementPaidDate();
        
        // 상태 변경 및 지급일 설정
        String actionType;
        Map<String, Object> oldValueMap = new HashMap<>();
        Map<String, Object> newValueMap = new HashMap<>();
        
        // 지급일만 수정하는 경우 (상태가 COMPLETED이고 현재 상태도 COMPLETED인 경우)
        if (paidDate != null && status == SettlementStatus.COMPLETED 
                && settlement.getSettlementStatus() == SettlementStatus.COMPLETED) {
            // 상태는 그대로 유지하고 지급일만 업데이트
            settlement.updatePaidDate(paidDate);
            actionType = "PAID_DATE_UPDATE";
            oldValueMap.put("paidDate", oldPaidDate != null ? oldPaidDate.toString() : null);
            newValueMap.put("paidDate", paidDate.toString());
        } else if (paidDate != null) {
            // 상태 변경과 함께 지급일 설정
            settlement.updateStatus(status, paidDate);
            actionType = "STATUS_CHANGE";
            oldValueMap.put("status", oldStatus.name());
            oldValueMap.put("paidDate", oldPaidDate != null ? oldPaidDate.toString() : null);
            newValueMap.put("status", status.name());
            newValueMap.put("paidDate", paidDate.toString());
        } else {
            // 상태만 변경
            settlement.updateStatus(status);
            actionType = "STATUS_CHANGE";
            oldValueMap.put("status", oldStatus.name());
            newValueMap.put("status", status.name());
        }
        settlement = settlementRepository.save(settlement);
        
        // 정산 변경 이력 기록
        try {
            String oldValueJson = objectMapper.writeValueAsString(oldValueMap);
            String newValueJson = objectMapper.writeValueAsString(newValueMap);
            
            SettlementHistoryEntity history = SettlementHistoryEntity.create(
                    settlement,
                    admin,
                    actionType,
                    oldValueJson,
                    newValueJson,
                    null
            );
            settlementHistoryRepository.save(history);
        } catch (Exception e) {
            // 이력 기록 실패해도 상태 변경은 계속 진행
            // 로그만 남기고 예외는 던지지 않음
        }
        
        // 응답 DTO 생성
        return toSettlementDetailDto(settlement);
    }
    
    /**
     * 정산 대시보드 조회 (관리자용)
     * 전체 정산 현황 통계, 파트너별 정산 금액 순위, 월별 정산 현황 제공
     * 
     * @param session HTTP 세션
     * @return 정산 대시보드 데이터
     */
    public SettlementDashboardDto getSettlementDashboard(HttpSession session) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 전체 정산 목록 조회
        List<SettlementEntity> allSettlements = settlementRepository.findAll();
        
        // PROCESSING 상태인 정산이 있으면 PENDING으로 자동 변환 (마이그레이션)
        @SuppressWarnings("deprecation")
        List<SettlementEntity> settlementsToUpdate = allSettlements.stream()
                .filter(s -> s.getSettlementStatus() == SettlementStatus.PROCESSING)
                .collect(Collectors.toList());
        if (!settlementsToUpdate.isEmpty()) {
            for (SettlementEntity settlement : settlementsToUpdate) {
                settlement.updateStatus(SettlementStatus.PENDING);
            }
            settlementRepository.saveAll(settlementsToUpdate);
            // 업데이트 후 다시 조회
            allSettlements = settlementRepository.findAll();
        }
        
        // 전체 정산 현황 통계 계산
        long totalSettlementCount = allSettlements.size();
        long pendingSettlementCount = allSettlements.stream()
                .filter(s -> s.getSettlementStatus() == SettlementStatus.PENDING)
                .count();
        long completedSettlementCount = allSettlements.stream()
                .filter(s -> s.getSettlementStatus() == SettlementStatus.COMPLETED)
                .count();
        
        long totalSettlementAmount = allSettlements.stream()
                .mapToLong(SettlementEntity::getSettlementAmount)
                .sum();
        long totalSalesAmount = allSettlements.stream()
                .mapToLong(SettlementEntity::getTotalSalesAmount)
                .sum();
        long totalCommissionAmount = allSettlements.stream()
                .mapToLong(SettlementEntity::getCommissionAmount)
                .sum();
        
        // 파트너별 정산 금액 순위 계산
        Map<Long, PartnerSettlementData> partnerDataMap = new HashMap<>();
        for (SettlementEntity settlement : allSettlements) {
            Long partnerId = settlement.getPartner().getPartnerId();
            String partnerName = settlement.getPartner().getPartnerName();
            
            partnerDataMap.computeIfAbsent(partnerId, k -> new PartnerSettlementData(partnerId, partnerName))
                    .addSettlement(settlement);
        }
        
        List<PartnerSettlementRankDto> partnerRankings = partnerDataMap.values().stream()
                .map(data -> PartnerSettlementRankDto.builder()
                        .partnerId(data.partnerId)
                        .partnerName(data.partnerName)
                        .totalSettlementAmount(data.totalSettlementAmount)
                        .totalSalesAmount(data.totalSalesAmount)
                        .totalCommissionAmount(data.totalCommissionAmount)
                        .settlementCount(data.settlementCount)
                        .build())
                .sorted((a, b) -> Long.compare(b.getTotalSettlementAmount(), a.getTotalSettlementAmount()))
                .collect(Collectors.toList());
        
        // 순위 추가
        for (int i = 0; i < partnerRankings.size(); i++) {
            PartnerSettlementRankDto dto = partnerRankings.get(i);
            PartnerSettlementRankDto rankedDto = PartnerSettlementRankDto.builder()
                    .partnerId(dto.getPartnerId())
                    .partnerName(dto.getPartnerName())
                    .totalSettlementAmount(dto.getTotalSettlementAmount())
                    .totalSalesAmount(dto.getTotalSalesAmount())
                    .totalCommissionAmount(dto.getTotalCommissionAmount())
                    .settlementCount(dto.getSettlementCount())
                    .rank(i + 1)
                    .build();
            partnerRankings.set(i, rankedDto);
        }
        
        // 월별 정산 현황 계산
        Map<String, MonthlySettlementData> monthlyDataMap = new HashMap<>();
        for (SettlementEntity settlement : allSettlements) {
            String yearMonth = settlement.getSettlementCreatedAt().toString().substring(0, 7); // YYYY-MM
            
            monthlyDataMap.computeIfAbsent(yearMonth, k -> new MonthlySettlementData(yearMonth))
                    .addSettlement(settlement);
        }
        
        List<MonthlySettlementDto> monthlySettlements = monthlyDataMap.values().stream()
                .map(data -> MonthlySettlementDto.builder()
                        .yearMonth(data.yearMonth)
                        .totalSettlementAmount(data.totalSettlementAmount)
                        .totalSalesAmount(data.totalSalesAmount)
                        .totalCommissionAmount(data.totalCommissionAmount)
                        .settlementCount(data.settlementCount)
                        .build())
                .sorted((a, b) -> a.getYearMonth().compareTo(b.getYearMonth()))
                .collect(Collectors.toList());
        
        return SettlementDashboardDto.builder()
                .totalSettlementCount(totalSettlementCount)
                .pendingSettlementCount(pendingSettlementCount)
                .completedSettlementCount(completedSettlementCount)
                .totalSettlementAmount(totalSettlementAmount)
                .totalSalesAmount(totalSalesAmount)
                .totalCommissionAmount(totalCommissionAmount)
                .partnerRankings(partnerRankings)
                .monthlySettlements(monthlySettlements)
                .build();
    }
    
    // 파트너별 정산 데이터를 임시로 저장하는 내부 클래스
    private static class PartnerSettlementData {
        Long partnerId;
        String partnerName;
        long totalSettlementAmount = 0;
        long totalSalesAmount = 0;
        long totalCommissionAmount = 0;
        int settlementCount = 0;
        
        PartnerSettlementData(Long partnerId, String partnerName) {
            this.partnerId = partnerId;
            this.partnerName = partnerName;
        }
        
        void addSettlement(SettlementEntity settlement) {
            this.totalSettlementAmount += settlement.getSettlementAmount();
            this.totalSalesAmount += settlement.getTotalSalesAmount();
            this.totalCommissionAmount += settlement.getCommissionAmount();
            this.settlementCount++;
        }
    }
    
    // 월별 정산 데이터를 임시로 저장하는 내부 클래스
    private static class MonthlySettlementData {
        String yearMonth;
        long totalSettlementAmount = 0;
        long totalSalesAmount = 0;
        long totalCommissionAmount = 0;
        int settlementCount = 0;
        
        MonthlySettlementData(String yearMonth) {
            this.yearMonth = yearMonth;
        }
        
        void addSettlement(SettlementEntity settlement) {
            this.totalSettlementAmount += settlement.getSettlementAmount();
            this.totalSalesAmount += settlement.getTotalSalesAmount();
            this.totalCommissionAmount += settlement.getCommissionAmount();
            this.settlementCount++;
        }
    }
    
    /**
     * 정산 변경 이력 조회 (관리자용)
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return 정산 변경 이력 목록
     */
    public List<SettlementHistoryDto> getSettlementHistory(
            HttpSession session,
            Long settlementId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 정산 존재 확인
        settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
        
        // 정산 변경 이력 조회
        List<SettlementHistoryEntity> histories = settlementHistoryRepository
                .findBySettlementIdOrderByChangedAtDesc(settlementId);
        
        // DTO 변환
        return histories.stream()
                .map(history -> SettlementHistoryDto.builder()
                        .historyId(history.getHistoryId())
                        .settlementId(history.getSettlement().getSettlementId())
                        .adminId(history.getAdmin().getAdminId())
                        .adminName(history.getAdmin().getAdminName())
                        .changedAt(history.getChangedAt())
                        .actionType(history.getActionType() != null
                                ? com.swimshop.swim_mall.common.enums.SettlementHistoryAction.valueOf(history.getActionType())
                                : (com.swimshop.swim_mall.common.enums.SettlementHistoryAction) null)
                        .oldValue(history.getOldValue())
                        .newValue(history.getNewValue())
                        .reason(history.getReason())
                        .build())
                .collect(Collectors.toList());
    }
}
