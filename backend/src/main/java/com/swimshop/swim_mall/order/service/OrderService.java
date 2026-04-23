package com.swimshop.swim_mall.order.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.cart.entity.CartItemEntity;
import com.swimshop.swim_mall.cart.repository.CartItemRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.OrderItemStatus;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.customer.service.CustomerGradeService;
import com.swimshop.swim_mall.order.dto.OrderCreateRequestDto;
import com.swimshop.swim_mall.order.dto.OrderItemResponseDto;
import com.swimshop.swim_mall.order.dto.OrderResponseDto;
import com.swimshop.swim_mall.order.dto.OrderStatusUpdateRequestDto;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.order.repository.OrderRepository;
import com.swimshop.swim_mall.delivery.entity.DeliveryEntity;
import com.swimshop.swim_mall.delivery.repository.DeliveryRepository;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import com.swimshop.swim_mall.customer.entity.CustomerActivityLogEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository;
import com.swimshop.swim_mall.common.enums.CustomerActivityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.event.partner.service.PartnerEventService;
import com.swimshop.swim_mall.event.admin.service.AdminEventRewardService;
import com.swimshop.swim_mall.event.repository.EventRepository;
import com.swimshop.swim_mall.event.repository.EventPointTargetRepository;
import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.sale.dto.SalePolicyResponseDto;
import com.swimshop.swim_mall.sale.service.SalePolicyService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final AuthService authService;
    private final DeliveryRepository deliveryRepository;
    private final PartnerRepository partnerRepository;
    private final InventoryService inventoryService;
    private final CustomerActivityLogRepository customerActivityLogRepository;
    private final CustomerGradeService customerGradeService;
    private final com.swimshop.swim_mall.point.service.PointService pointService;
    private final PartnerEventService partnerEventService;
    private final AdminEventRewardService adminEventRewardService;
    private final EventRepository eventRepository;
    private final EventPointTargetRepository eventPointTargetRepository;
    private final SalePolicyService salePolicyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 주문 생성
     */
    @Transactional
    public OrderResponseDto createOrder(HttpSession session, OrderCreateRequestDto requestDto) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID 가져오기
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 장바구니 아이템들 조회 및 검증
        List<CartItemEntity> cartItems = requestDto.getCartItemNos().stream()
                .map(cartItemNo -> cartItemRepository.findById(cartItemNo)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)))
                .collect(Collectors.toList());
        
        // 장바구니 아이템이 비어있으면 에러
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // 장바구니 아이템들이 현재 고객의 것인지 검증 (보안: 남의 장바구니로 주문 방지)
        for (CartItemEntity cartItem : cartItems) {
            if (!cartItem.getCart().getCustomer().getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        
        // 배송지/연락처 필수값 및 형식 검증
        validateOrderRequest(requestDto);

        // 가격 보장(15분) 세션 락이 유효하면, 세일 적용 판정 기준시각도 락 시작시각으로 고정합니다.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime saleAt = now;
        Object lockStartedAtObj = session.getAttribute("priceLockStartedAt");
        if (lockStartedAtObj instanceof LocalDateTime lockStartedAt) {
            LocalDateTime lockExpiresAt = lockStartedAt.plusMinutes(15);
            if (!now.isAfter(lockExpiresAt)) {
                saleAt = lockStartedAt;
            }
        }

        // 총 주문 금액 계산 (세일 할인액 포함, 서버에서 최종 확정)
        Map<Long, Long> discountAmountByCartItemNo = new HashMap<>();
        Map<Long, Long> itemTotalPriceByCartItemNo = new HashMap<>();
        Map<Long, Long> appliedSalePolicyNoByCartItemNo = new HashMap<>();
        Map<Long, String> appliedSaleCampaignIdByCartItemNo = new HashMap<>();
        Map<Long, Long> appliedSaleEventNoByCartItemNo = new HashMap<>();
        Map<Long, EventType> appliedSaleEventTypeByCartItemNo = new HashMap<>();
        Long totalPrice = 0L;
        for (CartItemEntity cartItem : cartItems) {
            Long baseTotalPrice = cartItem.getItemPrice() * cartItem.getQuantity();

            Long productNo = cartItem.getProduct() != null ? cartItem.getProduct().getProductNo() : null;
            Long optionNo = cartItem.getOption() != null ? cartItem.getOption().getOptionNo() : null;

            SalePolicyResponseDto salePolicy = salePolicyService.findApplicable(productNo, optionNo, saleAt);
            Long discountAmount = calculateDiscountAmount(salePolicy, baseTotalPrice);

            Long finalTotal = Math.max(0L, baseTotalPrice - discountAmount);

            discountAmountByCartItemNo.put(cartItem.getCartItemNo(), discountAmount);
            itemTotalPriceByCartItemNo.put(cartItem.getCartItemNo(), finalTotal);
            appliedSalePolicyNoByCartItemNo.put(
                    cartItem.getCartItemNo(),
                    salePolicy != null ? salePolicy.getId() : null
            );
            appliedSaleCampaignIdByCartItemNo.put(
                    cartItem.getCartItemNo(),
                    salePolicy != null ? salePolicy.getCampaignId() : null
            );
            appliedSaleEventNoByCartItemNo.put(
                    cartItem.getCartItemNo(),
                    salePolicy != null ? salePolicy.getEventNo() : null
            );
            appliedSaleEventTypeByCartItemNo.put(
                    cartItem.getCartItemNo(),
                    salePolicy != null ? salePolicy.getEventType() : null
            );
            totalPrice += finalTotal;
        }

        // 포인트 사용 금액 (선택)
        Long usePointAmount = requestDto.getUsePointAmount();
        if (usePointAmount != null && usePointAmount < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "포인트 사용 금액은 0 이상이어야 합니다.");
        }
        if (usePointAmount == null) {
            usePointAmount = 0L;
        }
        if (usePointAmount > totalPrice) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "포인트 사용 금액은 주문 금액을 초과할 수 없습니다.");
        }
        
        // 전화번호 정규화 (하이픈 제거하여 저장)
        String normalizedPhone = normalizePhoneNumber(requestDto.getRecipientPhone());
        
        // 주문 엔티티 생성 (결제 대기 상태로 시작, Payment는 결제 성공/실패 시에만 생성)
        OrderEntity order = OrderEntity.builder()
                .customer(customer)
                .payment(null) // 결제 성공/실패 시에만 Payment 생성
                .orderTotalPrice(totalPrice - usePointAmount)
                .usedPointAmount(usePointAmount)
                .orderStatus(OrderStatus.PENDING_PAYMENT) // 결제 대기 상태
                .paymentMethod(requestDto.getPaymentMethod().trim()) // 결제 방법 저장
                .recipientName(requestDto.getRecipientName().trim())
                .recipientPhone(normalizedPhone)
                .deliveryAddress(requestDto.getDeliveryAddress().trim())
                .deliveryAddressDetail(requestDto.getDeliveryAddressDetail() != null 
                        ? requestDto.getDeliveryAddressDetail().trim() : null)
                .deliveryZipCode(requestDto.getDeliveryZipCode() != null 
                        ? requestDto.getDeliveryZipCode().trim() : null)
                .orderMemo(requestDto.getOrderMemo() != null 
                        ? requestDto.getOrderMemo().trim() : null)
                .build();
        order = orderRepository.save(order);

        // 포인트 사용 처리 (잔액 차감 및 히스토리 기록)
        if (usePointAmount > 0) {
            try {
                pointService.usePoint(customer, order, usePointAmount);
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.INSUFFICIENT_POINT) {
                    // 포인트 부족 시 주문 생성 실패 처리 (재고 롤백을 위해 예외 전파)
                    throw e;
                }
                throw e;
            }
        }
        
        // 재고 확인 및 차감 (주문 생성 시)
        // 주문 생성 시점에 재고를 차감하여 재고 부족으로 인한 주문 실패를 방지
        for (CartItemEntity cartItem : cartItems) {
            try {
                Long optionNo = null;
                Long productNo = null;
                
                // 옵션이 있는 경우: optionNo 사용
                if (cartItem.getOption() != null) {
                    optionNo = cartItem.getOption().getOptionNo();
                } else {
                    // 옵션이 없는 경우: productNo 사용
                    if (cartItem.getProduct() == null) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                                "장바구니 아이템에 상품 정보가 없습니다.");
                    }
                    productNo = cartItem.getProduct().getProductNo();
                }
                
                inventoryService.decreaseStockForOrder(
                        optionNo,
                        productNo,
                        cartItem.getQuantity()
                );
            } catch (BusinessException e) {
                // 재고 부족 시 주문 생성 실패 (트랜잭션 롤백)
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                        String.format("주문 생성 실패: %s", e.getMessage()));
            }
        }
        
        // 주문 아이템 엔티티들 생성
        List<OrderItemEntity> orderItems = new ArrayList<>();
        LocalDateTime snapshotAt = LocalDateTime.now();
        List<Long> adminEventSnapshotNos = adminEventRewardService.getSnapshotEligibleEventNosAt(snapshotAt);
        Map<Long, List<Long>> partnerEventSnapshotCache = new HashMap<>();
        for (CartItemEntity cartItem : cartItems) {
            Long itemTotalPrice = itemTotalPriceByCartItemNo.get(cartItem.getCartItemNo());
            Long itemDiscountAmount = discountAmountByCartItemNo.get(cartItem.getCartItemNo());
            Long appliedSalePolicyNo = appliedSalePolicyNoByCartItemNo.get(cartItem.getCartItemNo());
            String appliedSaleCampaignId = appliedSaleCampaignIdByCartItemNo.get(cartItem.getCartItemNo());
            Long appliedSaleEventNo = appliedSaleEventNoByCartItemNo.get(cartItem.getCartItemNo());
            EventType appliedSaleEventType = appliedSaleEventTypeByCartItemNo.get(cartItem.getCartItemNo());

            Long partnerId = resolvePartnerId(cartItem);
            List<Long> partnerEventSnapshotNos = List.of();
            if (partnerId != null) {
                partnerEventSnapshotNos = partnerEventSnapshotCache.computeIfAbsent(
                        partnerId,
                        id -> partnerEventService.getSnapshotEligibleEventNosForPartnerAt(id, snapshotAt)
                );
            }
            
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .option(cartItem.getOption())
                    .itemQuantity(cartItem.getQuantity())
                    .itemPrice(cartItem.getItemPrice())
                    .itemDiscountAmount(itemDiscountAmount != null ? itemDiscountAmount : 0L)
                    .itemTotalPrice(itemTotalPrice != null ? itemTotalPrice : (cartItem.getItemPrice() * cartItem.getQuantity()))
                    .isCancelled(false)
                    .build();

            orderItem.lockEventSnapshot(
                    toCsv(partnerEventSnapshotNos),
                    toCsv(adminEventSnapshotNos),
                    snapshotAt
            );
            orderItem.lockSaleSnapshot(
                    appliedSalePolicyNo,
                    appliedSaleCampaignId,
                    appliedSaleEventNo,
                    appliedSaleEventType,
                    saleAt
            );
            
            // 이벤트 정책 스냅샷(JSON) 구성: 주문 시점 기준
            try {
                // 관리자 단독 이벤트 정책 스냅샷
                List<Map<String, Object>> adminPolicies = adminEventSnapshotNos.stream()
                        .map(eventNo -> {
                            var eOpt = eventRepository.findById(eventNo);
                            if (eOpt.isEmpty()) return null;
                            var e = eOpt.get();
                            Map<String, Object> m = new HashMap<>();
                            m.put("eventTitle", e.getEventTitle());
                            m.put("eventNo", e.getEventNo());
                            m.put("eventType", e.getEventType() != null ? e.getEventType().name() : null);
                            m.put("rewardType", e.getSaleDiscountType() != null ? e.getSaleDiscountType().name() : null);
                            m.put("rewardValue", e.getSaleDiscountValue());
                            m.put("pointEventTargetType", e.getPointEventTargetType() != null ? e.getPointEventTargetType().name() : null);
                            m.put("pointEventTargetValues", readPointTargetValuesSafe(e.getEventNo()));
                            m.put("pointEventMinOrderAmount", e.getPointEventMinOrderAmount());
                            return m;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                // 파트너 참여형 이벤트 정책 스냅샷(파트너별 eligible 이벤트 번호 기준)
                List<Map<String, Object>> partnerPolicies = partnerEventSnapshotNos.stream()
                        .map(eventNo -> {
                            var eOpt = eventRepository.findById(eventNo);
                            if (eOpt.isEmpty()) return null;
                            var e = eOpt.get();
                            Map<String, Object> m = new HashMap<>();
                            m.put("eventTitle", e.getEventTitle());
                            m.put("eventNo", e.getEventNo());
                            m.put("eventType", e.getEventType() != null ? e.getEventType().name() : null);
                            m.put("rewardType", e.getSaleDiscountType() != null ? e.getSaleDiscountType().name() : null);
                            m.put("rewardValue", e.getSaleDiscountValue());
                            m.put("pointEventTargetType", e.getPointEventTargetType() != null ? e.getPointEventTargetType().name() : null);
                            m.put("pointEventTargetValues", readPointTargetValuesSafe(e.getEventNo()));
                            m.put("pointEventMinOrderAmount", e.getPointEventMinOrderAmount());
                            return m;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                String adminPolicyJson = objectMapper.writeValueAsString(Map.of(
                        "policies", adminPolicies,
                        "snapshotAt", snapshotAt.toString()
                ));
                String partnerPolicyJson = objectMapper.writeValueAsString(Map.of(
                        "policies", partnerPolicies,
                        "snapshotAt", snapshotAt.toString()
                ));
                orderItem.lockEventPolicySnapshot(adminPolicyJson, partnerPolicyJson);
            } catch (Exception ignore) {
                // 스냅샷 구성 실패해도 주문은 진행
            }
            
            orderItems.add(orderItem);
        }
        orderItems = orderItemRepository.saveAll(orderItems);
        
        // 결제 확정 전까지는 장바구니를 유지합니다.
        // 결제 승인(confirm) 성공 시 장바구니에서 해당 아이템들을 삭제하도록 시점을 이동합니다.
        
        // 활동 로그 기록
        try {
            String activityDetails = objectMapper.writeValueAsString(Map.of(
                "orderNo", order.getOrderNo(),
                "orderTotalPrice", order.getOrderTotalPrice(),
                "itemCount", orderItems.size()
            ));
            CustomerActivityLogEntity log = CustomerActivityLogEntity.create(
                customer,
                CustomerActivityType.ORDER_CREATED,
                activityDetails,
                null, // IP 주소는 나중에 추가 가능
                null  // User-Agent는 나중에 추가 가능
            );
            customerActivityLogRepository.save(log);
        } catch (Exception e) {
            // 로그 기록 실패해도 주문은 계속 진행
        }
        
        // 응답 DTO 생성
        List<OrderItemResponseDto> orderItemDtos = orderItems.stream()
                .map(this::toOrderItemResponseDto)
                .collect(Collectors.toList());
        
        return OrderResponseDto.builder()
                .orderNo(order.getOrderNo())
                .orderTotalPrice(order.getOrderTotalPrice())
                .orderCreatedAt(order.getOrderCreatedAt())
                .orderStatus(order.getOrderStatus())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryAddressDetail(order.getDeliveryAddressDetail())
                .deliveryZipCode(order.getDeliveryZipCode())
                .paymentMethod(order.getPayment() != null ? order.getPayment().getPaymentMethod() : order.getPaymentMethod())
                .paymentAmount(order.getPayment() != null ? order.getPayment().getPaymentAmount() : null)
                .orderMemo(order.getOrderMemo())
                .orderItems(orderItemDtos)
                .build();
    }
    
    /**
     * 주문 목록 조회
     */
    @Transactional
    public com.swimshop.swim_mall.common.response.PagedResponse<OrderResponseDto> getOrders(HttpSession session, int page, int size) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID 가져오기
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 주문 목록 조회
        List<OrderEntity> orders = orderRepository.findByCustomerOrderByOrderCreatedAtDesc(customer);
        
        int total = orders.size();
        int from = Math.max(0, Math.min((page - 1) * size, total));
        int to = Math.max(from, Math.min(from + size, total));
        List<OrderResponseDto> items = orders.subList(from, to).stream()
                .map(order -> toOrderResponseDto(order, null))
                .collect(Collectors.toList());
        return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
    }
    
    /**
     * 주문 상세 조회
     */
    @Transactional
    public OrderResponseDto getOrder(HttpSession session, Long orderNo) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID 가져오기
        Long customerId = getCustomerIdFromSession(session);
        
        // 주문 조회
        OrderEntity order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문이 현재 고객의 것인지 검증
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        return toOrderResponseDto(order, null);
    }

    private List<String> readPointTargetValuesSafe(Long eventNo) {
        try {
            return eventPointTargetRepository.findByEvent_EventNo(eventNo).stream()
                    .map(com.swimshop.swim_mall.event.entity.EventPointTargetEntity::getTargetValue)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 전체 주문 목록 조회 (관리자/파트너용)
     * GET /api/orders/admin
     * - 관리자: 모든 주문 조회
     * - 파트너: 자신의 상품 주문만 조회
     */
    @Transactional
    public com.swimshop.swim_mall.common.response.PagedResponse<OrderResponseDto> getAllOrders(HttpSession session, int page, int size) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        if (userRole == AccountRole.ADMIN) {
            // 관리자: 모든 주문 조회
            List<OrderEntity> orders = orderRepository.findAllWithRelations();
            int total = orders.size();
            int from = Math.max(0, Math.min((page - 1) * size, total));
            int to = Math.max(from, Math.min(from + size, total));
            List<OrderResponseDto> items = orders.subList(from, to).stream()
                    .map(order -> toOrderResponseDto(order, null))
                    .collect(Collectors.toList());
            return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
        } else if (userRole == AccountRole.PARTNER) {
            // 파트너: 자신의 상품 주문만 조회
            Long partnerId = getPartnerIdFromSession(session);
            List<OrderEntity> orders = orderRepository.findByPartnerId(partnerId);
            int total = orders.size();
            int from = Math.max(0, Math.min((page - 1) * size, total));
            int to = Math.max(from, Math.min(from + size, total));
            List<OrderResponseDto> items = orders.subList(from, to).stream()
                    .map(order -> toOrderResponseDto(order, partnerId))
                    .collect(Collectors.toList());
            return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 접근 가능합니다.");
        }
    }

    /**
     * 주문 상세 조회 (관리자/파트너용)
     * GET /api/orders/{orderNo}/admin
     */
    @Transactional
    public OrderResponseDto getOrderForAdmin(HttpSession session, Long orderNo) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        authService.requireRole(session, AccountRole.ADMIN, AccountRole.PARTNER);
        
        // 주문 조회 (OrderItems와 관련 엔티티 포함)
        OrderEntity order = orderRepository.findByOrderNoWithRelations(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 파트너는 자신의 상품 주문만 조회 가능
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            boolean hasPartnerProduct = order.getOrderItems().stream()
                    .anyMatch(orderItem -> {
                        if (orderItem.getOption() != null) {
                            return orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
                        } else if (orderItem.getProduct().getPartner() != null) {
                            return orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
                        }
                        return false;
                    });
            
            if (!hasPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품 주문만 조회할 수 있습니다.");
            }
            
            // 파트너의 경우 자신의 상품 OrderItem만 필터링하여 반환
            return toOrderResponseDto(order, partnerId);
        }
        // 관리자(ADMIN): 모든 주문 조회 가능 (검증 없음)
        
        return toOrderResponseDto(order, null);
    }

    /**
     * 주문 상태 변경 (관리자만)
     * PATCH /api/orders/{orderNo}/status
     * - 관리자: 모든 주문 상태 변경 가능 (예외 상황 처리용)
     * - 파트너: 주문 상태 변경 불가 (발주 확인 API 사용)
     */
    @Transactional
    public OrderResponseDto updateOrderStatus(HttpSession session, Long orderNo, OrderStatusUpdateRequestDto requestDto) {
        // 관리자만 가능
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 주문 조회 (OrderItem과 Delivery를 함께 조회)
        OrderEntity order = orderRepository.findByOrderNoWithRelations(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문 상태 변경 (검증 포함)
        try {
            order.changeStatus(requestDto.getOrderStatus());
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        }
        
        // 주문 취소 시 모든 OrderItem의 isCancelled를 true로 설정
        if (requestDto.getOrderStatus() == OrderStatus.CANCELLED) {
            List<OrderItemEntity> orderItems = orderItemRepository.findByOrder(order);
            for (OrderItemEntity orderItem : orderItems) {
                orderItem.cancel();
            }
            orderItemRepository.saveAll(orderItems);
        }
        
        order = orderRepository.save(order);
        
        return toOrderResponseDto(order, null);
    }

    /**
     * 주문 상품별 발주 확인 (파트너/관리자용)
     * 결제 완료된 주문 상품을 발주 확인하여 배송 생성
     * 모든 주문 상품이 발주 확인되면 주문 상태를 ACTIVE로 변경
     * POST /api/orders/order-items/{orderItemNo}/confirm
     */
    @Transactional
    public OrderItemResponseDto confirmOrderItem(HttpSession session, Long orderItemNo) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        authService.requireRole(session, AccountRole.ADMIN, AccountRole.PARTNER);
        
        // 주문 상품 조회 (Order와 Delivery를 함께 조회)
        OrderItemEntity orderItem = orderItemRepository.findById(orderItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "주문 상품을 찾을 수 없습니다."));
        
        // Order를 로드하기 위해 접근
        OrderEntity order = orderItem.getOrder();
        
        // 파트너는 자신의 상품 주문만 발주 확인 가능
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 주문 관리 불가
            checkPartnerActive(partnerId);
            
            boolean isPartnerProduct = false;
            
            if (orderItem.getOption() != null) {
                isPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            } else if (orderItem.getProduct().getPartner() != null) {
                isPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!isPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품 주문만 발주 확인할 수 있습니다.");
            }
        }
        
        // 발주 확인 처리
        try {
            orderItem.confirmOrderItem();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        }
        
        // 배송 생성 (아직 생성되지 않은 경우)
        if (orderItem.getDelivery() == null) {
            DeliveryEntity delivery = DeliveryEntity.builder()
                    .orderItem(orderItem)
                    .deliveryStatus(DeliveryStatus.READY)
                    .deliveryStartDate(null)
                    .deliveryEndDate(null)
                    .deliveryTrackingNumber(null)
                    .deliveryCourier(null)
                    .build();
            deliveryRepository.save(delivery);
        }
        
        orderItem = orderItemRepository.save(orderItem);
        
        // 모든 주문 상품이 발주 확인되었는지 확인
        checkAndUpdateOrderStatus(order);
        
        return toOrderItemResponseDto(orderItem);
    }
    
    /**
     * 모든 주문 상품이 발주 확인되었는지 확인하고, 주문 상태를 ACTIVE로 변경
     */
    private void checkAndUpdateOrderStatus(OrderEntity order) {
        // 주문 상태가 PAID가 아니면 무시
        if (order.getOrderStatus() != OrderStatus.PAID) {
            return;
        }
        
        // 모든 주문 상품 조회
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrder(order);
        
        // 모든 주문 상품이 발주 확인되었는지 확인
        boolean allConfirmed = orderItems.stream()
                .allMatch(item -> item.getConfirmedAt() != null);
        
        if (allConfirmed) {
            // 주문 상태 전이: PAID → ACTIVE
            order.updateStatus(OrderStatus.ACTIVE);
            orderRepository.save(order);
        }
    }
    
    /**
     * 발주 확인 (파트너/관리자용) - 주문 전체 발주 확인 (레거시 호환)
     * 결제 완료된 주문을 발주 확인하여 주문 처리 시작
     * PAID → ACTIVE 전이 + 배송 생성
     * POST /api/orders/{orderNo}/confirm
     * @deprecated 주문 상품별 발주 확인을 사용하세요. (POST /api/orders/order-items/{orderItemNo}/confirm)
     */
    @Deprecated
    @Transactional
    public OrderResponseDto confirmOrder(HttpSession session, Long orderNo) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        authService.requireRole(session, AccountRole.ADMIN, AccountRole.PARTNER);
        
        // 주문 조회 (OrderItems와 관련 엔티티 포함)
        OrderEntity order = orderRepository.findByOrderNoWithRelations(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 파트너는 자신의 상품 주문만 발주 확인 가능
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 주문 관리 불가
            checkPartnerActive(partnerId);
            
            boolean hasPartnerProduct = order.getOrderItems().stream()
                    .anyMatch(orderItem -> {
                        if (orderItem.getOption() != null) {
                            return orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
                        } else if (orderItem.getProduct().getPartner() != null) {
                            return orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
                        }
                        return false;
                    });
            
            if (!hasPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품 주문만 발주 확인할 수 있습니다.");
            }
        }
        
        // 주문 상태 검증: PAID 상태만 발주 확인 가능
        if (order.getOrderStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "결제 완료된 주문만 발주 확인할 수 있습니다. 현재 상태: " + order.getOrderStatus().getLabel());
        }
        
        // 파트너인 경우 자신의 상품만 필터링
        List<OrderItemEntity> orderItemsToConfirm;
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 주문 관리 불가 (위에서 이미 체크했지만 중복 방어)
            checkPartnerActive(partnerId);
            orderItemsToConfirm = order.getOrderItems().stream()
                    .filter(orderItem -> {
                        if (orderItem.getOption() != null) {
                            return orderItem.getOption().getPartner() != null &&
                                   orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
                        } else if (orderItem.getProduct().getPartner() != null) {
                            return orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        } else {
            // 관리자는 모든 OrderItem 발주 확인
            orderItemsToConfirm = order.getOrderItems();
        }
        
        // 필터링된 OrderItem에 대해 발주 확인 처리 (confirmedAt 설정)
        // 주의: 주문 상태가 PAID일 때만 가능하므로, 주문 상태 변경 전에 처리
        for (OrderItemEntity orderItem : orderItemsToConfirm) {
            if (orderItem.getConfirmedAt() == null) {
                try {
                    orderItem.confirmOrderItem(); // confirmedAt 설정
                } catch (IllegalStateException e) {
                    // 이미 발주 확인되었거나 주문 상태가 PAID가 아닌 경우
                    // 레거시 메서드는 주문 전체를 한 번에 처리하므로, 
                    // 개별 OrderItem의 예외는 무시하고 계속 진행
                    // (log가 없으므로 예외만 무시)
                }
            }
        }
        
        // 모든 OrderItem이 발주 확인되었는지 확인하여 주문 상태 전이
        List<OrderItemEntity> allOrderItems = orderItemRepository.findByOrder(order);
        boolean allConfirmed = allOrderItems.stream()
                .allMatch(item -> item.getConfirmedAt() != null);
        
        if (allConfirmed) {
            // 주문 상태 전이: PAID → ACTIVE
            order.updateStatus(OrderStatus.ACTIVE);
            orderRepository.save(order);
        }
        
        // 배송 생성 (발주 확인된 OrderItem에 대해서만 Delivery 생성)
        for (OrderItemEntity orderItem : orderItemsToConfirm) {
            if (orderItem.getConfirmedAt() != null && orderItem.getDelivery() == null) {
                DeliveryEntity delivery = DeliveryEntity.builder()
                        .orderItem(orderItem)
                        .deliveryStatus(DeliveryStatus.READY)
                        .deliveryStartDate(null)
                        .deliveryEndDate(null)
                        .deliveryTrackingNumber(null)
                        .deliveryCourier(null)
                        .build();
                deliveryRepository.save(delivery);
            }
        }
        
        // OrderItem 저장 (confirmedAt 변경사항 반영)
        for (OrderItemEntity orderItem : orderItemsToConfirm) {
            if (orderItem.getConfirmedAt() != null) {
                orderItemRepository.save(orderItem);
            }
        }
        
        return toOrderResponseDto(order, null);
    }
    
    /**
     * 주문의 모든 OrderItem에 대해 Delivery 생성
     * 발주 확인 시 배송 준비 상태로 생성
     */
    @Transactional
    private void createDeliveriesForOrder(OrderEntity order) {
        // 주문의 모든 OrderItem 조회
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrder(order);
        
        // 각 OrderItem에 대해 Delivery 생성
        for (OrderItemEntity orderItem : orderItems) {
            // 이미 Delivery가 있으면 건너뜀 (중복 생성 방지)
            if (orderItem.getDelivery() != null) {
                continue;
            }
            
            // Delivery 생성 (초기 상태: READY)
            DeliveryEntity delivery = DeliveryEntity.builder()
                    .orderItem(orderItem)
                    .deliveryStatus(DeliveryStatus.READY)
                    .deliveryStartDate(null)
                    .deliveryEndDate(null)
                    .deliveryTrackingNumber(null)
                    .deliveryCourier(null)
                    .build();
            
            deliveryRepository.save(delivery);
        }
    }

    /**
     * 주문 취소 (고객용)
     * PATCH /api/orders/{orderNo}/cancel
     */
    @Transactional
    public OrderResponseDto cancelOrder(HttpSession session, Long orderNo) {
        // 고객만 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID 가져오기
        Long customerId = getCustomerIdFromSession(session);
        
        // 주문 조회 (OrderItem과 Delivery를 함께 조회)
        OrderEntity order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문이 현재 고객의 것인지 검증
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // OrderItem과 Delivery를 로드하기 위해 orderItems 접근
        order.getOrderItems().forEach(orderItem -> {
            if (orderItem.getDelivery() != null) {
                orderItem.getDelivery().getDeliveryStatus(); // Delivery 상태 로드
            }
        });
        
        // 주문 상태 검증: 취소 가능한 상태인지 확인
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 취소된 주문입니다.");
        }
        
        // 발주 확인된 주문 상품이 있으면 주문 취소 불가
        boolean hasConfirmedItem = order.getOrderItems().stream()
                .anyMatch(orderItem -> orderItem.getConfirmedAt() != null);
        
        if (hasConfirmedItem) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "발주 확인된 주문은 취소할 수 없습니다.");
        }
        
        // 구매 확정된 주문 상품이 있으면 주문 취소 불가
        boolean hasCompletedItem = order.getOrderItems().stream()
                .anyMatch(orderItem -> orderItem.getCompletedAt() != null);
        
        if (hasCompletedItem) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "구매 확정된 주문 상품이 있어 주문을 취소할 수 없습니다.");
        }
        
        // 주문 취소 (검증 포함)
        try {
            order.changeStatus(OrderStatus.CANCELLED);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        }
        
        // 모든 OrderItem의 isCancelled를 true로 설정 및 재고 복구
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrder(order);
        for (OrderItemEntity orderItem : orderItems) {
            orderItem.cancel();
            
            // 재고 복구 (주문 취소 시)
            try {
                Long optionNo = orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null;
                Long productNo = orderItem.getOption() == null ? orderItem.getProduct().getProductNo() : null;
                
                inventoryService.restoreStockForOrder(
                        optionNo,
                        productNo,
                        orderItem.getItemQuantity()
                );
            } catch (Exception e) {
                // 재고 복구 실패는 로그만 남기고 계속 진행 (옵션이나 상품이 삭제된 경우 등)
                // 주문 취소는 성공 처리
            }
        }
        orderItemRepository.saveAll(orderItems);
        
        // 포인트 복구 (주문 취소 시)
        try {
            pointService.restorePoint(order.getCustomer(), order.getOrderNo(), "주문 취소 복구");
        } catch (Exception e) {
            // 포인트 복구 실패는 로그만 남기고 계속 진행
            // 주문 취소는 성공 처리
        }
        
        order = orderRepository.save(order);
        
        return toOrderResponseDto(order, null);
    }

    /**
     * 주문 상품별 구매 확정 (고객이 수령 확인)
     * POST /api/order-items/{orderItemNo}/complete
     */
    @Transactional
    public OrderItemResponseDto completeOrderItem(HttpSession session, Long orderItemNo) {
        // 고객만 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID 가져오기
        Long customerId = getCustomerIdFromSession(session);
        
        // 주문 상품 조회 (Delivery와 ReturnEntity를 함께 조회)
        OrderItemEntity orderItem = orderItemRepository.findById(orderItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "주문 상품을 찾을 수 없습니다."));
        
        // 주문 상품이 현재 고객의 것인지 검증
        if (!orderItem.getOrder().getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // Delivery와 ReturnEntity를 로드하기 위해 접근
        if (orderItem.getDelivery() != null) {
            orderItem.getDelivery().getDeliveryStatus();
        }
        if (orderItem.getReturnEntity() != null) {
            orderItem.getReturnEntity().getReturnStatus();
        }
        
        // 구매 확정
        try {
            orderItem.completeOrderItem();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        }
        
        orderItem = orderItemRepository.save(orderItem);
        
        // 고객 정보 조회 (등급 및 포인트 적립을 위해)
        CustomerEntity customer = orderItem.getOrder().getCustomer();
        customer = customerRepository.findById(customer.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 고객 등급 자동 업데이트 (구매 확정 시)
        try {
            customerGradeService.updateCustomerGrade(customerId);
            // 등급 업데이트 후 다시 조회 (등급이 변경되었을 수 있음)
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        } catch (Exception e) {
            // 등급 업데이트 실패해도 구매 확정은 성공으로 처리
            // 로그만 남기고 계속 진행
            System.err.println("고객 등급 업데이트 실패: " + e.getMessage());
        }
        
        // 포인트 적립 (구매 확정 시)
        try {
            if (customer.getCustomerGrade() != null) {
                // 등급별 포인트 적립률 조회
                Double pointAccumulationRate = customer.getCustomerGrade().getPointAccumulationRate();
                
                // 주문 상품 금액 기준으로 포인트 계산
                Long orderItemAmount = orderItem.getItemTotalPrice();
                Long pointAmount = (long) (orderItemAmount * pointAccumulationRate / 100.0);
                
                // 포인트 적립 (최소 1원 단위)
                if (pointAmount > 0) {
                    pointService.accumulatePoint(customer, orderItem, pointAmount);
                    // MVP: 파트너 참여 이벤트 보상(포인트) 추가 적립
                    partnerEventService.handleOrderItemCompleted(orderItem, customer, pointAmount);
                    // MVP: 관리자 단독 이벤트(ADMIN_PROMOTION) 보상(포인트) 추가 적립
                    adminEventRewardService.handleOrderItemCompleted(orderItem, customer, pointAmount);
                }
            }
        } catch (Exception e) {
            // 포인트 적립 실패해도 구매 확정은 성공으로 처리
            // 로그만 남기고 계속 진행
            System.err.println("포인트 적립 실패: " + e.getMessage());
        }
        
        return toOrderItemResponseDto(orderItem);
    }
    
    /**
     * OrderEntity를 OrderResponseDto로 변환
     * @param order 주문 엔티티
     * @param partnerId 파트너 ID (null이면 필터링 없이 모든 OrderItem 포함, 파트너인 경우 자신의 상품만 필터링)
     */
    private OrderResponseDto toOrderResponseDto(OrderEntity order, Long partnerId) {
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrder(order);
        
        // 파트너인 경우 자신의 상품 OrderItem만 필터링
        if (partnerId != null) {
            orderItems = orderItems.stream()
                    .filter(orderItem -> {
                        if (orderItem.getOption() != null) {
                            return orderItem.getOption().getPartner() != null &&
                                   orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
                        } else if (orderItem.getProduct().getPartner() != null) {
                            return orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }
        
        List<OrderItemResponseDto> orderItemDtos = orderItems.stream()
                .map(this::toOrderItemResponseDto)
                .collect(Collectors.toList());
        
        // 파트너인 경우 자신의 상품 금액만 합산
        Long orderTotalPrice = order.getOrderTotalPrice();
        if (partnerId != null) {
            orderTotalPrice = orderItems.stream()
                    .mapToLong(OrderItemEntity::getItemTotalPrice)
                    .sum();
        }
        
        OrderResponseDto.OrderResponseDtoBuilder builder = OrderResponseDto.builder()
                .orderNo(order.getOrderNo())
                .orderTotalPrice(orderTotalPrice)
                .usedPointAmount(order.getUsedPointAmount())
                .orderCreatedAt(order.getOrderCreatedAt())
                .orderStatus(order.getOrderStatus())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryAddressDetail(order.getDeliveryAddressDetail())
                .deliveryZipCode(order.getDeliveryZipCode())
                .orderMemo(order.getOrderMemo())
                .orderItems(orderItemDtos);
        
        // 결제 정보 추가 (nullable)
        if (order.getPayment() != null) {
            builder.paymentNo(order.getPayment().getPaymentNo())
                    .paymentMethod(order.getPayment().getPaymentMethod())
                    .paymentAmount(order.getPayment().getPaymentAmount())
                    .paymentCreatedAt(order.getPayment().getPaymentCreatedAt())
                    .paidAt(order.getPayment().getPaidAt())
                    .paymentCancelYn(order.getPayment().getPaymentCancelYn());
        } else {
            // Payment가 없는 경우 (레거시 호환)
            builder.paymentMethod(order.getPaymentMethod())
                    .paymentAmount(null);
        }

        // 주문 목록/상세에서 표시할 대표 상태는 주문상품 상태를 기준으로 계산
        OrderItemStatus displayStatus = resolveOrderDisplayStatus(orderItemDtos);
        String displayStatusLabel = null;
        if (displayStatus != null) {
            displayStatusLabel = (displayStatus == OrderItemStatus.CANCELLED)
                    ? "주문 취소"
                    : displayStatus.getLabel();
        }
        builder.orderDisplayStatus(displayStatus)
                .orderDisplayStatusLabel(displayStatusLabel);
        
        return builder.build();
    }

    /**
     * 주문 표시 상태 계산 (주문상품 상태 기반)
     * 혼합 상태에서는 "덜 진행된" 상태를 대표로 표시합니다.
     * 우선순위: 취소 > 환불 완료 > 반품 거절 > 반품 진행중 > 배송 완료 > 배송 중 > 배송 준비 > 발주 확인 > 발주 확인 대기 > 구매 확정
     */
    private OrderItemStatus resolveOrderDisplayStatus(List<OrderItemResponseDto> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return null;
        }

        List<OrderItemStatus> statuses = orderItems.stream()
                .map(OrderItemResponseDto::getStatus)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        if (statuses.isEmpty()) return null;
        if (statuses.contains(OrderItemStatus.CANCELLED)) return OrderItemStatus.CANCELLED;
        if (statuses.contains(OrderItemStatus.REFUNDED)) return OrderItemStatus.REFUNDED;
        if (statuses.contains(OrderItemStatus.RETURN_REJECTED)) return OrderItemStatus.RETURN_REJECTED;
        if (statuses.contains(OrderItemStatus.RETURN_IN_PROGRESS)) return OrderItemStatus.RETURN_IN_PROGRESS;
        if (statuses.contains(OrderItemStatus.DELIVERED)) return OrderItemStatus.DELIVERED;
        if (statuses.contains(OrderItemStatus.SHIPPED)) return OrderItemStatus.SHIPPED;
        if (statuses.contains(OrderItemStatus.READY)) return OrderItemStatus.READY;
        if (statuses.contains(OrderItemStatus.CONFIRMED)) return OrderItemStatus.CONFIRMED;
        if (statuses.contains(OrderItemStatus.PENDING_CONFIRMATION)) return OrderItemStatus.PENDING_CONFIRMATION;
        return OrderItemStatus.COMPLETED;
    }
    
    /**
     * OrderItemEntity를 OrderItemResponseDto로 변환
     */
    private OrderItemResponseDto toOrderItemResponseDto(OrderItemEntity orderItem) {
        OrderItemResponseDto.OrderItemResponseDtoBuilder builder = OrderItemResponseDto.builder()
                .orderItemNo(orderItem.getOrderItemNo())
                .productNo(orderItem.getProduct().getProductNo())
                .productName(orderItem.getProduct().getProductName())
                .productImageUrl(orderItem.getProduct().getProductImageUrl())
                .optionNo(orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null)
                .color(orderItem.getOption() != null ? orderItem.getOption().getColor() : null)
                .size(orderItem.getOption() != null ? orderItem.getOption().getSize() : null)
                .quantity(orderItem.getItemQuantity())
                .itemPrice(orderItem.getItemPrice())
                .itemDiscountAmount(orderItem.getItemDiscountAmount())
                .itemTotalPrice(orderItem.getItemTotalPrice())
                .appliedSalePolicyNo(orderItem.getAppliedSalePolicyNo())
                .appliedSaleCampaignId(orderItem.getAppliedSaleCampaignId())
                .appliedSaleEventNo(orderItem.getAppliedSaleEventNo())
                .appliedSaleEventType(orderItem.getAppliedSaleEventType())
                .saleEvaluatedAt(orderItem.getSaleEvaluatedAt())
                .isCancelled(orderItem.getIsCancelled())
                .completedAt(orderItem.getCompletedAt()) // 구매 확정일시 추가
                .confirmedAt(orderItem.getConfirmedAt()); // 발주 확인일시 추가
        
        // 배송 정보 추가 (nullable)
        if (orderItem.getDelivery() != null) {
            builder.deliveryNo(orderItem.getDelivery().getDeliveryNo())
                    .deliveryStatus(orderItem.getDelivery().getDeliveryStatus())
                    .deliveryStartDate(orderItem.getDelivery().getDeliveryStartDate())
                    .deliveryEndDate(orderItem.getDelivery().getDeliveryEndDate())
                    .deliveryTrackingNumber(orderItem.getDelivery().getDeliveryTrackingNumber())
                    .deliveryCourier(orderItem.getDelivery().getDeliveryCourier());
        }
        
        // 반품 정보 추가 (nullable)
        if (orderItem.getReturnEntity() != null) {
            builder.returnNo(orderItem.getReturnEntity().getReturnNo())
                    .returnStatus(orderItem.getReturnEntity().getReturnStatus())
                    .returnRequestedAt(orderItem.getReturnEntity().getReturnRequestedAt())
                    .returnReason(orderItem.getReturnEntity().getReturnReason())
                    .returnAmount(orderItem.getReturnEntity().getReturnAmount())
                    .returnTrackingNumber(orderItem.getReturnEntity().getReturnTrackingNumber())
                    .returnCourier(orderItem.getReturnEntity().getReturnCourier())
                    .rejectionReason(orderItem.getReturnEntity().getRejectionReason());
        }
        
        // 배송/반품 관련 연관 엔티티 접근 후 상태 계산 (지연 로딩 보장)
        builder.status(orderItem.getStatus()); // 주문 상품 상태 (computed 필드)
        
        return builder.build();
    }

    private Long resolvePartnerId(CartItemEntity cartItem) {
        if (cartItem.getOption() != null && cartItem.getOption().getPartner() != null) {
            return cartItem.getOption().getPartner().getPartnerId();
        }
        if (cartItem.getProduct() != null && cartItem.getProduct().getPartner() != null) {
            return cartItem.getProduct().getPartner().getPartnerId();
        }
        return null;
    }

    private String toCsv(List<Long> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private Long calculateDiscountAmount(SalePolicyResponseDto salePolicy, Long baseTotalPrice) {
        if (salePolicy == null || salePolicy.getDiscountType() == null || salePolicy.getDiscountValue() == null) {
            return 0L;
        }
        if (baseTotalPrice == null || baseTotalPrice <= 0) return 0L;

        DiscountType type = salePolicy.getDiscountType();
        Long value = salePolicy.getDiscountValue();
        Long maxDiscountAmount = salePolicy.getMaxDiscountAmount();

        long rawDiscount;
        if (type == DiscountType.PERCENT) {
            rawDiscount = baseTotalPrice * value / 100L;
        } else {
            // FIXED: value는 "해당 장바구니 아이템 총액(baseTotalPrice)에서 빼는 할인금액"으로 해석합니다.
            rawDiscount = value;
        }

        if (rawDiscount <= 0) return 0L;
        if (maxDiscountAmount != null && maxDiscountAmount > 0) {
            rawDiscount = Math.min(rawDiscount, maxDiscountAmount);
        }
        return Math.max(0L, Math.min(rawDiscount, baseTotalPrice));
    }
    
    /**
     * 세션에서 고객 ID 가져오기
     */
    private Long getCustomerIdFromSession(HttpSession session) {
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId"); // 통합 로그인(CUSTOMER) 시 subjectId
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return customerId;
    }
    
    /**
     * 세션에서 파트너 ID 가져오기
     */
    private Long getPartnerIdFromSession(HttpSession session) {
        Object subjObj = session.getAttribute("subjectId");
        Long partnerId = toLong(subjObj);
        
        if (partnerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return partnerId;
    }

    /**
     * 파트너가 활성화 상태인지 확인 (비활성화된 파트너는 관리 기능 사용 불가)
     */
    private void checkPartnerActive(Long partnerId) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PARTNER_INACTIVE);
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return (Long) value;
    }
    
    /**
     * 주문 요청 데이터 검증 (필수값 및 형식 체크)
     */
    private void validateOrderRequest(OrderCreateRequestDto requestDto) {
        // 수령인 이름 검증
        if (requestDto.getRecipientName() == null || requestDto.getRecipientName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "수령인 이름은 필수입니다.");
        }
        
        // 전화번호 검증 (하이픈 있든 없든 통과, 빈 문자열만 체크)
        if (requestDto.getRecipientPhone() == null || requestDto.getRecipientPhone().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "수령인 전화번호는 필수입니다.");
        }
        
        // 배송지 주소 검증
        if (requestDto.getDeliveryAddress() == null || requestDto.getDeliveryAddress().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "배송지 주소는 필수입니다.");
        }
        
        // 결제 방법 검증
        if (requestDto.getPaymentMethod() == null || requestDto.getPaymentMethod().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "결제 방법은 필수입니다.");
        }
    }
    
    /**
     * 전화번호 정규화 (하이픈 제거)
     * 예: "010-1234-5678" -> "01012345678"
     *     "010 1234 5678" -> "01012345678"
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        // 하이픈, 공백 제거
        return phone.replaceAll("[\\s-]", "");
    }
}
