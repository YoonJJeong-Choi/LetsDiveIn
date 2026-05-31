package com.swimshop.swim_mall.partner.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.DayOfWeek;

import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.repository.AccountRepository;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.color.repository.ColorRepository;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.common.enums.PartnerChangeRequestStatus;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.common.enums.ProductSubType;
import com.swimshop.swim_mall.common.enums.ProductType;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.option.repository.OptionRepository;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.partner.dto.DeactivationRequestDto;
import com.swimshop.swim_mall.partner.dto.OptionDto;
import com.swimshop.swim_mall.partner.dto.PartnerHistoryResponseDto;
import com.swimshop.swim_mall.partner.dto.ReactivationRequestDto;
import com.swimshop.swim_mall.partner.dto.PartnerApplicationRequestDto;
import com.swimshop.swim_mall.partner.dto.PartnerApplicationResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerChangeRequestCreateDto;
import com.swimshop.swim_mall.partner.dto.PartnerChangeRequestResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerProfileResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerProfileUpdateRequestDto;
import com.swimshop.swim_mall.partner.dto.ProductCreateRequestDto;
import com.swimshop.swim_mall.partner.dto.ProductUpdateRequestDto;
import com.swimshop.swim_mall.partner.dto.PartnerSalesStatisticsDto;
import com.swimshop.swim_mall.partner.dto.PartnerDashboardAnalyticsDto;
import com.swimshop.swim_mall.partner.dto.PartnerTodayOperationsDto;
import com.swimshop.swim_mall.delivery.repository.DeliveryRepository;
import com.swimshop.swim_mall.payment.PaymentEntity;
import com.swimshop.swim_mall.payment.PaymentRepository;
import com.swimshop.swim_mall.settlement.dto.SettlementSummaryDto;
import com.swimshop.swim_mall.settlement.repository.SettlementRepository;
import com.swimshop.swim_mall.settlement.service.SettlementService;
import com.swimshop.swim_mall.partner.dto.ProductWithOptionsDto;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.entity.PartnerChangeRequestEntity;
import com.swimshop.swim_mall.partner.entity.PartnerHistoryEntity;
import com.swimshop.swim_mall.partner.entity.PartnerProfileEntity;
import com.swimshop.swim_mall.partner.repository.PartnerChangeRequestRepository;
import com.swimshop.swim_mall.inventory.repository.InventoryRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnRepository;
import com.swimshop.swim_mall.partner.repository.PartnerHistoryRepository;
import com.swimshop.swim_mall.partner.repository.PartnerProfileRepository;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.repository.ProductRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final OptionRepository optionRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerHistoryRepository partnerHistoryRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final ColorRepository colorRepository;
    private final OrderItemRepository orderItemRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerChangeRequestRepository partnerChangeRequestRepository;
    private final InventoryRepository inventoryRepository;
    private final ReturnRepository returnRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;
    private final DeliveryRepository deliveryRepository;

    private static final int PARTNER_LOW_STOCK_THRESHOLD = 5;
    private static final int PARTNER_TREND_MIN_DAYS = 7;
    private static final int PARTNER_TREND_MAX_DAYS = 30;
    private static final int PARTNER_ANALYTICS_TREND_MIN_DAYS = 7;
    private static final int PARTNER_ANALYTICS_TREND_MAX_DAYS = 90;
    private static final int PARTNER_DELIVERY_READY_DELAY_DAYS = 3;
    
    /**
     * 현재 로그인한 파트너의 상품 목록 조회 (상품 단위로 그룹화)
     * Option을 통해 파트너의 상품을 조회하고, 같은 상품의 옵션들을 그룹화
     */
    @Transactional(readOnly = true)
    public List<ProductWithOptionsDto> getMyProducts(HttpSession session) {
        // 1. 세션에서 파트너 ID 가져오기
        Long partnerId = getCurrentPartnerId(session);
        
        // 2. 파트너의 옵션 목록 조회 (상품 정보 포함)
        List<OptionEntity> options = optionRepository.findByPartnerIdWithProduct(partnerId);
        
        // 3. 옵션들을 상품별로 그룹화
        Map<Long, List<OptionEntity>> productGroupMap = new LinkedHashMap<>();
        if (!options.isEmpty()) {
            productGroupMap = options.stream()
                    .collect(Collectors.groupingBy(
                            option -> option.getProduct().getProductNo(),
                            LinkedHashMap::new, // 순서 유지
                            Collectors.toList()
                    ));
        }
        
        // 4. 옵션이 없는 상품 조회 (ProductEntity.partner로 직접 연결된 상품)
        List<ProductEntity> productsWithoutOptions = productRepository.findByPartner_PartnerId(partnerId);
        
        // 옵션이 있는 상품의 productNo Set 생성 (중복 제거용)
        Set<Long> productsWithOptionsSet = productGroupMap.keySet();
        
        // 옵션이 없는 상품만 필터링 (옵션이 있는 상품은 이미 productGroupMap에 포함됨)
        List<ProductEntity> productsWithoutOptionsFiltered = productsWithoutOptions.stream()
                .filter(product -> {
                    // 옵션이 있는 상품은 제외
                    if (productsWithOptionsSet.contains(product.getProductNo())) {
                        return false;
                    }
                    // 해당 상품에 옵션이 있는지 확인
                    List<OptionEntity> productOptions = optionRepository.findByProduct_ProductNoAllStatus(product.getProductNo());
                    return productOptions.isEmpty(); // 옵션이 없는 상품만 포함
                })
                .collect(Collectors.toList());
        
        // 5. 각 상품에 해당하는 옵션들을 포함하여 DTO 변환
        // 파트너는 자신의 모든 상품을 볼 수 있음 (ACTIVE, INACTIVE 모두 포함)
        List<ProductWithOptionsDto> result = new ArrayList<>();
        
        // 옵션이 있는 상품 처리
        for (Map.Entry<Long, List<OptionEntity>> entry : productGroupMap.entrySet()) {
            List<OptionEntity> productOptions = entry.getValue();
            OptionEntity firstOption = productOptions.get(0);
            ProductEntity product = firstOption.getProduct();
            
            // 모든 상태의 상품 포함 (파트너는 비활성화된 상품도 관리 가능)
            
            // 옵션 DTO 변환 (INACTIVE 상태 옵션 제외)
            // 삭제된 옵션은 카운트에 포함하지 않음
            List<OptionDto> optionDtos = productOptions.stream()
                    .filter(option -> option.getOptionStatus() != ActiveStatus.INACTIVE)
                    .map(option -> new OptionDto(
                            option.getOptionNo(),
                            option.getColor(),
                            option.getSize(),
                            option.getOptionAddPrice()
                    ))
                    .collect(Collectors.toList());
            
            // 상품 DTO 생성
            ProductWithOptionsDto productDto = new ProductWithOptionsDto(
                    product.getProductNo(),
                    product.getProductName(),
                    product.getProductType() != null ? product.getProductType().name() : null,
                    product.getProductSubType() != null ? product.getProductSubType().name() : null,
                    product.getProductPrice(),
                    product.getProductDescription(),
                    product.getProductImageUrl(),
                    product.getSku(),
                    product.getBrandName(),
                    product.getMaterialInfo(),
                    product.getOriginCountry(),
                    product.getManufactureCountry(),
                    product.getCareInstructions(),
                    product.getSizeGuideText(),
                    product.getSizeGuideJson(),
                    product.getProductCreatedAt(),
                    product.getProductUpdatedAt(),
                    product.getProductActiveStatus() != null ? product.getProductActiveStatus().name() : null,
                    product.getRejectionReason(), // 거절 사유 추가
                    optionDtos
            );
            
            result.add(productDto);
        }
        
        // 옵션이 없는 상품 처리
        for (ProductEntity product : productsWithoutOptionsFiltered) {
            // 옵션이 없는 상품은 빈 옵션 리스트로 DTO 생성
            ProductWithOptionsDto productDto = new ProductWithOptionsDto(
                    product.getProductNo(),
                    product.getProductName(),
                    product.getProductType() != null ? product.getProductType().name() : null,
                    product.getProductSubType() != null ? product.getProductSubType().name() : null,
                    product.getProductPrice(),
                    product.getProductDescription(),
                    product.getProductImageUrl(),
                    product.getSku(),
                    product.getBrandName(),
                    product.getMaterialInfo(),
                    product.getOriginCountry(),
                    product.getManufactureCountry(),
                    product.getCareInstructions(),
                    product.getSizeGuideText(),
                    product.getSizeGuideJson(),
                    product.getProductCreatedAt(),
                    product.getProductUpdatedAt(),
                    product.getProductActiveStatus() != null ? product.getProductActiveStatus().name() : null,
                    product.getRejectionReason(), // 거절 사유 추가
                    new ArrayList<>() // 옵션이 없으므로 빈 리스트
            );
            
            result.add(productDto);
        }
        
        return result;
    }

    /**
     * 파트너 대시보드 — 오늘의 운영(결제·발송 전 주문·승인 대기 상품/옵션)
     */
    @Transactional(readOnly = true)
    public PartnerTodayOperationsDto getPartnerTodayOperations(HttpSession session, int trendDays) {
        Long partnerId = getCurrentPartnerId(session);
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEndExclusive = today.plusDays(1).atStartOfDay();
        List<OrderStatus> paidOrActive = List.of(OrderStatus.PAID, OrderStatus.ACTIVE);

        long todayPaidOrders = orderItemRepository.countDistinctPartnerOrdersPaidBetween(
                partnerId, dayStart, dayEndExclusive, paidOrActive);
        long todayRevenue = orderItemRepository.sumPartnerLineRevenuePaidBetween(
                partnerId, dayStart, dayEndExclusive, paidOrActive);
        long preShipmentOrders = orderItemRepository.countDistinctPartnerOrdersPreShipment(
                partnerId, paidOrActive, DeliveryStatus.READY);

        long productsPendingNew = productRepository.countByPartner_PartnerIdAndProductActiveStatus(
                partnerId, ActiveStatus.PENDING);
        long productsPendingUpdate = productRepository.countByPartner_PartnerIdAndProductActiveStatus(
                partnerId, ActiveStatus.PENDING_UPDATE);
        long optionsPendingNew = optionRepository.countByPartner_PartnerIdAndOptionStatus(
                partnerId, ActiveStatus.PENDING);
        long optionsPendingUpdate = optionRepository.countByPartner_PartnerIdAndOptionStatus(
                partnerId, ActiveStatus.PENDING_UPDATE);

        long lowStockLines = inventoryRepository.countLowStockLinesForPartner(
                partnerId, PARTNER_LOW_STOCK_THRESHOLD, ActiveStatus.ACTIVE);
        List<ReturnStatus> openReturnStatuses = List.of(
                ReturnStatus.REQUESTED,
                ReturnStatus.APPROVED,
                ReturnStatus.PICKUP_COMPLETED);
        long returnsOpen = returnRepository.countByPartnerIdAndReturnStatusIn(partnerId, openReturnStatuses);

        int trendDaysClamped = Math.min(PARTNER_TREND_MAX_DAYS, Math.max(PARTNER_TREND_MIN_DAYS, trendDays));
        List<PartnerTodayOperationsDto.DailyPaidTrendPointDto> dailyTrend =
                buildPartnerDailyPaidTrend(partnerId, today, trendDaysClamped, paidOrActive, null);

        SettlementSummaryDto readySummary = settlementService.getPartnerSettlementReadySummary(session);
        long readySettlementAmount = readySummary.getTotalSettlementAmount() != null
                ? readySummary.getTotalSettlementAmount()
                : 0L;
        long readySalesAmount = readySummary.getTotalSalesAmount() != null
                ? readySummary.getTotalSalesAmount()
                : 0L;
        int readyLineCount = readySummary.getSettlementReadyCount() != null
                ? readySummary.getSettlementReadyCount()
                : 0;

        long pendingBatchCount = settlementRepository.countByPartner_PartnerIdAndSettlementStatus(
                partnerId, SettlementStatus.PENDING);
        long pendingBatchAmount = settlementRepository.sumSettlementAmountByPartnerAndStatus(
                partnerId, SettlementStatus.PENDING);

        return PartnerTodayOperationsDto.builder()
                .todayPaidDistinctOrderCount(todayPaidOrders)
                .todayPartnerLineRevenueKrw(todayRevenue)
                .ordersPreShipmentDistinctCount(preShipmentOrders)
                .productsPendingNewApprovalCount(productsPendingNew)
                .productsPendingUpdateApprovalCount(productsPendingUpdate)
                .optionsPendingNewApprovalCount(optionsPendingNew)
                .optionsPendingUpdateApprovalCount(optionsPendingUpdate)
                .lowStockThresholdUsed(PARTNER_LOW_STOCK_THRESHOLD)
                .lowStockLineCount(lowStockLines)
                .returnsOpenCount(returnsOpen)
                .trendDays(trendDaysClamped)
                .dailyPaidTrend(dailyTrend)
                .settlementReadyTotalSettlementAmountKrw(readySettlementAmount)
                .settlementReadyTotalSalesAmountKrw(readySalesAmount)
                .settlementReadyLineCount(readyLineCount)
                .pendingSettlementBatchCount(pendingBatchCount)
                .pendingSettlementBatchAmountKrw(pendingBatchAmount)
                .build();
    }

    /**
     * 파트너 매출·운영 분석(기간 7~90일, 선택 상품번호로 일별·기간 합계 필터)
     */
    @Transactional(readOnly = true)
    public PartnerDashboardAnalyticsDto getPartnerDashboardAnalytics(
            HttpSession session,
            int trendDays,
            Long productNo
    ) {
        Long partnerId = getCurrentPartnerId(session);
        Long productNoFilter = null;
        if (productNo != null) {
            ProductEntity p = productRepository.findById(productNo)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            if (p.getPartner() == null || !partnerId.equals(p.getPartner().getPartnerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            productNoFilter = productNo;
        }

        LocalDate today = LocalDate.now();
        List<OrderStatus> paidOrActive = List.of(OrderStatus.PAID, OrderStatus.ACTIVE);
        int trendDaysClamped = Math.min(PARTNER_ANALYTICS_TREND_MAX_DAYS,
                Math.max(PARTNER_ANALYTICS_TREND_MIN_DAYS, trendDays));
        LocalDateTime rangeStart = today.minusDays(trendDaysClamped - 1L).atStartOfDay();
        LocalDateTime rangeEndExclusive = today.plusDays(1).atStartOfDay();

        List<PartnerTodayOperationsDto.DailyPaidTrendPointDto> trendRaw =
                buildPartnerDailyPaidTrend(partnerId, today, trendDaysClamped, paidOrActive, productNoFilter);
        List<PartnerDashboardAnalyticsDto.DailyPaidTrendPointDto> dailyTrend = trendRaw.stream()
                .map(pt -> PartnerDashboardAnalyticsDto.DailyPaidTrendPointDto.builder()
                        .date(pt.getDate())
                        .paidOrderCount(pt.getPaidOrderCount())
                        .paidRevenueKrw(pt.getPaidRevenueKrw())
                        .build())
                .collect(Collectors.toList());

        long periodOrders;
        long periodRev;
        if (productNoFilter == null) {
            periodOrders = orderItemRepository.countDistinctPartnerOrdersPaidBetween(
                    partnerId, rangeStart, rangeEndExclusive, paidOrActive);
            periodRev = orderItemRepository.sumPartnerLineRevenuePaidBetween(
                    partnerId, rangeStart, rangeEndExclusive, paidOrActive);
        } else {
            periodOrders = orderItemRepository.countDistinctPartnerOrdersPaidBetweenForProduct(
                    partnerId, rangeStart, rangeEndExclusive, paidOrActive, productNoFilter);
            periodRev = orderItemRepository.sumPartnerLineRevenuePaidBetweenForProduct(
                    partnerId, rangeStart, rangeEndExclusive, paidOrActive, productNoFilter);
        }

        List<Object[]> aggRows = orderItemRepository.aggregatePartnerLineSalesByProductBetween(
                partnerId, rangeStart, rangeEndExclusive, paidOrActive);
        List<PartnerDashboardAnalyticsDto.ProductSkuPerformanceDto> topProducts = new ArrayList<>();
        int rank = 0;
        for (Object[] row : aggRows) {
            if (row == null || row.length < 5) {
                continue;
            }
            long pno = ((Number) row[0]).longValue();
            if (productNoFilter != null && pno != productNoFilter) {
                continue;
            }
            String pname = row[1] != null ? String.valueOf(row[1]) : "";
            String sku = row[2] != null ? String.valueOf(row[2]) : "";
            long qty = row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L;
            long rev = row[4] instanceof Number ? ((Number) row[4]).longValue() : 0L;
            topProducts.add(PartnerDashboardAnalyticsDto.ProductSkuPerformanceDto.builder()
                    .productNo(pno)
                    .productName(pname)
                    .sku(sku)
                    .quantitySold(qty)
                    .lineRevenueKrw(rev)
                    .build());
            rank++;
            if (rank >= 12) {
                break;
            }
        }

        long productsPendingNew = productRepository.countByPartner_PartnerIdAndProductActiveStatus(
                partnerId, ActiveStatus.PENDING);
        long productsPendingUpdate = productRepository.countByPartner_PartnerIdAndProductActiveStatus(
                partnerId, ActiveStatus.PENDING_UPDATE);
        long optionsPendingNew = optionRepository.countByPartner_PartnerIdAndOptionStatus(
                partnerId, ActiveStatus.PENDING);
        long optionsPendingUpdate = optionRepository.countByPartner_PartnerIdAndOptionStatus(
                partnerId, ActiveStatus.PENDING_UPDATE);

        long lowStockLines = inventoryRepository.countLowStockLinesForPartner(
                partnerId, PARTNER_LOW_STOCK_THRESHOLD, ActiveStatus.ACTIVE);
        long outOfStockLines = inventoryRepository.countOutOfStockLinesForPartner(partnerId, ActiveStatus.ACTIVE);

        long preShipmentOrders = orderItemRepository.countDistinctPartnerOrdersPreShipment(
                partnerId, paidOrActive, DeliveryStatus.READY);
        long inDeliveryOrders = orderItemRepository.countDistinctPartnerOrdersInDelivery(
                partnerId, paidOrActive, DeliveryStatus.SHIPPED);
        LocalDateTime deliveryDelayBefore = LocalDateTime.now().minusDays(PARTNER_DELIVERY_READY_DELAY_DAYS);
        long delayedReady = deliveryRepository.countReadyDeliveryConfirmedBeforeForPartner(
                DeliveryStatus.READY, paidOrActive, deliveryDelayBefore, partnerId);

        SettlementSummaryDto readySummary = settlementService.getPartnerSettlementReadySummary(session);
        long readySettlementAmount = readySummary.getTotalSettlementAmount() != null
                ? readySummary.getTotalSettlementAmount()
                : 0L;
        long readySalesAmount = readySummary.getTotalSalesAmount() != null
                ? readySummary.getTotalSalesAmount()
                : 0L;
        int readyLineCount = readySummary.getSettlementReadyCount() != null
                ? readySummary.getSettlementReadyCount()
                : 0;
        long pendingBatchCount = settlementRepository.countByPartner_PartnerIdAndSettlementStatus(
                partnerId, SettlementStatus.PENDING);
        long pendingBatchAmount = settlementRepository.sumSettlementAmountByPartnerAndStatus(
                partnerId, SettlementStatus.PENDING);

        long returnsInPeriod = returnRepository.countPartnerReturnsRequestedBetween(
                partnerId, rangeStart, rangeEndExclusive);
        List<Object[]> reasonRows = returnRepository.countPartnerReturnsByReasonTypeBetween(
                partnerId, rangeStart, rangeEndExclusive);
        List<PartnerDashboardAnalyticsDto.ReturnReasonCountDto> reasonBreakdown = new ArrayList<>();
        for (Object[] rr : reasonRows) {
            if (rr == null || rr.length < 2 || rr[0] == null) {
                continue;
            }
            reasonBreakdown.add(PartnerDashboardAnalyticsDto.ReturnReasonCountDto.builder()
                    .reasonType(String.valueOf(rr[0]))
                    .count(((Number) rr[1]).longValue())
                    .build());
        }

        List<ReturnStatus> openReturnStatuses = List.of(
                ReturnStatus.REQUESTED,
                ReturnStatus.APPROVED,
                ReturnStatus.PICKUP_COMPLETED);
        long returnsOpen = returnRepository.countByPartnerIdAndReturnStatusIn(partnerId, openReturnStatuses);

        return PartnerDashboardAnalyticsDto.builder()
                .trendDays(trendDaysClamped)
                .productNoFilter(productNoFilter)
                .dailyPaidTrend(dailyTrend)
                .periodPaidDistinctOrderCount(periodOrders)
                .periodPartnerLineRevenueKrw(periodRev)
                .topProductsByLineRevenue(topProducts)
                .productsPendingNewApprovalCount(productsPendingNew)
                .productsPendingUpdateApprovalCount(productsPendingUpdate)
                .optionsPendingNewApprovalCount(optionsPendingNew)
                .optionsPendingUpdateApprovalCount(optionsPendingUpdate)
                .lowStockThresholdUsed(PARTNER_LOW_STOCK_THRESHOLD)
                .lowStockLineCount(lowStockLines)
                .outOfStockLineCount(outOfStockLines)
                .ordersPreShipmentDistinctCount(preShipmentOrders)
                .ordersInDeliveryDistinctCount(inDeliveryOrders)
                .deliveryReadyDelayedDaysThreshold(PARTNER_DELIVERY_READY_DELAY_DAYS)
                .deliveriesReadyDelayedPartnerLineCount(delayedReady)
                .settlementReadyTotalSettlementAmountKrw(readySettlementAmount)
                .settlementReadyTotalSalesAmountKrw(readySalesAmount)
                .settlementReadyLineCount(readyLineCount)
                .pendingSettlementBatchCount(pendingBatchCount)
                .pendingSettlementBatchAmountKrw(pendingBatchAmount)
                .returnsRequestedInPeriod(returnsInPeriod)
                .returnReasonBreakdownInPeriod(reasonBreakdown)
                .returnsOpenCount(returnsOpen)
                .build();
    }

    private List<PartnerTodayOperationsDto.DailyPaidTrendPointDto> buildPartnerDailyPaidTrend(
            Long partnerId,
            LocalDate today,
            int trendDays,
            List<OrderStatus> paidOrActive,
            Long productNoFilter
    ) {
        LocalDateTime rangeStart = today.minusDays(trendDays - 1L).atStartOfDay();
        LocalDateTime rangeEndExclusive = today.plusDays(1).atStartOfDay();
        List<PaymentEntity> raw = paymentRepository.findPartnerRelatedPaidPaymentsBetween(
                partnerId, rangeStart, rangeEndExclusive, paidOrActive);
        Map<Long, PaymentEntity> uniqPay = new LinkedHashMap<>();
        for (PaymentEntity p : raw) {
            if (p.getPaymentNo() != null) {
                uniqPay.putIfAbsent(p.getPaymentNo(), p);
            }
        }
        Map<LocalDate, Set<Long>> ordersByDay = new TreeMap<>();
        Map<LocalDate, Long> revenueByDay = new TreeMap<>();
        for (PaymentEntity p : uniqPay.values()) {
            if (p.getPaidAt() == null || p.getOrder() == null) {
                continue;
            }
            LocalDate d = p.getPaidAt().toLocalDate();
            long lineSum = sumPartnerLinesOnOrder(p.getOrder(), partnerId, productNoFilter);
            if (lineSum <= 0) {
                continue;
            }
            revenueByDay.merge(d, lineSum, Long::sum);
            ordersByDay.computeIfAbsent(d, k -> new java.util.HashSet<>()).add(p.getOrder().getOrderNo());
        }
        List<PartnerTodayOperationsDto.DailyPaidTrendPointDto> out = new ArrayList<>();
        LocalDate c = today.minusDays(trendDays - 1L);
        while (!c.isAfter(today)) {
            Set<Long> orders = ordersByDay.getOrDefault(c, Collections.emptySet());
            long rev = revenueByDay.getOrDefault(c, 0L);
            out.add(PartnerTodayOperationsDto.DailyPaidTrendPointDto.builder()
                    .date(c)
                    .paidOrderCount(orders.size())
                    .paidRevenueKrw(rev)
                    .build());
            c = c.plusDays(1);
        }
        return out;
    }

    private static long sumPartnerLinesOnOrder(OrderEntity order, Long partnerId, Long productNoFilter) {
        return order.getOrderItems().stream()
                .filter(oi -> !Boolean.TRUE.equals(oi.getIsCancelled()))
                .filter(oi -> partnerOwnsOrderItem(oi, partnerId))
                .filter(oi -> productNoFilter == null
                        || (oi.getProduct() != null && productNoFilter.equals(oi.getProduct().getProductNo())))
                .mapToLong(OrderItemEntity::getItemTotalPrice)
                .sum();
    }

    private static boolean partnerOwnsOrderItem(OrderItemEntity oi, Long partnerId) {
        if (oi.getOption() != null && oi.getOption().getPartner() != null) {
            return partnerId.equals(oi.getOption().getPartner().getPartnerId());
        }
        return oi.getProduct() != null && oi.getProduct().getPartner() != null
                && partnerId.equals(oi.getProduct().getPartner().getPartnerId());
    }
    
    /**
     * 현재 로그인한 파트너의 정보를 조회합니다.
     * 
     * @param session 현재 세션
     * @return 파트너 정보
     * @throws BusinessException 파트너가 로그인하지 않았거나 파트너 정보를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PartnerApplicationResponseDto getMyPartnerInfo(HttpSession session) {
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        return PartnerApplicationResponseDto.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerContact(partner.getPartnerContact())
                .partnerBankAccount(partner.getPartnerBankAccount())
                .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                .representativeBrandCode(partner.getRepresentativeBrandCode())
                .partnerStatus(partner.getPartnerStatus())
                .partnerApprovedAt(partner.getPartnerApprovedAt())
                .email(partner.getEmail())
                .deactivationRequestReason(partner.getDeactivationRequestReason())
                .deactivationRequestedAt(partner.getDeactivationRequestedAt())
                .reactivationRequestReason(partner.getReactivationRequestReason())
                .reactivationRequestedAt(partner.getReactivationRequestedAt())
                .build();
    }

    @Transactional
    public PartnerProfileResponseDto getMyProfile(HttpSession session) {
        Long partnerId = getCurrentPartnerId(session);
        PartnerProfileEntity profile = partnerProfileRepository.findByPartner_PartnerId(partnerId)
                .orElseGet(() -> {
                    PartnerEntity partner = partnerRepository.findById(partnerId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
                    return partnerProfileRepository.save(PartnerProfileEntity.builder()
                            .partner(partner)
                            .build());
                });
        return toProfileDto(profile);
    }

    @Transactional
    public PartnerProfileResponseDto updateMyProfile(HttpSession session, PartnerProfileUpdateRequestDto requestDto) {
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        PartnerProfileEntity existing = partnerProfileRepository.findByPartner_PartnerId(partnerId)
                .orElse(null);

        PartnerProfileEntity updated = PartnerProfileEntity.builder()
                .profileId(existing != null ? existing.getProfileId() : null)
                .partner(partner)
                .contactPersonName(trimToNull(requestDto.getContactPersonName()))
                .customerServicePhone(trimToNull(requestDto.getCustomerServicePhone()))
                .profileImageUrl(trimToNull(requestDto.getProfileImageUrl()))
                .introduction(trimToNull(requestDto.getIntroduction()))
                .updatedAt(LocalDateTime.now())
                .build();

        updated = partnerProfileRepository.save(updated);
        return toProfileDto(updated);
    }

    @Transactional
    public PartnerChangeRequestResponseDto createChangeRequest(HttpSession session, PartnerChangeRequestCreateDto requestDto) {
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        PartnerChangeRequestEntity req = PartnerChangeRequestEntity.builder()
                .partner(partner)
                .requestedPartnerName(trimToNull(requestDto.getRequestedPartnerName()))
                .requestedPartnerBankAccount(trimToNull(requestDto.getRequestedPartnerBankAccount()))
                .requestedBusinessRegistrationNumber(trimToNull(requestDto.getRequestedBusinessRegistrationNumber()))
                .requestedRepresentativeBrandCode(normalizeUpper(requestDto.getRequestedRepresentativeBrandCode()))
                .status(PartnerChangeRequestStatus.PENDING)
                .requestReason(trimToNull(requestDto.getRequestReason()))
                .build();
        req = partnerChangeRequestRepository.save(req);
        return toChangeRequestDto(req);
    }

    @Transactional(readOnly = true)
    public List<PartnerChangeRequestResponseDto> getMyChangeRequests(HttpSession session) {
        Long partnerId = getCurrentPartnerId(session);
        return partnerChangeRequestRepository.findByPartner_PartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .map(this::toChangeRequestDto)
                .collect(Collectors.toList());
    }

    /**
     * 세션에서 현재 파트너 ID 가져오기
     */
    private Long getCurrentPartnerId(HttpSession session) {
        // 통합 로그인 세션에서 파트너 ID 가져오기
        var authResponse = authService.getCurrentUser(session);
        
        // 세션 정보 로깅 (디버깅용)
        if (authResponse.getRole() != com.swimshop.swim_mall.common.enums.AccountRole.PARTNER) {
            // role이 PARTNER가 아닌 경우, 세션 정보를 로깅하고 에러 발생
            log.warn("파트너 권한이 필요합니다. 현재 role: {}, subjectId: {}, email: {}", 
                authResponse.getRole(), authResponse.getSubjectId(), authResponse.getEmail());
            throw new BusinessException(ErrorCode.FORBIDDEN, 
                "파트너 권한이 필요합니다. 현재 role: " + authResponse.getRole());
        }
        
        Long partnerId = authResponse.getSubjectId();
        
        // 파트너 존재 확인
        partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        return partnerId;
    }

    private PartnerProfileResponseDto toProfileDto(PartnerProfileEntity profile) {
        return PartnerProfileResponseDto.builder()
                .partnerId(profile.getPartner().getPartnerId())
                .contactPersonName(profile.getContactPersonName())
                .customerServicePhone(profile.getCustomerServicePhone())
                .profileImageUrl(profile.getProfileImageUrl())
                .introduction(profile.getIntroduction())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private PartnerChangeRequestResponseDto toChangeRequestDto(PartnerChangeRequestEntity e) {
        return PartnerChangeRequestResponseDto.builder()
                .requestId(e.getRequestId())
                .partnerId(e.getPartner().getPartnerId())
                .partnerName(e.getPartner().getPartnerName())
                .requestedPartnerName(e.getRequestedPartnerName())
                .requestedPartnerBankAccount(e.getRequestedPartnerBankAccount())
                .requestedBusinessRegistrationNumber(e.getRequestedBusinessRegistrationNumber())
                .requestedRepresentativeBrandCode(e.getRequestedRepresentativeBrandCode())
                .status(e.getStatus())
                .requestReason(e.getRequestReason())
                .rejectReason(e.getRejectReason())
                .processedAdminId(e.getProcessedByAdmin() != null ? e.getProcessedByAdmin().getAdminId() : null)
                .createdAt(e.getCreatedAt())
                .processedAt(e.getProcessedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpper(String value) {
        String v = trimToNull(value);
        return v == null ? null : v.toUpperCase();
    }

    private String resolvePartnerBrandName(PartnerEntity partner) {
        return trimToNull(partner.getRepresentativeBrandCode());
    }

    private String normalizeOptionColorCode(String rawColorCode) {
        String colorCode = normalizeUpper(rawColorCode);
        if (colorCode == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "옵션 컬러는 필수입니다.");
        }
        return colorRepository.findById(colorCode)
                .filter(color -> Boolean.TRUE.equals(color.getIsActive()))
                .map(color -> color.getCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않거나 비활성화된 컬러 코드입니다: " + colorCode));
    }

    private boolean ownsAnyRequestedOption(Long partnerId, Long productNo, List<ProductUpdateRequestDto.OptionUpdateRequestDto> requestedOptions) {
        if (requestedOptions == null || requestedOptions.isEmpty()) return false;
        return requestedOptions.stream()
                .map(ProductUpdateRequestDto.OptionUpdateRequestDto::getOptionNo)
                .filter(optionNo -> optionNo != null)
                .anyMatch(optionNo -> optionRepository.findById(optionNo)
                        .filter(option -> option.getProduct() != null && productNo.equals(option.getProduct().getProductNo()))
                        .filter(option -> option.getPartner() != null && partnerId.equals(option.getPartner().getPartnerId()))
                        .isPresent());
    }

    private String generateSku(LocalDateTime createdAt, Long productNo) {
        String ym = createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        return String.format("SWM-%s-%06d", ym, productNo);
    }

    private String normalizeSizeGuideJson(String rawJson, ProductType productType, ProductSubType productSubType) {
        if (rawJson == null || rawJson.isBlank()) return null;
        String expectedTemplate = resolveTemplateKey(productType, productSubType);
        if (expectedTemplate == null) return null;

        Map<String, Object> root;
        try {
            root = OBJECT_MAPPER.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈표 JSON 형식이 올바르지 않습니다.");
        }

        String templateKey = root.get("templateKey") instanceof String ? (String) root.get("templateKey") : null;
        if (templateKey == null || !expectedTemplate.equals(templateKey)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "상품 유형에 맞는 sizeGuide 템플릿이 아닙니다.");
        }

        Object rowsObj = root.get("rows");
        if (!(rowsObj instanceof List<?> rows) || rows.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈표 rows는 1개 이상이어야 합니다.");
        }

        Set<String> allowedMetrics = allowedMetricsByTemplate(templateKey);
        Set<String> seenLabels = new LinkedHashSet<>();
        List<Map<String, Object>> normalizedRows = new ArrayList<>();

        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map<?, ?> row)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈표 row 형식이 올바르지 않습니다.");
            }
            String sizeLabel = row.get("sizeLabel") instanceof String ? ((String) row.get("sizeLabel")).trim() : null;
            if (sizeLabel == null || sizeLabel.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈표의 sizeLabel은 필수입니다.");
            }
            if (!seenLabels.add(sizeLabel)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈표에 중복 sizeLabel이 있습니다.");
            }

            Map<String, Object> normalizedRow = new LinkedHashMap<>();
            normalizedRow.put("sizeLabel", sizeLabel);
            for (String metric : allowedMetrics) {
                normalizedRow.put(metric, normalizePositiveNumber(row.get(metric)));
            }
            normalizedRows.add(normalizedRow);
        }

        Map<String, Object> normalizedRoot = new LinkedHashMap<>();
        normalizedRoot.put("templateKey", templateKey);
        normalizedRoot.put("rows", normalizedRows);
        try {
            return OBJECT_MAPPER.writeValueAsString(normalizedRoot);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "사이즈표 JSON 직렬화에 실패했습니다.");
        }
    }

    private Double normalizePositiveNumber(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) {
            double v = n.doubleValue();
            if (v <= 0) throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈 수치는 0보다 커야 합니다.");
            return v;
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) return null;
            try {
                double v = Double.parseDouble(trimmed);
                if (v <= 0) throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈 수치는 0보다 커야 합니다.");
                return v;
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈 수치 형식이 올바르지 않습니다.");
            }
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "사이즈 수치 형식이 올바르지 않습니다.");
    }

    private Set<String> allowedMetricsByTemplate(String templateKey) {
        return switch (templateKey) {
            case "SWIMSUIT_MEN_PANTS" -> new LinkedHashSet<>(List.of("waistCm", "hipCm"));
            case "SWIMSUIT_WOMEN_ONEPIECE" -> new LinkedHashSet<>(List.of("chestCm", "waistCm", "hipCm", "torsoCm"));
            case "SWIMSUIT_WOMEN_BIKINI" -> new LinkedHashSet<>(List.of("chestCm", "waistCm", "hipCm"));
            case "FINS_SIZE" -> new LinkedHashSet<>(List.of("footLengthCm"));
            default -> Collections.emptySet();
        };
    }

    private String resolveTemplateKey(ProductType productType, ProductSubType productSubType) {
        if (productType == ProductType.FINS) return "FINS_SIZE";
        if (productType == ProductType.SWIMSUIT_MEN) return "SWIMSUIT_MEN_PANTS";
        if (productType == ProductType.SWIMSUIT_WOMEN) {
            if (productSubType == ProductSubType.BIKINI) return "SWIMSUIT_WOMEN_BIKINI";
            return "SWIMSUIT_WOMEN_ONEPIECE";
        }
        return null;
    }
    
    /**
     * 파트너 상품 등록 (상품 + 옵션들)
     * 
     * @param session 현재 세션
     * @param requestDto 상품 등록 요청 DTO
     * @return 등록된 상품 정보 (옵션 포함)
     */
    @Transactional
    public ProductWithOptionsDto createProduct(HttpSession session, ProductCreateRequestDto requestDto) {
        // 1. 현재 파트너 조회
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 2. 파트너 상태 확인 (INACTIVE 상태면 상품 등록 불가)
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "비활성화된 파트너는 상품을 등록할 수 없습니다. 재활성화 신청을 해주세요.");
        }
        
        // 2. ProductType enum 변환
        ProductType productType;
        try {
            productType = ProductType.valueOf(requestDto.getProductType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR); // TODO: 적절한 에러 코드로 변경
        }
        
        // 3. ProductSubType enum 변환 (null이면 NONE으로 설정)
        ProductSubType productSubType = ProductSubType.NONE; // 기본값: NONE
        if (requestDto.getProductSubType() != null && !requestDto.getProductSubType().isEmpty()) {
            try {
                ProductSubType requestedSubType = ProductSubType.valueOf(requestDto.getProductSubType());
                // NONE이 아닌 경우에만 설정
                if (requestedSubType != ProductSubType.NONE) {
                    productSubType = requestedSubType;
                }
            } catch (IllegalArgumentException e) {
                // 잘못된 subType은 NONE으로 처리
                productSubType = ProductSubType.NONE;
            }
        }
        
        // 4. 상품 생성 (PENDING 상태로 생성 - 관리자 승인 필요)
        LocalDateTime now = LocalDateTime.now();
        String normalizedSizeGuideJson = normalizeSizeGuideJson(
                requestDto.getSizeGuideJson(), productType, productSubType
        );
        ProductEntity product = ProductEntity.builder()
                .productName(requestDto.getProductName())
                .productType(productType)
                .productSubType(productSubType)
                .productPrice(requestDto.getProductPrice())
                .productDescription(requestDto.getProductDescription())
                .productImageUrl(requestDto.getProductImageUrl())
                .sku(null)
                .brandName(resolvePartnerBrandName(partner))
                .materialInfo(requestDto.getMaterialInfo())
                .originCountry(requestDto.getOriginCountry())
                .manufactureCountry(requestDto.getManufactureCountry())
                .careInstructions(requestDto.getCareInstructions())
                .sizeGuideText(requestDto.getSizeGuideText())
                .sizeGuideJson(normalizedSizeGuideJson)
                .productCreatedAt(now)
                .productUpdatedAt(null)
                .productActiveStatus(ActiveStatus.PENDING) // 승인 대기 상태로 생성
                .partner(partner) // 파트너 정보 설정
                .build();
        
        product = productRepository.save(product);
        product.setSku(generateSku(now, product.getProductNo()));
        product = productRepository.save(product);
        
        // 5. 옵션들 생성 (PENDING 상태로 생성 - 관리자 승인 필요)
        List<OptionDto> optionDtos = new ArrayList<>();
        for (ProductCreateRequestDto.OptionCreateRequestDto optionRequest : requestDto.getOptions()) {
            String normalizedColorCode = normalizeOptionColorCode(optionRequest.getColor());
            OptionEntity option = OptionEntity.builder()
                    .product(product)
                    .partner(partner)
                    .color(normalizedColorCode)
                    .size(optionRequest.getSize())
                    .optionAddPrice(optionRequest.getOptionAddPrice())
                    .optionStatus(ActiveStatus.PENDING) // 옵션 상태를 PENDING으로 설정
                    .build();
            
            OptionEntity savedOption = optionRepository.save(option);
            optionDtos.add(new OptionDto(
                    savedOption.getOptionNo(),
                    savedOption.getColor(),
                    savedOption.getSize(),
                    savedOption.getOptionAddPrice()
            ));
        }
        
        // 6. 응답 DTO 생성
        return new ProductWithOptionsDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                product.getProductDescription(),
                product.getProductImageUrl(),
                product.getSku(),
                product.getBrandName(),
                product.getMaterialInfo(),
                product.getOriginCountry(),
                product.getManufactureCountry(),
                product.getCareInstructions(),
                product.getSizeGuideText(),
                product.getSizeGuideJson(),
                product.getProductCreatedAt(),
                product.getProductUpdatedAt(),
                product.getProductActiveStatus().name(),
                product.getRejectionReason(), // 거절 사유 추가
                optionDtos
        );
    }
    
    /**
     * 파트너 상품 수정 (상품 + 옵션들)
     * 
     * @param session 현재 세션
     * @param productNo 수정할 상품 번호
     * @param requestDto 상품 수정 요청 DTO
     * @return 수정된 상품 정보 (옵션 포함)
     */
    @Transactional
    public ProductWithOptionsDto updateProduct(HttpSession session, Long productNo, ProductUpdateRequestDto requestDto) {
        // 1. 현재 파트너 조회
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 2. 파트너 상태 확인 (INACTIVE 상태면 상품 수정 불가)
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "비활성화된 파트너는 상품을 수정할 수 없습니다. 재활성화 신청을 해주세요.");
        }
        
        // 3. 수정할 상품 조회 및 권한 확인 (해당 파트너의 상품인지 확인)
        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        // 3-1. ProductEntity의 partner 필드로 소유권 확인 (옵션이 없는 단일 상품의 경우)
        boolean isOwnerByProduct = product.getPartner() != null 
                && product.getPartner().getPartnerId().equals(partnerId);
        
        // 3-2. 옵션으로 상품 소유권 확인 (옵션이 있는 상품의 경우)
        // 모든 상태의 옵션 조회 (PENDING_UPDATE 상태 옵션도 포함)
        List<OptionEntity> existingOptions = optionRepository.findByProduct_ProductNoAllStatus(productNo);
        boolean isOwnerByOption = existingOptions.stream()
                .anyMatch(opt -> opt.getPartner() != null 
                        && opt.getPartner().getPartnerId().equals(partnerId));
        
        // 3-3. 둘 중 하나라도 소유권이 있으면 통과
        // 레거시 데이터에서 상품 파트너 연결이 누락된 경우를 위해, 요청 optionNo 기준 소유권도 보조 확인
        boolean isOwnerByRequestedOption = ownsAnyRequestedOption(partnerId, productNo, requestDto.getOptions());
        boolean isOwner = isOwnerByProduct || isOwnerByOption || isOwnerByRequestedOption;
        
        if (!isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, 
                "본인의 상품만 수정할 수 있습니다."); // 본인의 상품이 아님
        }
        
        // 4. 상품 상태에 따른 수정 로직 분기
        ActiveStatus currentStatus = product.getProductActiveStatus();
        
        // 4-1. REJECTED 상태: 재신청 (PENDING으로 변경)
        // 4-2. ACTIVE 상태: 재심사 필수 항목 변경 시 PENDING_UPDATE, 일반 항목만 변경 시 즉시 반영
        // 4-3. PENDING_UPDATE 상태: 수정 가능 (재심사 필수 항목 변경 시 다시 PENDING_UPDATE)
        // 4-4. INACTIVE 상태: 수정 가능 (재활성화를 위한 수정)
        // 4-5. PENDING 상태: 수정 가능 (수정 후에도 PENDING 상태 유지)
        
        // PENDING, REJECTED, ACTIVE, PENDING_UPDATE, INACTIVE 상태에서 수정 가능
        if (currentStatus != ActiveStatus.REJECTED 
                && currentStatus != ActiveStatus.ACTIVE 
                && currentStatus != ActiveStatus.PENDING_UPDATE
                && currentStatus != ActiveStatus.INACTIVE
                && currentStatus != ActiveStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "수정 가능한 상태가 아닙니다. 현재 상태: " + currentStatus.getLabel());
        }
        
        // 5. ProductType enum 변환
        ProductType productType;
        try {
            productType = ProductType.valueOf(requestDto.getProductType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        
        // 6. ProductSubType enum 변환
        ProductSubType productSubType = ProductSubType.NONE;
        if (requestDto.getProductSubType() != null && !requestDto.getProductSubType().isEmpty()) {
            try {
                ProductSubType requestedSubType = ProductSubType.valueOf(requestDto.getProductSubType());
                if (requestedSubType != ProductSubType.NONE) {
                    productSubType = requestedSubType;
                }
            } catch (IllegalArgumentException e) {
                productSubType = ProductSubType.NONE;
            }
        }
        
        // 7. 재심사 필수 항목 변경 여부 확인 (상품명, 카테고리, 소분류, 가격)
        boolean hasCriticalFieldsChanged = product.hasCriticalFieldsChanged(
            requestDto.getProductName(),
            productType,
            productSubType,
            requestDto.getProductPrice()
        );
        String normalizedSizeGuideJson = normalizeSizeGuideJson(
                requestDto.getSizeGuideJson(), productType, productSubType
        );
        
        // 8. 상태 결정 및 상품 정보 업데이트
        // 옵션 변경 여부는 나중에 확인하므로, 일단 상품 변경 여부만으로 상태 결정
        // 옵션 변경이 있으면 나중에 상품 상태도 업데이트
        ActiveStatus newStatus;
        if (currentStatus == ActiveStatus.REJECTED) {
            // REJECTED → PENDING (재신청)
            newStatus = ActiveStatus.PENDING;
            product.resubmit(); // 재신청 (거절 사유는 유지)
        } else if (currentStatus == ActiveStatus.PENDING) {
            // PENDING 상태: 수정 가능하지만 상태는 PENDING 유지 (관리자 승인 대기 중)
            newStatus = ActiveStatus.PENDING;
        } else if (currentStatus == ActiveStatus.INACTIVE) {
            // INACTIVE 상태: 수정 가능
            // 거절 사유가 있으면 재신청 필요, 없으면 즉시 재활성화 가능
            if (product.getRejectionReason() != null && !product.getRejectionReason().trim().isEmpty()) {
                // 거절 사유가 있는 경우: 재심사 필수 항목 변경 여부에 따라 결정
                if (hasCriticalFieldsChanged) {
                    newStatus = ActiveStatus.PENDING_UPDATE;
                } else {
                    // 일반 항목만 변경 시에도 재신청 필요 (거절 사유가 있으므로)
                    newStatus = ActiveStatus.PENDING;
                }
            } else {
                // 거절 사유가 없는 경우: 재심사 필수 항목 변경 여부에 따라 결정
                if (hasCriticalFieldsChanged) {
                    newStatus = ActiveStatus.PENDING_UPDATE;
                } else {
                    // 일반 항목만 변경 → 즉시 재활성화 가능 (INACTIVE 유지 또는 ACTIVE로 변경)
                    // 옵션 변경 여부는 나중에 확인하여 상태 업데이트
                    newStatus = ActiveStatus.INACTIVE;
                }
            }
        } else if (currentStatus == ActiveStatus.ACTIVE || currentStatus == ActiveStatus.PENDING_UPDATE) {
            // ACTIVE 또는 PENDING_UPDATE 상태
            if (hasCriticalFieldsChanged) {
                // 재심사 필수 항목 변경 → PENDING_UPDATE
                newStatus = ActiveStatus.PENDING_UPDATE;
            } else {
                // 일반 항목만 변경 → 즉시 반영 (상태 유지)
                // 옵션 변경 여부는 나중에 확인하여 상태 업데이트
                newStatus = currentStatus;
            }
        } else {
            newStatus = currentStatus; // 기본값 (실제로는 위에서 예외 발생)
        }
        
        // 9. 상품 정보 업데이트
        product.update(
            requestDto.getProductName(),
            productType,
            productSubType,
            requestDto.getProductPrice(),
            requestDto.getProductDescription(),
            requestDto.getProductImageUrl(),
            product.getSku(),
            resolvePartnerBrandName(partner),
            requestDto.getMaterialInfo(),
            requestDto.getOriginCountry(),
            requestDto.getManufactureCountry(),
            requestDto.getCareInstructions(),
            requestDto.getSizeGuideText(),
            normalizedSizeGuideJson,
            newStatus
        );
        
        // PENDING_UPDATE 상태로 명시적으로 설정 (재심사 필수 항목 변경 시)
        if (hasCriticalFieldsChanged && (currentStatus == ActiveStatus.ACTIVE || currentStatus == ActiveStatus.PENDING_UPDATE)) {
            product.setPendingUpdate();
        }
        
        product.setPartner(product.getPartner() != null ? product.getPartner() : partner); // 파트너 정보 유지
        
        ProductEntity updatedProduct = productRepository.save(product);
        
        // 7. 옵션 업데이트/생성/삭제 처리 (optionNo 기준으로 구분)
        List<OptionEntity> partnerOptions = existingOptions.stream()
                .filter(opt -> opt.getPartner().getPartnerId().equals(partnerId))
                .collect(Collectors.toList());
        
        // 요청에 포함된 optionNo 목록
        List<Long> requestedOptionNos = requestDto.getOptions().stream()
                .map(ProductUpdateRequestDto.OptionUpdateRequestDto::getOptionNo)
                .filter(no -> no != null) // null이 아닌 것만
                .collect(Collectors.toList());
        
        // 옵션 변경 여부 확인 (옵션 추가/수정/삭제)
        boolean hasOptionChanges = false;
        
        // 옵션 삭제 확인
        boolean hasOptionDeletion = partnerOptions.stream()
                .anyMatch(opt -> !requestedOptionNos.contains(opt.getOptionNo()));
        if (hasOptionDeletion) {
            hasOptionChanges = true;
        }
        
        // 요청에 없는 기존 옵션들은 소프트 삭제
        for (OptionEntity existingOption : partnerOptions) {
            if (!requestedOptionNos.contains(existingOption.getOptionNo())) {
                // 요청에 포함되지 않은 기존 옵션 → 소프트 삭제
                existingOption.deactivate();
                optionRepository.save(existingOption);
            }
        }
        
        // 10. 옵션 업데이트/생성 및 변경 여부 확인
        // 옵션 상태 결정: 상품 상태 + 옵션 변경 여부에 따라 결정
        List<OptionDto> optionDtos = new ArrayList<>();
        for (ProductUpdateRequestDto.OptionUpdateRequestDto optionRequest : requestDto.getOptions()) {
            String normalizedColorCode = normalizeOptionColorCode(optionRequest.getColor());
            OptionEntity option;
            ActiveStatus finalOptionStatus;
            
            if (optionRequest.getOptionNo() != null) {
                // 기존 옵션 업데이트 (optionNo가 있으면)
                option = optionRepository.findById(optionRequest.getOptionNo())
                        .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
                
                // 소유권 확인
                if (!option.getPartner().getPartnerId().equals(partnerId)) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
                
                // 옵션 변경 여부 확인 (색상, 사이즈, 추가 가격)
                boolean optionChanged = option.hasCriticalFieldsChanged(
                    normalizedColorCode,
                    optionRequest.getSize(),
                    optionRequest.getOptionAddPrice()
                );
                
                if (optionChanged) {
                    hasOptionChanges = true;
                }
                
                // 옵션 상태 결정
                if (currentStatus == ActiveStatus.REJECTED) {
                    // REJECTED 상태: 재신청 → PENDING
                    finalOptionStatus = ActiveStatus.PENDING;
                } else if (currentStatus == ActiveStatus.PENDING) {
                    // PENDING 상태: 옵션 수정 시에도 PENDING 유지
                    finalOptionStatus = ActiveStatus.PENDING;
                } else if (currentStatus == ActiveStatus.INACTIVE) {
                    // INACTIVE 상태: 거절 사유가 있으면 재신청 필요, 없으면 INACTIVE 유지 또는 재활성화 가능
                    if (product.getRejectionReason() != null && !product.getRejectionReason().trim().isEmpty()) {
                        // 거절 사유가 있는 경우: 재신청 필요
                        if (hasCriticalFieldsChanged || optionChanged) {
                            finalOptionStatus = ActiveStatus.PENDING_UPDATE;
                        } else {
                            finalOptionStatus = ActiveStatus.PENDING;
                        }
                    } else {
                        // 거절 사유가 없는 경우: 재심사 필수 항목 변경 여부에 따라 결정
                        if (hasCriticalFieldsChanged || optionChanged) {
                            finalOptionStatus = ActiveStatus.PENDING_UPDATE;
                        } else {
                            // 일반 항목만 변경 → INACTIVE 유지 (즉시 재활성화 가능)
                            finalOptionStatus = ActiveStatus.INACTIVE;
                        }
                    }
                } else if (currentStatus == ActiveStatus.ACTIVE || currentStatus == ActiveStatus.PENDING_UPDATE) {
                    // ACTIVE 또는 PENDING_UPDATE 상태
                    if (hasCriticalFieldsChanged || optionChanged) {
                        // 상품 재심사 필수 항목 변경 또는 옵션 변경 → PENDING_UPDATE (관리자 승인 필요)
                        finalOptionStatus = ActiveStatus.PENDING_UPDATE;
                    } else {
                        // 상품 일반 항목만 변경 + 옵션 변경 없음 → ACTIVE 유지 (즉시 반영)
                        finalOptionStatus = ActiveStatus.ACTIVE;
                    }
                } else {
                    finalOptionStatus = ActiveStatus.PENDING; // 기본값
                }
                
                // 옵션 정보 업데이트
                option.update(
                        normalizedColorCode,
                        optionRequest.getSize(),
                        optionRequest.getOptionAddPrice(),
                        finalOptionStatus
                );
                
                // PENDING_UPDATE 상태로 변경 시 원본 데이터 저장
                if (finalOptionStatus == ActiveStatus.PENDING_UPDATE) {
                    option.setPendingUpdate();
                }
            } else {
                // 새 옵션 생성 (optionNo가 null이면)
                // 옵션 추가는 항상 관리자 승인 필요
                hasOptionChanges = true; // 옵션 추가도 변경으로 간주
                
                if (currentStatus == ActiveStatus.REJECTED) {
                    // REJECTED 상태: 재신청 → PENDING
                    finalOptionStatus = ActiveStatus.PENDING;
                } else if (currentStatus == ActiveStatus.PENDING) {
                    // PENDING 상태: 새 옵션도 PENDING (관리자 승인 필요)
                    finalOptionStatus = ActiveStatus.PENDING;
                } else if (currentStatus == ActiveStatus.INACTIVE) {
                    // INACTIVE 상태: 거절 사유가 있으면 재신청 필요, 없으면 PENDING (관리자 승인 필요)
                    if (product.getRejectionReason() != null && !product.getRejectionReason().trim().isEmpty()) {
                        finalOptionStatus = ActiveStatus.PENDING;
                    } else {
                        finalOptionStatus = ActiveStatus.PENDING; // 새 옵션은 항상 관리자 승인 필요
                    }
                } else if (currentStatus == ActiveStatus.ACTIVE || currentStatus == ActiveStatus.PENDING_UPDATE) {
                    // ACTIVE 또는 PENDING_UPDATE 상태: 새 옵션은 PENDING (관리자 승인 필요)
                    finalOptionStatus = ActiveStatus.PENDING;
                } else {
                    finalOptionStatus = ActiveStatus.PENDING; // 기본값
                }
                
                option = OptionEntity.builder()
                        .product(updatedProduct)
                        .partner(partner)
                        .color(normalizedColorCode)
                        .size(optionRequest.getSize())
                        .optionAddPrice(optionRequest.getOptionAddPrice())
                        .optionStatus(finalOptionStatus)
                        .build();
            }
            
            OptionEntity savedOption = optionRepository.save(option);
            optionDtos.add(new OptionDto(
                    savedOption.getOptionNo(),
                    savedOption.getColor(),
                    savedOption.getSize(),
                    savedOption.getOptionAddPrice()
            ));
        }
        
        // 11. 옵션 변경이 있으면 상품 상태도 업데이트
        if (hasOptionChanges) {
            if (currentStatus == ActiveStatus.ACTIVE || currentStatus == ActiveStatus.PENDING_UPDATE) {
                // ACTIVE 또는 PENDING_UPDATE 상태: 옵션 변경 시 PENDING_UPDATE로 변경
                if (!hasCriticalFieldsChanged) {
                    // 옵션만 변경된 경우: 상품도 PENDING_UPDATE로 변경
                    updatedProduct.setPendingUpdate();
                    productRepository.save(updatedProduct);
                }
                // 상품 재심사 필수 항목도 변경된 경우는 이미 PENDING_UPDATE 상태이므로 추가 처리 불필요
            } else if (currentStatus == ActiveStatus.INACTIVE) {
                // INACTIVE 상태: 옵션 변경 시 상태 업데이트
                if (product.getRejectionReason() != null && !product.getRejectionReason().trim().isEmpty()) {
                    // 거절 사유가 있는 경우: 재신청 필요
                    if (!hasCriticalFieldsChanged) {
                        updatedProduct.setPendingUpdate();
                        productRepository.save(updatedProduct);
                    }
                } else {
                    // 거절 사유가 없는 경우: 재심사 필수 항목 변경 여부에 따라 결정
                    if (!hasCriticalFieldsChanged) {
                        // 옵션만 변경된 경우: PENDING_UPDATE로 변경
                        updatedProduct.setPendingUpdate();
                        productRepository.save(updatedProduct);
                    }
                }
            }
            // PENDING, REJECTED 상태는 이미 적절한 상태로 설정되어 있으므로 추가 처리 불필요
        }
        
        // 12. 응답 DTO 생성
        return new ProductWithOptionsDto(
                updatedProduct.getProductNo(),
                updatedProduct.getProductName(),
                updatedProduct.getProductType().name(),
                updatedProduct.getProductSubType() != null ? updatedProduct.getProductSubType().name() : null,
                updatedProduct.getProductPrice(),
                updatedProduct.getProductDescription(),
                updatedProduct.getProductImageUrl(),
                updatedProduct.getSku(),
                updatedProduct.getBrandName(),
                updatedProduct.getMaterialInfo(),
                updatedProduct.getOriginCountry(),
                updatedProduct.getManufactureCountry(),
                updatedProduct.getCareInstructions(),
                updatedProduct.getSizeGuideText(),
                updatedProduct.getSizeGuideJson(),
                updatedProduct.getProductCreatedAt(),
                updatedProduct.getProductUpdatedAt(),
                updatedProduct.getProductActiveStatus().name(),
                updatedProduct.getRejectionReason(), // 거절 사유 추가 (재신청 시 null)
                optionDtos
        );
    }

    /**
     * 파트너 상품 비활성화 (상태를 INACTIVE로 변경)
     * 고객 목록에는 표시되지 않지만, 파트너는 목록에서 확인하고 다시 활성화할 수 있음
     */
    @Transactional
    public void deleteProduct(HttpSession session, Long productNo) {
        // 1. 현재 파트너 조회 및 상품 소유권 확인
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 2. 파트너 상태 확인 (INACTIVE 상태면 상품 삭제 불가)
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "비활성화된 파트너는 상품을 삭제할 수 없습니다. 재활성화 신청을 해주세요.");
        }

        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. 상품 상태 확인 (ACTIVE 또는 PENDING 상태인 상품만 삭제 가능)
        // ACTIVE: 비활성화 (INACTIVE)
        // PENDING: 완전 삭제 (데이터베이스에서 삭제)
        ActiveStatus currentStatus = product.getProductActiveStatus();
        if (currentStatus != ActiveStatus.ACTIVE && currentStatus != ActiveStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "활성 상태 또는 승인 대기 중인 상품만 삭제할 수 있습니다. 현재 상태: " + currentStatus.getLabel());
        }

        // 4. 상품 소유권 확인
        // ProductEntity의 partner 필드로 소유권 확인
        boolean isOwnerByProduct = product.getPartner() != null 
                && product.getPartner().getPartnerId().equals(partnerId);
        
        // 옵션으로 상품 소유권 확인
        List<OptionEntity> existingOptions = optionRepository.findByProduct_ProductNoAllStatus(productNo);
        boolean isOwnerByOption = existingOptions.stream()
                .anyMatch(opt -> opt.getPartner() != null 
                        && opt.getPartner().getPartnerId().equals(partnerId));
        
        boolean isOwner = isOwnerByProduct || isOwnerByOption;

        if (!isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, 
                "본인의 상품만 비활성화할 수 있습니다."); // 다른 파트너의 상품 비활성화 시도
        }

        // 5. 상품 삭제 처리
        if (currentStatus == ActiveStatus.PENDING) {
            // PENDING 상태: 완전 삭제 (데이터베이스에서 삭제)
            // 해당 상품의 모든 옵션도 삭제
            for (OptionEntity option : existingOptions) {
                if (option.getPartner() != null 
                        && option.getPartner().getPartnerId().equals(partnerId)) {
                    optionRepository.delete(option);
                }
            }
            productRepository.delete(product);
        } else {
            // ACTIVE 상태: 비활성화 (INACTIVE로 변경)
            product.deactivate();
            productRepository.save(product);
            
            // 6. 해당 상품의 모든 옵션도 비활성화 (ACTIVE 상태인 옵션만)
            for (OptionEntity option : existingOptions) {
                if (option.getOptionStatus() == ActiveStatus.ACTIVE 
                        && option.getPartner() != null 
                        && option.getPartner().getPartnerId().equals(partnerId)) {
                    option.deactivate();
                    optionRepository.save(option);
                }
            }
        }
    }

    /**
     * 파트너 상품 재활성화 (INACTIVE → ACTIVE)
     * 파트너가 직접 비활성화한 상품만 즉시 재활성화 가능 (거절 사유가 없는 경우)
     * 거절 사유가 있는 경우는 수정 후 재신청 필요
     * 
     * @param session 현재 세션
     * @param productNo 상품 번호
     * @return 재활성화된 상품 정보
     */
    @Transactional
    public ProductWithOptionsDto reactivateProduct(HttpSession session, Long productNo) {
        // 1. 현재 파트너 조회 및 상품 소유권 확인
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 2. 파트너 상태 확인 (INACTIVE 상태면 상품 재활성화 불가)
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "비활성화된 파트너는 상품을 재활성화할 수 없습니다. 재활성화 신청을 해주세요.");
        }

        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. 상품 상태 확인 (INACTIVE 상태인 상품만 재활성화 가능)
        if (product.getProductActiveStatus() != ActiveStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "비활성화된 상품만 재활성화할 수 있습니다. 현재 상태: " + product.getProductActiveStatus().getLabel());
        }

        // 4. 거절 사유 확인
        // 거절 사유가 있으면 즉시 재활성화 불가 (수정 후 재신청 필요)
        if (product.getRejectionReason() != null && !product.getRejectionReason().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "거절 사유가 있는 상품은 즉시 재활성화할 수 없습니다. 상품을 수정한 후 재신청해주세요.");
        }

        // 5. 상품 소유권 확인
        boolean isOwnerByProduct = product.getPartner() != null 
                && product.getPartner().getPartnerId().equals(partnerId);
        
        List<OptionEntity> existingOptions = optionRepository.findByProduct_ProductNoAllStatus(productNo);
        boolean isOwnerByOption = existingOptions.stream()
                .anyMatch(opt -> opt.getPartner() != null 
                        && opt.getPartner().getPartnerId().equals(partnerId));
        
        boolean isOwner = isOwnerByProduct || isOwnerByOption;

        if (!isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, 
                "본인의 상품만 재활성화할 수 있습니다.");
        }

        // 6. 재활성화: 상태를 ACTIVE로 변경 (즉시 반영, 관리자 승인 불필요)
        product.activate();
        product = productRepository.save(product);
        
        // 7. 해당 상품의 모든 옵션도 재활성화 (INACTIVE 상태인 옵션만)
        for (OptionEntity option : existingOptions) {
            if (option.getOptionStatus() == ActiveStatus.INACTIVE 
                    && option.getPartner() != null 
                    && option.getPartner().getPartnerId().equals(partnerId)) {
                option.activate();
                optionRepository.save(option);
            }
        }

        // 8. 응답 DTO 생성
        List<OptionDto> optionDtos = new ArrayList<>();
        for (OptionEntity option : existingOptions) {
            if (option.getPartner() != null && option.getPartner().getPartnerId().equals(partnerId)) {
                optionDtos.add(new OptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    option.getOptionAddPrice()
                ));
            }
        }

        return new ProductWithOptionsDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                product.getProductDescription(),
                product.getProductImageUrl(),
                product.getSku(),
                product.getBrandName(),
                product.getMaterialInfo(),
                product.getOriginCountry(),
                product.getManufactureCountry(),
                product.getCareInstructions(),
                product.getSizeGuideText(),
                product.getSizeGuideJson(),
                product.getProductCreatedAt(),
                product.getProductUpdatedAt(),
                product.getProductActiveStatus().name(),
                product.getRejectionReason(),
                optionDtos
        );
    }

    /**
     * 파트너 상품 수정 신청 취소 (PENDING_UPDATE → ACTIVE)
     * 파트너가 수정 신청을 취소하여 원래 상태로 복구
     * 
     * @param session 현재 세션
     * @param productNo 상품 번호
     * @return 취소된 상품 정보
     */
    @Transactional
    public ProductWithOptionsDto cancelProductUpdate(HttpSession session, Long productNo) {
        // 1. 현재 파트너 조회 및 상품 소유권 확인
        Long partnerId = getCurrentPartnerId(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 2. 파트너 상태 확인 (INACTIVE 상태면 수정 신청 취소 불가)
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "비활성화된 파트너는 수정 신청을 취소할 수 없습니다. 재활성화 신청을 해주세요.");
        }

        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. 상품 상태 확인 (PENDING_UPDATE 상태인 상품만 취소 가능)
        if (product.getProductActiveStatus() != ActiveStatus.PENDING_UPDATE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "수정 승인 대기 중인 상품만 취소할 수 있습니다. 현재 상태: " + product.getProductActiveStatus().getLabel());
        }

        // 4. 상품 소유권 확인
        boolean isOwnerByProduct = product.getPartner() != null 
                && product.getPartner().getPartnerId().equals(partnerId);
        
        List<OptionEntity> existingOptions = optionRepository.findByProduct_ProductNoAllStatus(productNo);
        boolean isOwnerByOption = existingOptions.stream()
                .anyMatch(opt -> opt.getPartner() != null 
                        && opt.getPartner().getPartnerId().equals(partnerId));
        
        boolean isOwner = isOwnerByProduct || isOwnerByOption;

        if (!isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, 
                "본인의 상품만 수정 신청을 취소할 수 있습니다.");
        }

        // 5. 수정 신청 취소: 상태를 ACTIVE로 복구
        product.cancelUpdate();
        product = productRepository.save(product);
        
        // 6. 해당 상품의 모든 옵션도 처리
        // PENDING_UPDATE 상태 옵션은 ACTIVE로 복구
        // PENDING 상태 옵션(새로 추가된 옵션)은 삭제 (INACTIVE)
        for (OptionEntity option : existingOptions) {
            if (option.getPartner() != null && option.getPartner().getPartnerId().equals(partnerId)) {
                if (option.getOptionStatus() == ActiveStatus.PENDING_UPDATE) {
                    // 수정 승인 대기 중인 옵션은 원본 데이터로 복구
                    option.cancelUpdate();
                    optionRepository.save(option);
                } else if (option.getOptionStatus() == ActiveStatus.PENDING) {
                    // 새로 추가된 옵션은 삭제 (INACTIVE)
                    option.deactivate();
                    optionRepository.save(option);
                }
                // ACTIVE 상태 옵션은 그대로 유지
            }
        }

        // 7. 응답 DTO 생성
        List<OptionDto> optionDtos = new ArrayList<>();
        for (OptionEntity option : existingOptions) {
            if (option.getPartner() != null && option.getPartner().getPartnerId().equals(partnerId)
                    && option.getOptionStatus() != ActiveStatus.INACTIVE) {
                optionDtos.add(new OptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    option.getOptionAddPrice()
                ));
            }
        }

        return new ProductWithOptionsDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                product.getProductDescription(),
                product.getProductImageUrl(),
                product.getSku(),
                product.getBrandName(),
                product.getMaterialInfo(),
                product.getOriginCountry(),
                product.getManufactureCountry(),
                product.getCareInstructions(),
                product.getSizeGuideText(),
                product.getSizeGuideJson(),
                product.getProductCreatedAt(),
                product.getProductUpdatedAt(),
                product.getProductActiveStatus().name(),
                product.getRejectionReason(),
                optionDtos
        );
    }

    /**
     * 파트너 입점 신청
     * 비회원/회원 모두 신청 가능
     * 신청 시 Account는 생성하지 않고, 승인 시에만 생성됨
     * 
     * @param requestDto 파트너 입점 신청 정보
     * @return 파트너 신청 정보
     */
    @Transactional
    public PartnerApplicationResponseDto applyForPartnership(PartnerApplicationRequestDto requestDto) {
        String normalizedBrandCode = requestDto.getRepresentativeBrandCode() != null
                ? requestDto.getRepresentativeBrandCode().trim().toUpperCase()
                : null;

        // 1. 이메일 중복 확인 (Customer, Account, 또는 Partner)
        // 고객 이메일 체크
        if (customerRepository.existsByCustomerEmail(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다.");
        }
        
        // Account 이메일 체크 (고객, 파트너, 관리자 모두 포함)
        if (accountRepository.existsByEmail(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다.");
        }
        
        // Partner 신청 중인 이메일도 확인 (PENDING 상태)
        boolean existingPendingPartner = partnerRepository.findAll().stream()
                .anyMatch(p -> p.getEmail() != null && p.getEmail().equals(requestDto.getEmail()) 
                        && p.getPartnerStatus() == PartnerStatus.PENDING);
        if (existingPendingPartner) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "이미 신청 중인 이메일입니다.");
        }

        // 2. 첫 번째 관리자 조회 (승인 담당자)
        AdminEntity admin = adminRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자가 존재하지 않습니다."));

        // 3. Partner 생성 (PENDING 상태, Account는 null, password는 null)
        PartnerEntity partner = PartnerEntity.createApplication(
                requestDto.getPartnerName(),
                requestDto.getPartnerContact(),
                requestDto.getPartnerBankAccount(),
                requestDto.getBusinessRegistrationNumber(),
                requestDto.getEmail(),
                normalizedBrandCode,
                requestDto.getBusinessRegistrationFileId(),
                requestDto.getBankAccountFileId(),
                admin
        );
        partner = partnerRepository.save(partner);

        // 4. 이력 기록 (입점 신청)
        PartnerHistoryEntity history = PartnerHistoryEntity.createApplication(partner);
        partnerHistoryRepository.save(history);

        // 5. 응답 DTO 생성
        return PartnerApplicationResponseDto.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerContact(partner.getPartnerContact())
                .partnerBankAccount(partner.getPartnerBankAccount())
                .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                .representativeBrandCode(partner.getRepresentativeBrandCode())
                .businessRegistrationFileId(partner.getBusinessRegistrationFileId())
                .bankAccountFileId(partner.getBankAccountFileId())
                .partnerStatus(partner.getPartnerStatus())
                .partnerApprovedAt(partner.getPartnerApprovedAt())
                .email(partner.getEmail())
                .build();
    }

    /**
     * 파트너 휴업 신청
     * APPROVED 상태인 파트너만 신청 가능
     * 
     * @param session 현재 세션
     * @param requestDto 휴업 신청 정보
     */
    @Transactional
    public void requestDeactivation(HttpSession session, DeactivationRequestDto requestDto) {
        // 1. 로그인한 파트너 확인 (AuthService를 통해 현재 사용자 정보 가져오기)
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        
        // 2. 파트너 역할 확인
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 휴업 신청이 가능합니다.");
        }

        // 3. 파트너 조회 (subjectId는 partnerId)
        Long partnerId = currentUser.getSubjectId();
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        // 4. APPROVED 상태인지 확인
        if (partner.getPartnerStatus() != PartnerStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "운영 중인 파트너만 휴업 신청이 가능합니다.");
        }

        // 5. 이미 진행 중인 휴업 신청이 있는지 확인
        if (partner.getDeactivationRequestedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 휴업 신청이 접수되었습니다.");
        }

        // 6. 휴업 신청 처리
        partner.requestDeactivation(requestDto.getDeactivationReason());
        partner = partnerRepository.save(partner);

        // 7. 이력 기록 (휴업 신청)
        PartnerHistoryEntity history = PartnerHistoryEntity.createDeactivationRequest(partner, requestDto.getDeactivationReason());
        partnerHistoryRepository.save(history);
    }

    /**
     * 파트너 재활성화 신청
     * INACTIVE 상태인 파트너만 신청 가능
     * 
     * @param session 현재 세션
     * @param requestDto 재활성화 신청 정보
     */
    @Transactional
    public void requestReactivation(HttpSession session, ReactivationRequestDto requestDto) {
        // 1. 로그인한 파트너 확인 (AuthService를 통해 현재 사용자 정보 가져오기)
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        
        // 2. 파트너 역할 확인
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 재활성화 신청이 가능합니다.");
        }

        // 3. 파트너 조회 (subjectId는 partnerId)
        Long partnerId = currentUser.getSubjectId();
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        // 4. INACTIVE 상태인지 확인
        if (partner.getPartnerStatus() != PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비활성화된 파트너만 재활성화 신청이 가능합니다.");
        }

        // 5. 이미 재활성화 신청이 있는지 확인
        if (partner.getReactivationRequestedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 재활성화 신청이 접수되었습니다.");
        }

        // 6. 재활성화 신청 처리
        partner.requestReactivation(requestDto.getReactivationReason());
        partner = partnerRepository.save(partner);

        // 7. 이력 기록 (재활성화 신청)
        PartnerHistoryEntity history = PartnerHistoryEntity.createReactivationRequest(partner, requestDto.getReactivationReason());
        partnerHistoryRepository.save(history);
    }

    /**
     * 파트너 자신의 이력 조회
     * 로그인한 파트너만 자신의 이력을 조회할 수 있음
     * 
     * @param session 현재 세션
     * @param actionTypeStr 액션 타입 필터 (선택, null이면 전체)
     * @return 파트너 이력 목록
     */
    @Transactional(readOnly = true)
    public List<PartnerHistoryResponseDto> getMyHistory(HttpSession session, String actionTypeStr) {
        // 현재 로그인한 파트너 확인
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 자신의 이력을 조회할 수 있습니다.");
        }

        Long partnerId = currentUser.getSubjectId();
        
        // 파트너 존재 확인
        partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        // 이력 조회
        List<PartnerHistoryEntity> histories;
        if (actionTypeStr != null && !actionTypeStr.isEmpty()) {
            try {
                com.swimshop.swim_mall.common.enums.PartnerHistoryActionType actionType = 
                    com.swimshop.swim_mall.common.enums.PartnerHistoryActionType.valueOf(actionTypeStr);
                histories = partnerHistoryRepository.findByPartner_PartnerIdAndActionTypeOrderByCreatedAtDesc(
                        partnerId, actionType);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 액션 타입입니다: " + actionTypeStr);
            }
        } else {
            histories = partnerHistoryRepository.findByPartner_PartnerIdOrderByCreatedAtDesc(partnerId);
        }

        // DTO 변환
        return histories.stream()
                .map(history -> PartnerHistoryResponseDto.builder()
                        .historyId(history.getHistoryId())
                        .partnerId(history.getPartner().getPartnerId())
                        .partnerName(history.getPartner().getPartnerName())
                        .actionType(history.getActionType())
                        .fromStatus(history.getFromStatus())
                        .toStatus(history.getToStatus())
                        .reason(history.getReason())
                        .adminId(history.getAdmin() != null ? history.getAdmin().getAdminId() : null)
                        .adminName(history.getAdmin() != null ? history.getAdmin().getAdminName() : null)
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 파트너 자신의 매출 통계 조회
     * 배송 완료되고 구매 확정된 주문상품만 집계
     * 
     * @param session 현재 세션
     * @param startDate 시작 날짜 (선택)
     * @param endDate 종료 날짜 (선택)
     * @return 파트너 매출 통계
     */
    @Transactional(readOnly = true)
    public PartnerSalesStatisticsDto getMySalesStatistics(HttpSession session, LocalDate startDate, LocalDate endDate) {
        // 현재 로그인한 파트너 확인
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 자신의 매출을 조회할 수 있습니다.");
        }
        
        Long partnerId = currentUser.getSubjectId();
        
        // 파트너 존재 확인
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        // 날짜 기본값 설정
        final LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();
        final LocalDate finalStartDate = startDate != null ? startDate : finalEndDate.minusDays(30);
        
        // 배송 완료되고 구매확정된 주문상품(OrderItem)만 조회 (현재 파트너의 상품만)
        List<OrderItemEntity> completedOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> {
                    // 취소되지 않은 주문상품만
                    if (item.getIsCancelled()) return false;
                    
                    // 배송이 있고 배송 완료 상태인 것만
                    if (item.getDelivery() == null) return false;
                    if (item.getDelivery().getDeliveryStatus() != DeliveryStatus.DELIVERED) return false;
                    
                    // 구매확정된 주문상품만 (completedAt이 null이 아니어야 함)
                    if (item.getCompletedAt() == null) return false;
                    
                    // 현재 파트너의 상품/옵션인지 확인
                    boolean isMyProduct = false;
                    if (item.getOption() != null && item.getOption().getPartner() != null) {
                        isMyProduct = item.getOption().getPartner().getPartnerId().equals(partnerId);
                    } else if (item.getProduct() != null && item.getProduct().getPartner() != null) {
                        isMyProduct = item.getProduct().getPartner().getPartnerId().equals(partnerId);
                    }
                    if (!isMyProduct) return false;
                    
                    // 기간 필터링 (구매확정일 기준)
                    LocalDate completedDate = item.getCompletedAt().toLocalDate();
                    return !completedDate.isBefore(finalStartDate) && !completedDate.isAfter(finalEndDate);
                })
                .collect(Collectors.toList());
        
        // 전체 통계
        Long totalSales = completedOrderItems.stream()
                .mapToLong(OrderItemEntity::getItemTotalPrice)
                .sum();
        Long totalOrders = completedOrderItems.stream()
                .map(OrderItemEntity::getOrder)
                .map(OrderEntity::getOrderNo)
                .distinct()
                .count();
        Long totalQuantity = completedOrderItems.stream()
                .mapToLong(OrderItemEntity::getItemQuantity)
                .sum();
        Long averageOrderAmount = totalOrders > 0 ? totalSales / totalOrders : 0L;
        
        // 일별 매출
        List<PartnerSalesStatisticsDto.PeriodSalesDto> dailySales = calculateDailySales(completedOrderItems, finalStartDate, finalEndDate);
        
        // 주별 매출
        List<PartnerSalesStatisticsDto.PeriodSalesDto> weeklySales = calculateWeeklySales(completedOrderItems, finalStartDate, finalEndDate);
        
        // 월별 매출
        List<PartnerSalesStatisticsDto.PeriodSalesDto> monthlySales = calculateMonthlySales(completedOrderItems, finalStartDate, finalEndDate);
        
        // 상품별 매출 TOP 10
        List<PartnerSalesStatisticsDto.ProductSalesDto> topProducts = calculateTopProducts(completedOrderItems, 10);
        
        // 카테고리별 매출
        List<PartnerSalesStatisticsDto.CategorySalesDto> categorySales = calculateCategorySales(completedOrderItems);
        
        return PartnerSalesStatisticsDto.builder()
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .totalQuantity(totalQuantity)
                .averageOrderAmount(averageOrderAmount)
                .dailySales(dailySales)
                .weeklySales(weeklySales)
                .monthlySales(monthlySales)
                .topProducts(topProducts)
                .categorySales(categorySales)
                .build();
    }
    
    /**
     * 일별 매출 계산
     */
    private List<PartnerSalesStatisticsDto.PeriodSalesDto> calculateDailySales(
            List<OrderItemEntity> orderItems, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<OrderItemEntity>> dailyMap = orderItems.stream()
                .collect(Collectors.groupingBy(item -> 
                    item.getCompletedAt().toLocalDate()));
        
        List<PartnerSalesStatisticsDto.PeriodSalesDto> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<OrderItemEntity> dayItems = dailyMap.getOrDefault(current, new ArrayList<>());
            Long daySales = dayItems.stream()
                    .mapToLong(OrderItemEntity::getItemTotalPrice)
                    .sum();
            Long dayOrderCount = dayItems.stream()
                    .map(OrderItemEntity::getOrder)
                    .map(OrderEntity::getOrderNo)
                    .distinct()
                    .count();
            Long dayQuantity = dayItems.stream()
                    .mapToLong(OrderItemEntity::getItemQuantity)
                    .sum();
            Long dayAverage = dayOrderCount > 0 ? daySales / dayOrderCount : 0L;
            
            result.add(PartnerSalesStatisticsDto.PeriodSalesDto.builder()
                    .date(current)
                    .totalSales(daySales)
                    .totalOrders(dayOrderCount)
                    .totalQuantity(dayQuantity)
                    .averageOrderAmount(dayAverage)
                    .build());
            current = current.plusDays(1);
        }
        return result;
    }
    
    /**
     * 주별 매출 계산
     */
    private List<PartnerSalesStatisticsDto.PeriodSalesDto> calculateWeeklySales(
            List<OrderItemEntity> orderItems, LocalDate startDate, LocalDate endDate) {
        // 주의 첫 날(월요일)을 키로 사용
        Map<LocalDate, List<OrderItemEntity>> weeklyMap = orderItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    LocalDate completedDate = item.getCompletedAt().toLocalDate();
                    // 해당 주의 월요일 찾기
                    DayOfWeek dayOfWeek = completedDate.getDayOfWeek();
                    int daysToSubtract = dayOfWeek.getValue() - 1; // 월요일이 1이므로
                    return completedDate.minusDays(daysToSubtract);
                }));
        
        return weeklyMap.entrySet().stream()
                .map(entry -> {
                    LocalDate weekStart = entry.getKey();
                    List<OrderItemEntity> weekItems = entry.getValue();
                    Long weekSales = weekItems.stream()
                            .mapToLong(OrderItemEntity::getItemTotalPrice)
                            .sum();
                    Long weekOrderCount = weekItems.stream()
                            .map(OrderItemEntity::getOrder)
                            .map(OrderEntity::getOrderNo)
                            .distinct()
                            .count();
                    Long weekQuantity = weekItems.stream()
                            .mapToLong(OrderItemEntity::getItemQuantity)
                            .sum();
                    Long weekAverage = weekOrderCount > 0 ? weekSales / weekOrderCount : 0L;
                    
                    return PartnerSalesStatisticsDto.PeriodSalesDto.builder()
                            .date(weekStart) // 주의 첫 날 (월요일)
                            .totalSales(weekSales)
                            .totalOrders(weekOrderCount)
                            .totalQuantity(weekQuantity)
                            .averageOrderAmount(weekAverage)
                            .build();
                })
                .sorted(Comparator.comparing(PartnerSalesStatisticsDto.PeriodSalesDto::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 월별 매출 계산
     */
    private List<PartnerSalesStatisticsDto.PeriodSalesDto> calculateMonthlySales(
            List<OrderItemEntity> orderItems, LocalDate startDate, LocalDate endDate) {
        Map<YearMonth, List<OrderItemEntity>> monthlyMap = orderItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    LocalDate completedDate = item.getCompletedAt().toLocalDate();
                    return YearMonth.from(completedDate);
                }));
        
        return monthlyMap.entrySet().stream()
                .map(entry -> {
                    YearMonth month = entry.getKey();
                    List<OrderItemEntity> monthItems = entry.getValue();
                    Long monthSales = monthItems.stream()
                            .mapToLong(OrderItemEntity::getItemTotalPrice)
                            .sum();
                    Long monthOrderCount = monthItems.stream()
                            .map(OrderItemEntity::getOrder)
                            .map(OrderEntity::getOrderNo)
                            .distinct()
                            .count();
                    Long monthQuantity = monthItems.stream()
                            .mapToLong(OrderItemEntity::getItemQuantity)
                            .sum();
                    Long monthAverage = monthOrderCount > 0 ? monthSales / monthOrderCount : 0L;
                    
                    return PartnerSalesStatisticsDto.PeriodSalesDto.builder()
                            .date(month.atDay(1)) // 월의 첫 날
                            .totalSales(monthSales)
                            .totalOrders(monthOrderCount)
                            .totalQuantity(monthQuantity)
                            .averageOrderAmount(monthAverage)
                            .build();
                })
                .sorted(Comparator.comparing(PartnerSalesStatisticsDto.PeriodSalesDto::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 상품별 매출 TOP N 계산
     */
    private List<PartnerSalesStatisticsDto.ProductSalesDto> calculateTopProducts(
            List<OrderItemEntity> orderItems, int topN) {
        Map<Long, ProductSalesInfo> productMap = new HashMap<>();
        
        for (OrderItemEntity item : orderItems) {
            Long productNo = item.getProduct().getProductNo();
            ProductSalesInfo info = productMap.getOrDefault(productNo, 
                new ProductSalesInfo(item.getProduct().getProductName()));
            
            info.totalSales += item.getItemTotalPrice();
            info.totalQuantity += item.getItemQuantity();
            info.orderCount++;
            
            productMap.put(productNo, info);
        }
        
        return productMap.entrySet().stream()
                .map(entry -> PartnerSalesStatisticsDto.ProductSalesDto.builder()
                        .productNo(entry.getKey())
                        .productName(entry.getValue().productName)
                        .totalSales(entry.getValue().totalSales)
                        .totalQuantity(entry.getValue().totalQuantity)
                        .orderCount(entry.getValue().orderCount)
                        .build())
                .sorted(Comparator.comparing(PartnerSalesStatisticsDto.ProductSalesDto::getTotalSales).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 매출 계산
     */
    private List<PartnerSalesStatisticsDto.CategorySalesDto> calculateCategorySales(List<OrderItemEntity> orderItems) {
        Map<String, CategorySalesInfo> categoryMap = new HashMap<>();
        
        for (OrderItemEntity item : orderItems) {
            ProductType productType = item.getProduct().getProductType();
            String categoryName = productType.getLabel();
            
            CategorySalesInfo info = categoryMap.getOrDefault(categoryName,
                new CategorySalesInfo(categoryName));
            
            info.totalSales += item.getItemTotalPrice();
            info.totalQuantity += item.getItemQuantity();
            info.orderCount++;
            
            categoryMap.put(categoryName, info);
        }
        
        return categoryMap.entrySet().stream()
                .map(entry -> {
                    CategorySalesInfo info = entry.getValue();
                    return PartnerSalesStatisticsDto.CategorySalesDto.builder()
                            .categoryName(info.categoryName)
                            .totalSales(info.totalSales)
                            .totalQuantity(info.totalQuantity)
                            .orderCount(info.orderCount)
                            .build();
                })
                .sorted(Comparator.comparing(PartnerSalesStatisticsDto.CategorySalesDto::getTotalSales).reversed())
                .collect(Collectors.toList());
    }
    
    // Helper classes
    private static class ProductSalesInfo {
        String productName;
        Long totalSales = 0L;
        Long totalQuantity = 0L;
        Long orderCount = 0L;
        
        ProductSalesInfo(String productName) {
            this.productName = productName;
        }
    }
    
    private static class CategorySalesInfo {
        String categoryName;
        Long totalSales = 0L;
        Long totalQuantity = 0L;
        Long orderCount = 0L;
        
        CategorySalesInfo(String categoryName) {
            this.categoryName = categoryName;
        }
    }
}
