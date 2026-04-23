package com.swimshop.swim_mall.delivery.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.delivery.dto.DeliveryResponseDto;
import com.swimshop.swim_mall.delivery.dto.DeliveryStartRequestDto;
import com.swimshop.swim_mall.delivery.dto.DeliveryUpdateRequestDto;
import com.swimshop.swim_mall.delivery.entity.DeliveryEntity;
import com.swimshop.swim_mall.delivery.repository.DeliveryRepository;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.common.enums.PartnerStatus;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthService authService;
    private final PartnerRepository partnerRepository;

    /**
     * 전체 배송 목록 조회 (관리자/파트너용)
     * GET /api/deliveries
     * - 관리자: 모든 배송 조회
     * - 파트너: 자신의 상품 배송만 조회
     */
    public List<DeliveryResponseDto> getAllDeliveries(HttpSession session) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        if (userRole == AccountRole.ADMIN) {
            // 관리자: 모든 배송 조회 (JOIN FETCH 사용)
            List<DeliveryEntity> deliveries = deliveryRepository.findAllWithOrderItem();
            return deliveries.stream()
                    .map(this::toDeliveryResponseDto)
                    .collect(Collectors.toList());
        } else if (userRole == AccountRole.PARTNER) {
            // 파트너: 자신의 상품 배송만 조회
            Long partnerId = getPartnerIdFromSession(session);
            List<DeliveryEntity> deliveries = deliveryRepository.findByPartnerId(partnerId);
            return deliveries.stream()
                    .map(this::toDeliveryResponseDto)
                    .collect(Collectors.toList());
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 접근 가능합니다.");
        }
    }

    /**
     * 주문 번호로 배송 목록 조회
     * GET /api/orders/{orderNo}/deliveries
     */
    public List<DeliveryResponseDto> getDeliveriesByOrderNo(HttpSession session, Long orderNo) {
        // 권한 확인: 고객, 관리자, 파트너 모두 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        List<DeliveryEntity> deliveries = deliveryRepository.findByOrderNo(orderNo);
        
        // 권한별 필터링
        if (userRole == AccountRole.CUSTOMER) {
            // 고객: 본인 주문만 조회 가능
            if (!deliveries.isEmpty()) {
                Long customerId = getCustomerIdFromSession(session);
                Long orderOwnerId = deliveries.get(0).getOrderItem().getOrder().getCustomer().getCustomerId();
                if (!orderOwnerId.equals(customerId)) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
        } else if (userRole == AccountRole.PARTNER) {
            // 파트너: 자신의 상품이 포함된 배송만 조회 가능
            Long partnerId = getPartnerIdFromSession(session);
            deliveries = deliveries.stream()
                    .filter(delivery -> {
                        OrderItemEntity orderItem = delivery.getOrderItem();
                        // Option이 있으면 Option의 Partner 확인
                        if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                            return orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
                        }
                        // Option이 없으면 Product의 Partner 확인
                        if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                            return orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }
        // 관리자(ADMIN): 모든 배송 조회 가능 (필터링 없음)
        
        return deliveries.stream()
                .map(this::toDeliveryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 주문 아이템 번호로 배송 조회
     * GET /api/order-items/{orderItemNo}/delivery
     */
    public DeliveryResponseDto getDeliveryByOrderItemNo(HttpSession session, Long orderItemNo) {
        // 권한 확인: 고객, 관리자, 파트너 모두 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        DeliveryEntity delivery = deliveryRepository.findByOrderItemNo(orderItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        
        // 권한별 검증
        if (userRole == AccountRole.CUSTOMER) {
            // 고객: 본인 주문만 조회 가능
            Long customerId = getCustomerIdFromSession(session);
            if (!delivery.getOrderItem().getOrder().getCustomer().getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else if (userRole == AccountRole.PARTNER) {
            // 파트너: 자신의 상품이 포함된 배송만 조회 가능
            Long partnerId = getPartnerIdFromSession(session);
            OrderItemEntity orderItem = delivery.getOrderItem();
            boolean hasPartnerProduct = false;
            
            // Option이 있으면 Option의 Partner 확인
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                hasPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            }
            // Option이 없으면 Product의 Partner 확인
            else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                hasPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!hasPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        // 관리자(ADMIN): 모든 배송 조회 가능 (검증 없음)
        
        return toDeliveryResponseDto(delivery);
    }

    /**
     * 배송 번호로 배송 조회
     * GET /api/deliveries/{deliveryNo}
     */
    public DeliveryResponseDto getDelivery(HttpSession session, Long deliveryNo) {
        // 권한 확인: 고객, 관리자, 파트너 모두 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        DeliveryEntity delivery = deliveryRepository.findById(deliveryNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        
        // 권한별 검증
        if (userRole == AccountRole.CUSTOMER) {
            // 고객: 본인 주문만 조회 가능
            Long customerId = getCustomerIdFromSession(session);
            if (!delivery.getOrderItem().getOrder().getCustomer().getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else if (userRole == AccountRole.PARTNER) {
            // 파트너: 자신의 상품이 포함된 배송만 조회 가능
            Long partnerId = getPartnerIdFromSession(session);
            OrderItemEntity orderItem = delivery.getOrderItem();
            boolean hasPartnerProduct = false;
            
            // Option이 있으면 Option의 Partner 확인
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                hasPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            }
            // Option이 없으면 Product의 Partner 확인
            else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                hasPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!hasPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        // 관리자(ADMIN): 모든 배송 조회 가능 (검증 없음)
        
        return toDeliveryResponseDto(delivery);
    }

    /**
     * 배송 시작 (READY → SHIPPED)
     * PATCH /api/deliveries/{deliveryNo}/start
     */
    @Transactional
    public DeliveryResponseDto startDelivery(HttpSession session, Long deliveryNo, DeliveryStartRequestDto requestDto) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        authService.requireRole(session, AccountRole.ADMIN, AccountRole.PARTNER);
        
        DeliveryEntity delivery = deliveryRepository.findById(deliveryNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        
        // 파트너는 자신의 상품 배송만 변경 가능
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 배송 관리 불가
            checkPartnerActive(partnerId);
            
            OrderItemEntity orderItem = delivery.getOrderItem();
            boolean hasPartnerProduct = false;
            
            // Option이 있으면 Option의 Partner 확인
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                hasPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            }
            // Option이 없으면 Product의 Partner 확인
            else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                hasPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!hasPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품 배송만 변경할 수 있습니다.");
            }
        }
        // 관리자(ADMIN): 모든 배송 변경 가능 (검증 없음)
        
        // 배송 시작
        delivery.startDelivery(
                requestDto.getTrackingNumber().trim(),
                requestDto.getCourier().trim()
        );
        
        delivery = deliveryRepository.save(delivery);
        return toDeliveryResponseDto(delivery);
    }

    /**
     * 배송 완료 (SHIPPED → DELIVERED)
     * PATCH /api/deliveries/{deliveryNo}/complete
     */
    @Transactional
    public DeliveryResponseDto completeDelivery(HttpSession session, Long deliveryNo) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        authService.requireRole(session, AccountRole.ADMIN, AccountRole.PARTNER);
        
        DeliveryEntity delivery = deliveryRepository.findById(deliveryNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        
        // 파트너는 자신의 상품 배송만 변경 가능
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 배송 관리 불가
            checkPartnerActive(partnerId);
            
            OrderItemEntity orderItem = delivery.getOrderItem();
            boolean hasPartnerProduct = false;
            
            // Option이 있으면 Option의 Partner 확인
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                hasPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            }
            // Option이 없으면 Product의 Partner 확인
            else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                hasPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!hasPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품 배송만 변경할 수 있습니다.");
            }
        }
        // 관리자(ADMIN): 모든 배송 변경 가능 (검증 없음)
        
        // 배송 완료
        delivery.completeDelivery();
        
        delivery = deliveryRepository.save(delivery);
        return toDeliveryResponseDto(delivery);
    }

    /**
     * 배송 정보 수정 (송장번호, 택배사)
     * PATCH /api/deliveries/{deliveryNo}
     */
    @Transactional
    public DeliveryResponseDto updateDelivery(HttpSession session, Long deliveryNo, DeliveryUpdateRequestDto requestDto) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        authService.requireRole(session, AccountRole.ADMIN, AccountRole.PARTNER);
        
        DeliveryEntity delivery = deliveryRepository.findById(deliveryNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        
        // 파트너는 자신의 상품 배송만 변경 가능
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 배송 관리 불가
            checkPartnerActive(partnerId);
            
            OrderItemEntity orderItem = delivery.getOrderItem();
            boolean hasPartnerProduct = false;
            
            // Option이 있으면 Option의 Partner 확인
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                hasPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            }
            // Option이 없으면 Product의 Partner 확인
            else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                hasPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!hasPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품 배송만 변경할 수 있습니다.");
            }
        }
        // 관리자(ADMIN): 모든 배송 변경 가능 (검증 없음)
        
        // 배송 정보 수정
        String trackingNumber = requestDto.getTrackingNumber() != null 
                ? requestDto.getTrackingNumber().trim() : null;
        String courier = requestDto.getCourier() != null 
                ? requestDto.getCourier().trim() : null;
        
        delivery.updateTrackingInfo(trackingNumber, courier);
        
        delivery = deliveryRepository.save(delivery);
        return toDeliveryResponseDto(delivery);
    }

    /**
     * DeliveryEntity를 DeliveryResponseDto로 변환
     */
    private DeliveryResponseDto toDeliveryResponseDto(DeliveryEntity delivery) {
        // orderItem이 null인 경우를 대비한 안전한 처리
        Long orderItemNo = null;
        Long orderNo = null;
        Long productNo = null;
        String productName = null;
        String productImageUrl = null;
        Long optionNo = null;
        String color = null;
        String size = null;
        Integer quantity = null;
        Long itemPrice = null;
        Long itemTotalPrice = null;
        
        if (delivery.getOrderItem() != null) {
            orderItemNo = delivery.getOrderItem().getOrderItemNo();
            quantity = delivery.getOrderItem().getItemQuantity();
            itemPrice = delivery.getOrderItem().getItemPrice();
            itemTotalPrice = delivery.getOrderItem().getItemTotalPrice();
            
            if (delivery.getOrderItem().getOrder() != null) {
                orderNo = delivery.getOrderItem().getOrder().getOrderNo();
            }
            
            // 상품 정보
            if (delivery.getOrderItem().getProduct() != null) {
                productNo = delivery.getOrderItem().getProduct().getProductNo();
                productName = delivery.getOrderItem().getProduct().getProductName();
                productImageUrl = delivery.getOrderItem().getProduct().getProductImageUrl();
            }
            
            // 옵션 정보 (nullable)
            if (delivery.getOrderItem().getOption() != null) {
                optionNo = delivery.getOrderItem().getOption().getOptionNo();
                color = delivery.getOrderItem().getOption().getColor();
                size = delivery.getOrderItem().getOption().getSize();
            }
        }
        
        return DeliveryResponseDto.builder()
                .deliveryNo(delivery.getDeliveryNo())
                .orderItemNo(orderItemNo)
                .orderNo(orderNo)
                .productNo(productNo)
                .productName(productName)
                .productImageUrl(productImageUrl)
                .optionNo(optionNo)
                .color(color)
                .size(size)
                .quantity(quantity)
                .itemPrice(itemPrice)
                .itemTotalPrice(itemTotalPrice)
                .deliveryStatus(delivery.getDeliveryStatus())
                .deliveryStartDate(delivery.getDeliveryStartDate())
                .deliveryEndDate(delivery.getDeliveryEndDate())
                .deliveryTrackingNumber(delivery.getDeliveryTrackingNumber())
                .deliveryCourier(delivery.getDeliveryCourier())
                .build();
    }

    /**
     * 세션에서 고객 ID 가져오기
     */
    private Long getCustomerIdFromSession(HttpSession session) {
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId");
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
}
