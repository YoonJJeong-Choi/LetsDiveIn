package com.swimshop.swim_mall.return_order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.enums.ReturnReasonType;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.return_order.dto.ReturnRequestDto;
import com.swimshop.swim_mall.return_order.dto.ReturnResponseDto;
import com.swimshop.swim_mall.return_order.dto.ReturnUpdateRequestDto;
import com.swimshop.swim_mall.return_order.dto.ReturnHistoryDto;
import com.swimshop.swim_mall.return_order.dto.ReturnAssistResponseDto;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnHistoryEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;
import com.swimshop.swim_mall.return_order.repository.ReturnRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnHistoryRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnImageRepository;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.customer.entity.CustomerActivityLogEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository;
import com.swimshop.swim_mall.common.enums.CustomerActivityType;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import com.swimshop.swim_mall.product.service.ProductCustomerImageUrlResolver;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnService {

    private static final Logger log = LoggerFactory.getLogger(ReturnService.class);

    private final ReturnRepository returnRepository;
    private final ReturnHistoryRepository returnHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthService authService;
    private final PartnerRepository partnerRepository;
    private final AdminRepository adminRepository;
    private final InventoryService inventoryService;
    private final com.swimshop.swim_mall.point.service.PointService pointService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomerActivityLogRepository customerActivityLogRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final ReturnImageRepository returnImageRepository;
    private final ReturnImageAnalysisService returnImageAnalysisService;
    private final ReturnCustomerHistoryInsightService returnCustomerHistoryInsightService;
    private final ProductCustomerImageUrlResolver productCustomerImageUrlResolver;

    @Value("${return.assist.enabled:true}")
    private boolean returnAssistEnabled;

    /**
     * 반품 신청 (고객용)
     * POST /api/returns
     */
    @Transactional
    public ReturnResponseDto requestReturn(HttpSession session, ReturnRequestDto requestDto) {
        // 고객만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.CUSTOMER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "고객만 반품 신청이 가능합니다.");
        }

        Long customerId = getCustomerIdFromSession(session);
        Long orderItemNo = requestDto.getOrderItemNo();

        // 주문 아이템 조회 (Delivery와 함께 로드)
        OrderItemEntity orderItem = orderItemRepository.findByIdWithDelivery(orderItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        // 본인 주문인지 확인
        if (!orderItem.getOrder().getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 주문만 반품 신청이 가능합니다.");
        }

        // 이미 반품 신청이 있는지 확인
        if (returnRepository.findByOrderItem_OrderItemNo(orderItemNo).isPresent()) {
            throw new BusinessException(ErrorCode.RETURN_ALREADY_EXISTS);
        }

        // 반품 신청 가능 여부 확인
        validateReturnEligibility(orderItem);

        validateReturnReasonForType(requestDto.getReturnReasonType(), requestDto.getReturnReason());

        List<Long> imageFileIds = requestDto.getImageFileIds() != null
                ? requestDto.getImageFileIds()
                : List.of();
        validateReturnImagesForType(requestDto.getReturnReasonType(), imageFileIds);

        String reasonToStore = normalizeReturnReason(requestDto.getReturnReason());

        // 반품 엔티티 생성
        ReturnEntity returnEntity = ReturnEntity.builder()
                .orderItem(orderItem)
                .returnStatus(ReturnStatus.REQUESTED)
                .returnRequestedAt(LocalDateTime.now())
                .returnReasonType(requestDto.getReturnReasonType())
                .returnReason(reasonToStore)
                .returnAmount(orderItem.getItemTotalPrice()) // 주문 아이템 총 가격을 반품 금액으로 설정
                .build();

        ReturnEntity savedReturn = returnRepository.save(returnEntity);

        // 반품 이미지 연결 (유형에 따라 생략 가능)
        for (Long fileId : imageFileIds) {
            UploadedFileEntity file = uploadedFileRepository.findById(fileId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 반품 이미지 파일 ID입니다: " + fileId));
            if (!"return-image".equals(file.getCategory())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "반품 이미지 카테고리(return-image) 파일만 사용할 수 있습니다.");
            }

            String imageUrl = Boolean.TRUE.equals(file.getIsPrivate())
                    ? String.format("%s/api/files/%d/download", "http://localhost:8080", file.getFileId())
                    : String.format("%s/uploads/%s/%s", "http://localhost:8080", file.getCategory(), file.getStoredName());

            ReturnImageEntity returnImage = ReturnImageEntity.builder()
                    .returnEntity(savedReturn)
                    .file(file)
                    .imageUrl(imageUrl)
                    .build();
            returnImageRepository.save(returnImage);
        }
        
        // 반품 신청 이력 기록 (고객이 신청하므로 관리자/파트너 없음)
        try {
            Map<String, Object> newValueMap = new HashMap<>();
            newValueMap.put("status", ReturnStatus.REQUESTED.name());
            newValueMap.put("returnReasonType", requestDto.getReturnReasonType().name());
            newValueMap.put("returnReason", reasonToStore);
            newValueMap.put("returnAmount", orderItem.getItemTotalPrice());
            newValueMap.put("imageFileIds", imageFileIds);
            
            String newValueJson = objectMapper.writeValueAsString(newValueMap);
            
            // 고객이 신청하므로 관리자/파트너 없이 이력 생성
            ReturnHistoryEntity history = ReturnHistoryEntity.createByCustomer(
                    savedReturn,
                    "CREATE",
                    null,
                    newValueJson,
                    "고객 반품 신청"
            );
            returnHistoryRepository.save(history);
        } catch (Exception e) {
            // 이력 기록 실패해도 반품 신청은 계속 진행
        }
        
        // 활동 로그 기록
        try {
            CustomerEntity customer = orderItem.getOrder().getCustomer();
            String activityDetails = objectMapper.writeValueAsString(Map.of(
                "returnNo", savedReturn.getReturnNo(),
                "orderNo", orderItem.getOrder().getOrderNo(),
                "productNo", orderItem.getProduct().getProductNo(),
                "productName", orderItem.getProduct().getProductName(),
                "returnAmount", savedReturn.getReturnAmount(),
                "returnReasonType", savedReturn.getReturnReasonType() != null ? savedReturn.getReturnReasonType().name() : "",
                "returnReason", savedReturn.getReturnReason() != null ? savedReturn.getReturnReason() : ""
            ));
            CustomerActivityLogEntity log = CustomerActivityLogEntity.create(
                customer,
                CustomerActivityType.RETURN_REQUESTED,
                activityDetails,
                null, // IP 주소는 나중에 추가 가능
                null  // User-Agent는 나중에 추가 가능
            );
            customerActivityLogRepository.save(log);
        } catch (Exception e) {
            // 로그 기록 실패해도 반품 신청은 계속 진행
        }
        
        return toReturnResponseDto(savedReturn);
    }

    /**
     * 반품 신청 가능 여부 확인
     * 
     * 반품 신청 가능 조건:
     * 1. 배송 완료된 상품 (DELIVERED)
     * 2. 배송 완료 후 7일 이내
     * 3. 구매 확정되지 않은 상품 (completedAt == null)
     * 4. 주문 상태가 ACTIVE (배송 완료 후에도 주문 상태는 ACTIVE 유지)
     * 
     * 즉, 배송 완료 후 7일 이내이며 구매 확정 전까지 반품 신청 가능
     */
    private void validateReturnEligibility(OrderItemEntity orderItem) {
        // 배송 완료 여부 확인: 배송이 완료되어야 반품 신청 가능
        if (orderItem.getDelivery() == null) {
            throw new BusinessException(ErrorCode.RETURN_NOT_ELIGIBLE, 
                    "배송 정보가 없는 상품은 반품 신청이 불가능합니다.");
        }
        
        if (orderItem.getDelivery().getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.RETURN_NOT_ELIGIBLE, 
                    String.format("배송 완료된 상품만 반품 신청이 가능합니다. 현재 배송 상태: %s", 
                            orderItem.getDelivery().getDeliveryStatus().getLabel()));
        }

        // 배송 완료 후 7일 이내만 반품 신청 가능
        LocalDateTime deliveryEndDate = orderItem.getDelivery().getDeliveryEndDate();
        if (deliveryEndDate == null) {
            throw new BusinessException(ErrorCode.RETURN_NOT_ELIGIBLE,
                    "배송 완료일이 확인되지 않아 반품 신청이 불가능합니다.");
        }

        LocalDateTime returnDeadline = deliveryEndDate.plusDays(7);
        if (LocalDateTime.now().isAfter(returnDeadline)) {
            throw new BusinessException(ErrorCode.RETURN_NOT_ELIGIBLE,
                    "반품 신청 가능 기간(배송 완료 후 7일)이 지났습니다.");
        }
        
        // 구매 확정된 주문 상품은 반품 신청 불가
        if (orderItem.getCompletedAt() != null) {
            throw new BusinessException(ErrorCode.RETURN_NOT_ELIGIBLE, 
                    "구매 확정된 주문 상품은 반품 신청이 불가능합니다.");
        }
        
        // 주문 상태 확인: ACTIVE 상태여야 함
        // 주의: 구매 확정은 OrderItem.completedAt으로 관리하므로 주문 상태와 무관
        // 배송 완료 후에도 주문 상태는 ACTIVE로 유지됨
        OrderStatus orderStatus = orderItem.getOrder().getOrderStatus();
        if (orderStatus != OrderStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.RETURN_NOT_ELIGIBLE, 
                    "반품 신청은 주문 진행중 상태에서만 가능합니다.");
        }
    }

    /**
     * 반품 목록 조회 (고객용)
     * GET /api/returns/customer
     */
    public List<ReturnResponseDto> getReturnsByCustomer(HttpSession session) {
        // 고객만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.CUSTOMER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Long customerId = getCustomerIdFromSession(session);
        List<ReturnEntity> returns = returnRepository.findByCustomerId(customerId);
        return returns.stream()
                .map(this::toReturnResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 반품 목록 조회 (관리자/파트너용)
     * GET /api/returns
     */
    public List<ReturnResponseDto> getAllReturns(HttpSession session) {
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        if (userRole == AccountRole.ADMIN) {
            // 관리자: 모든 반품 조회
            List<ReturnEntity> returns = returnRepository.findAllWithRelations();
            return returns.stream()
                    .map(this::toReturnResponseDto)
                    .collect(Collectors.toList());
        } else if (userRole == AccountRole.PARTNER) {
            // 파트너: 자신의 상품 반품만 조회
            Long partnerId = getPartnerIdFromSession(session);
            List<ReturnEntity> returns = returnRepository.findByPartnerId(partnerId);
            return returns.stream()
                    .map(this::toReturnResponseDto)
                    .collect(Collectors.toList());
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 접근 가능합니다.");
        }
    }

    /**
     * 반품 상세 조회
     * GET /api/returns/{returnNo}
     */
    public ReturnResponseDto getReturn(HttpSession session, Long returnNo) {
        ReturnEntity returnEntity = returnRepository.findById(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND));

        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        // 권한 확인
        if (userRole == AccountRole.CUSTOMER) {
            // 고객: 본인 반품만 조회 가능
            Long customerId = getCustomerIdFromSession(session);
            if (!returnEntity.getOrderItem().getOrder().getCustomer().getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else if (userRole == AccountRole.PARTNER) {
            // 파트너: 자신의 상품 반품만 조회 가능
            Long partnerId = getPartnerIdFromSession(session);
            OrderItemEntity orderItem = returnEntity.getOrderItem();
            boolean isPartnerProduct = false;
            
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                isPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            } else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                isPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!isPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        // 관리자(ADMIN): 모든 반품 조회 가능

        return toReturnResponseDto(returnEntity);
    }

    /**
     * 관리자 반품 검토 보조 (규칙 기반: 증빙·정책 체크리스트·고객 이력 참고)
     */
    @Transactional
    public ReturnAssistResponseDto getAdminReturnAssist(HttpSession session, Long returnNo) {
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        if (currentUser.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자만 반품 검토 보조를 사용할 수 있습니다.");
        }

        ReturnEntity targetReturn = returnRepository.findById(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND));

        if (!returnAssistEnabled) {
            ReturnAssistResponseDto disabled = buildDisabledReturnAssistResponse();
            recordReturnAssistAudit(targetReturn, currentUser, disabled);
            return disabled;
        }

        Long customerId = targetReturn.getOrderItem().getOrder().getCustomer().getCustomerId();
        List<ReturnEntity> customerReturns = returnRepository.findByCustomerId(customerId);
        ReturnCustomerHistoryInsightService.CustomerHistoryInsight historyInsight =
                returnCustomerHistoryInsightService.summarize(targetReturn, customerReturns);

        List<String> imageUrls = getReturnImageUrls(targetReturn);
        ReturnImageAnalysisService.EvidenceResult evidence = returnImageAnalysisService.analyze(
                imageUrls,
                targetReturn.getReturnReasonType(),
                targetReturn.getReturnReason());

        ReturnAssistResponseDto response = buildReturnAssistResponse(
                targetReturn,
                historyInsight,
                evidence);

        recordReturnAssistAudit(targetReturn, currentUser, response);
        return response;
    }

    /** @deprecated {@link #getAdminReturnAssist(HttpSession, Long)} */
    @Deprecated
    public ReturnAssistResponseDto getAdminReviewAssist(HttpSession session, Long returnNo) {
        return getAdminReturnAssist(session, returnNo);
    }

    /** @deprecated {@link #getAdminReturnAssist(HttpSession, Long)} */
    @Deprecated
    public ReturnAssistResponseDto getAdminAiAssist(HttpSession session, Long returnNo) {
        return getAdminReturnAssist(session, returnNo);
    }

    /**
     * 반품 상태 변경 (관리자/파트너용)
     * PATCH /api/returns/{returnNo}
     */
    @Transactional
    public ReturnResponseDto updateReturnStatus(HttpSession session, Long returnNo, ReturnUpdateRequestDto requestDto) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.ADMIN && userRole != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 반품 상태 변경이 가능합니다.");
        }

        ReturnEntity returnEntity = returnRepository.findById(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND));

        // 파트너 권한 확인
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 반품 관리 불가
            checkPartnerActive(partnerId);
            
            OrderItemEntity orderItem = returnEntity.getOrderItem();
            boolean isPartnerProduct = false;
            
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                isPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            } else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                isPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!isPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        // 상태 변경
        ReturnStatus oldStatus = returnEntity.getReturnStatus();
        String oldTrackingNumber = returnEntity.getReturnTrackingNumber();
        String oldCourier = returnEntity.getReturnCourier();
        
        ReturnStatus newStatus = requestDto.getReturnStatus();
        if (newStatus != null) {
            // 상태 전환 검증
            validateStatusTransition(oldStatus, newStatus);
            
            // APPROVED 상태로 변경 시
            if (newStatus == ReturnStatus.APPROVED) {
                returnEntity.approve();
            }
            // REJECTED 상태로 변경 시 (거절 사유는 DTO에 없으면 기본 메시지)
            else if (newStatus == ReturnStatus.REJECTED) {
                String rejectionReason = requestDto.getRejectionReason() != null 
                    ? requestDto.getRejectionReason().trim() 
                    : (returnEntity.getReturnStatus() == ReturnStatus.PICKUP_COMPLETED 
                        ? "상품 확인 결과 반품 불가" 
                        : "반품 거절");
                returnEntity.reject(rejectionReason);
            }
            // 기타 상태 변경 (PICKUP_COMPLETED, REFUNDED)
            else {
                // 송장번호/택배사 변경 여부 확인
                boolean trackingChanged = (requestDto.getReturnTrackingNumber() != null 
                        && !requestDto.getReturnTrackingNumber().equals(oldTrackingNumber))
                    || (requestDto.getReturnCourier() != null 
                        && !requestDto.getReturnCourier().equals(oldCourier));
                
                returnEntity.updateStatus(newStatus, requestDto.getReturnTrackingNumber(), requestDto.getReturnCourier());
                
                // 환불 완료 시 재고 복구 및 포인트 복구
                if (newStatus == ReturnStatus.REFUNDED) {
                    restoreInventoryForReturn(returnEntity.getOrderItem());
                    
                    // 포인트 복구 (환불 완료 시)
                    try {
                        OrderItemEntity orderItem = returnEntity.getOrderItem();
                        CustomerEntity customer = orderItem.getOrder().getCustomer();
                        Long orderNo = orderItem.getOrder().getOrderNo();
                        pointService.restorePoint(customer, orderNo, "반품/환불 완료 복구");
                    } catch (Exception e) {
                        // 포인트 복구 실패는 로그만 남기고 계속 진행
                        // 환불 완료는 성공 처리
                    }
                }
                
                // 송장번호/택배사만 변경된 경우 별도 이력 기록
                if (trackingChanged && oldStatus == newStatus) {
                    recordReturnHistory(returnEntity, userRole, session, "TRACKING_UPDATE", 
                            oldStatus, oldTrackingNumber, oldCourier,
                            newStatus, requestDto.getReturnTrackingNumber(), requestDto.getReturnCourier(),
                            null);
                }
            }
            
            returnEntity = returnRepository.save(returnEntity);
            
            // 상태 변경 이력 기록
            if (newStatus != oldStatus) {
                recordReturnHistory(returnEntity, userRole, session, "STATUS_CHANGE",
                        oldStatus, oldTrackingNumber, oldCourier,
                        newStatus, returnEntity.getReturnTrackingNumber(), returnEntity.getReturnCourier(),
                        requestDto.getRejectionReason());
            }
        }

        return toReturnResponseDto(returnEntity);
    }

    /**
     * 반품 승인 (관리자/파트너용)
     * POST /api/returns/{returnNo}/approve
     */
    @Transactional
    public ReturnResponseDto approveReturn(HttpSession session, Long returnNo) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.ADMIN && userRole != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 반품 승인이 가능합니다.");
        }

        ReturnEntity returnEntity = returnRepository.findById(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND));

        // 파트너 권한 확인
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 반품 관리 불가
            checkPartnerActive(partnerId);
            
            OrderItemEntity orderItem = returnEntity.getOrderItem();
            boolean isPartnerProduct = false;
            
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                isPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            } else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                isPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!isPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        // 상태 검증: REQUESTED 상태만 승인 가능
        if (returnEntity.getReturnStatus() != ReturnStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "반품 신청 상태인 반품만 승인할 수 있습니다. 현재 상태: " + returnEntity.getReturnStatus().getLabel());
        }

        // 반품 승인
        ReturnStatus oldStatus = returnEntity.getReturnStatus();
        returnEntity.approve();
        returnEntity = returnRepository.save(returnEntity);
        
        // 반품 승인 이력 기록
        recordReturnHistory(returnEntity, userRole, session, "APPROVE",
                oldStatus, null, null,
                ReturnStatus.APPROVED, null, null,
                null);

        return toReturnResponseDto(returnEntity);
    }

    /**
     * 반품 거절 (관리자/파트너용)
     * POST /api/returns/{returnNo}/reject
     * - REQUESTED 상태: 신청 단계에서 거절 (예: 반품 기간 초과 등)
     * - PICKUP_COMPLETED 상태: 수거 후 상품 확인 결과 이상으로 거절
     * 
     * 주의: APPROVED 상태에서는 거절 불가 (승인 = 반품을 받겠다는 의미이므로 수거 진행해야 함)
     */
    @Transactional
    public ReturnResponseDto rejectReturn(HttpSession session, Long returnNo, String rejectionReason) {
        // 관리자/파트너만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.ADMIN && userRole != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 반품 거절이 가능합니다.");
        }

        ReturnEntity returnEntity = returnRepository.findById(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND));

        // 파트너 권한 확인
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            
            // 비활성화된 파트너는 반품 관리 불가
            checkPartnerActive(partnerId);
            
            OrderItemEntity orderItem = returnEntity.getOrderItem();
            boolean isPartnerProduct = false;
            
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                isPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            } else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                isPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!isPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        // 상태 검증: REQUESTED 또는 PICKUP_COMPLETED 상태만 거절 가능
        // - REQUESTED: 반품 신청 단계에서 거절 (예: 반품 기간 초과 등)
        // - PICKUP_COMPLETED: 수거 후 상품 확인 결과 이상으로 거절
        // - APPROVED 상태에서는 거절 불가 (승인 = 반품을 받겠다는 의미이므로 수거 진행해야 함)
        ReturnStatus currentStatus = returnEntity.getReturnStatus();
        if (currentStatus != ReturnStatus.REQUESTED && currentStatus != ReturnStatus.PICKUP_COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "반품 신청 또는 수거 완료 상태인 반품만 거절할 수 있습니다. 현재 상태: " + currentStatus.getLabel());
        }

        // 거절 사유 설정
        String finalRejectionReason;
        if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            finalRejectionReason = rejectionReason.trim();
        } else {
            // 기본 거절 사유
            if (currentStatus == ReturnStatus.PICKUP_COMPLETED) {
                finalRejectionReason = "상품 확인 결과 반품 불가 (상품 상태 불량)";
            } else {
                finalRejectionReason = "반품 거절";
            }
        }

        // 반품 거절
        ReturnStatus oldStatus = returnEntity.getReturnStatus();
        returnEntity.reject(finalRejectionReason);
        returnEntity = returnRepository.save(returnEntity);
        
        // 반품 거절 이력 기록
        recordReturnHistory(returnEntity, userRole, session, "REJECT",
                oldStatus, null, null,
                ReturnStatus.REJECTED, null, null,
                finalRejectionReason);

        return toReturnResponseDto(returnEntity);
    }

    /**
     * 반품으로 인한 재고 복구
     * 환불 완료 시 호출됨
     */
    private void restoreInventoryForReturn(OrderItemEntity orderItem) {
        try {
            Long optionNo = orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null;
            Long productNo = orderItem.getProduct().getProductNo();
            Integer quantity = orderItem.getItemQuantity();
            
            inventoryService.restoreStockForOrder(optionNo, productNo, quantity);
        } catch (Exception e) {
            // 재고 복구 실패는 로그만 남기고 계속 진행 (옵션/상품이 삭제된 경우 등)
            // 반품 처리는 성공 처리
            // log.warn("Failed to restore stock for return order item {}: {}", orderItem.getOrderItemNo(), e.getMessage());
        }
    }

    /**
     * 반품 상태 전환 검증
     */
    private void validateStatusTransition(ReturnStatus currentStatus, ReturnStatus newStatus) {
        switch (currentStatus) {
            case REQUESTED:
                // 반품 신청 → 승인 또는 거절만 가능
                if (newStatus != ReturnStatus.APPROVED && newStatus != ReturnStatus.REJECTED) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                            "반품 신청 상태에서는 승인 또는 거절만 가능합니다.");
                }
                break;
            case APPROVED:
                // 반품 승인 → 수거 완료만 가능
                // 승인 = 반품을 받겠다는 의미이므로 수거 진행해야 함
                if (newStatus != ReturnStatus.PICKUP_COMPLETED) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                            "반품 승인 상태에서는 수거 완료로만 변경 가능합니다.");
                }
                break;
            case REJECTED:
                // 반품 거절 → 변경 불가
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                        "반품 거절 상태에서는 더 이상 변경할 수 없습니다.");
            case PICKUP_COMPLETED:
                // 수거 완료 → 환불 완료 또는 거절 가능 (상품 확인 후)
                // 상품이 정상이면 → REFUNDED (환불 완료)
                // 상품에 이상이 있으면 → REJECTED (반품 거절)
                if (newStatus != ReturnStatus.REFUNDED && newStatus != ReturnStatus.REJECTED) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                            "수거 완료 상태에서는 환불 완료 또는 반품 거절만 가능합니다.");
                }
                break;
            case REFUNDED:
                // 환불 완료 → 변경 불가
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                        "환불 완료 상태에서는 더 이상 변경할 수 없습니다.");
        }
    }

    /**
     * 반품 변경 이력 기록
     */
    private void recordReturnHistory(
            ReturnEntity returnEntity,
            AccountRole userRole,
            HttpSession session,
            String actionType,
            ReturnStatus oldStatus,
            String oldTrackingNumber,
            String oldCourier,
            ReturnStatus newStatus,
            String newTrackingNumber,
            String newCourier,
            String reason
    ) {
        try {
            Map<String, Object> oldValueMap = new HashMap<>();
            Map<String, Object> newValueMap = new HashMap<>();
            
            if (oldStatus != null) {
                oldValueMap.put("status", oldStatus.name());
            }
            if (oldTrackingNumber != null) {
                oldValueMap.put("trackingNumber", oldTrackingNumber);
            }
            if (oldCourier != null) {
                oldValueMap.put("courier", oldCourier);
            }
            
            if (newStatus != null) {
                newValueMap.put("status", newStatus.name());
            }
            if (newTrackingNumber != null) {
                newValueMap.put("trackingNumber", newTrackingNumber);
            }
            if (newCourier != null) {
                newValueMap.put("courier", newCourier);
            }
            
            String oldValueJson = oldValueMap.isEmpty() ? null : objectMapper.writeValueAsString(oldValueMap);
            String newValueJson = newValueMap.isEmpty() ? null : objectMapper.writeValueAsString(newValueMap);
            
            ReturnHistoryEntity history;
            AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
            
            if (userRole == AccountRole.ADMIN) {
                Long adminId = currentUser.getSubjectId();
                AdminEntity admin = adminRepository.findById(adminId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
                history = ReturnHistoryEntity.createByAdmin(
                        returnEntity,
                        admin,
                        actionType,
                        oldValueJson,
                        newValueJson,
                        reason
                );
            } else if (userRole == AccountRole.PARTNER) {
                Long partnerId = currentUser.getSubjectId();
                PartnerEntity partner = partnerRepository.findById(partnerId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
                history = ReturnHistoryEntity.createByPartner(
                        returnEntity,
                        partner,
                        actionType,
                        oldValueJson,
                        newValueJson,
                        reason
                );
            } else {
                // 고객인 경우 이력 기록 안 함 (이미 CREATE에서 기록됨)
                return;
            }
            
            returnHistoryRepository.save(history);
        } catch (Exception e) {
            // 이력 기록 실패해도 반품 처리는 계속 진행
            // 로그만 남기고 예외는 던지지 않음
        }
    }
    
    /**
     * 반품 변경 이력 조회 (관리자/파트너용)
     */
    public List<ReturnHistoryDto> getReturnHistory(HttpSession session, Long returnNo) {
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        
        // 반품 조회 및 권한 확인
        ReturnEntity returnEntity = returnRepository.findById(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND));
        
        // 권한 확인
        if (userRole == AccountRole.PARTNER) {
            Long partnerId = getPartnerIdFromSession(session);
            OrderItemEntity orderItem = returnEntity.getOrderItem();
            boolean isPartnerProduct = false;
            
            if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
                isPartnerProduct = orderItem.getOption().getPartner().getPartnerId().equals(partnerId);
            } else if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
                isPartnerProduct = orderItem.getProduct().getPartner().getPartnerId().equals(partnerId);
            }
            
            if (!isPartnerProduct) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else if (userRole != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 반품 이력을 조회할 수 있습니다.");
        }
        
        // 이력 조회
        List<ReturnHistoryEntity> histories = returnHistoryRepository.findByReturnNoOrderByChangedAtDesc(returnNo);
        
        return histories.stream()
                .map(this::toReturnHistoryDto)
                .collect(Collectors.toList());
    }
    
    /**
     * ReturnHistoryEntity를 ReturnHistoryDto로 변환
     */
    private ReturnHistoryDto toReturnHistoryDto(ReturnHistoryEntity history) {
        ReturnHistoryDto.ReturnHistoryDtoBuilder builder = ReturnHistoryDto.builder()
                .historyId(history.getHistoryId())
                .returnNo(history.getReturnEntity().getReturnNo())
                .changedAt(history.getChangedAt())
                .actionType(history.getActionType())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .reason(history.getReason());
        
        if (history.getAdmin() != null) {
            builder.adminId(history.getAdmin().getAdminId())
                   .adminName(history.getAdmin().getAdminName());
        }
        
        if (history.getPartner() != null) {
            builder.partnerId(history.getPartner().getPartnerId())
                   .partnerName(history.getPartner().getPartnerName());
        }
        
        return builder.build();
    }

    private void recordReturnAssistAudit(ReturnEntity returnEntity, AuthLoginResponseDto actor, ReturnAssistResponseDto response) {
        try {
            AdminEntity admin = adminRepository.findById(actor.getSubjectId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

            Map<String, Object> payload = new HashMap<>();
            payload.put("actorRole", actor.getRole() != null ? actor.getRole().name() : "UNKNOWN");
            payload.put("actorId", actor.getSubjectId());
            payload.put("summary", response.getSummary());
            payload.put("reviewPriority", response.getReviewPriority());
            payload.put("evidenceStatus", response.getEvidenceStatus());
            payload.put("checkPoints", safeList(response.getCheckPoints()));
            payload.put("signals", response.getSignals());

            String newValueJson = objectMapper.writeValueAsString(payload);
            ReturnHistoryEntity history = ReturnHistoryEntity.createByAdmin(
                    returnEntity,
                    admin,
                    "RETURN_ASSIST",
                    null,
                    newValueJson,
                    "관리자 반품 검토 보조 조회");
            returnHistoryRepository.save(history);
        } catch (Exception ignored) {
            // 감사로그 실패가 관리자 조회를 막지 않도록 무시
        }
    }

    private static <T> List<T> safeList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private ReturnAssistResponseDto buildDisabledReturnAssistResponse() {
        return ReturnAssistResponseDto.builder()
                .summary("검토 보조 기능이 비활성화되어 있습니다. 주문·배송·반품 이력을 기본 절차대로 확인하세요.")
                .reviewPriority(ReturnAssistResponseDto.ReviewPriority.builder()
                        .code("MANUAL")
                        .label("수동 검토")
                        .reason("자동 보조 없이 주문, 배송, 반품 이력을 기본 절차대로 확인해야 합니다.")
                        .build())
                .evidenceStatus(ReturnAssistResponseDto.EvidenceStatus.builder()
                        .code("NOT_ANALYZED")
                        .label("자동 정리 안 함")
                        .detail("검토 보조가 꺼져 있어 증빙 상태를 자동 정리하지 않았습니다.")
                        .build())
                .checkPoints(List.of("주문, 배송, 반품 이력을 기본 절차에 따라 확인하세요."))
                .signals(ReturnAssistResponseDto.Signals.builder()
                        .customerSignals(List.of())
                        .build())
                .build();
    }

    private ReturnAssistResponseDto buildReturnAssistResponse(
            ReturnEntity targetReturn,
            ReturnCustomerHistoryInsightService.CustomerHistoryInsight historyInsight,
            ReturnImageAnalysisService.EvidenceResult evidence) {
        List<String> insightCodes = historyInsight != null ? safeList(historyInsight.insightCodes()) : List.of();
        List<String> evidenceTags = evidence != null ? safeList(evidence.evidenceTags()) : List.of();
        List<String> evidenceGaps = evidence != null ? safeList(evidence.evidenceGaps()) : List.of();

        ReturnAssistResponseDto.EvidenceStatus evidenceStatus = buildEvidenceStatus(
                targetReturn.getReturnReasonType(),
                evidenceTags,
                evidenceGaps);
        ReturnAssistResponseDto.ReviewPriority reviewPriority = buildReviewPriority(
                targetReturn.getReturnReasonType(),
                evidenceGaps);
        ReturnAssistResponseDto.Signals signals = buildSignals(insightCodes);

        return ReturnAssistResponseDto.builder()
                .summary(buildSummary(targetReturn.getReturnReasonType(), reviewPriority, evidenceStatus))
                .reviewPriority(reviewPriority)
                .evidenceStatus(evidenceStatus)
                .checkPoints(buildCheckPoints(targetReturn.getReturnReasonType(), evidenceGaps))
                .signals(signals)
                .build();
    }

    private ReturnAssistResponseDto.EvidenceStatus buildEvidenceStatus(
            ReturnReasonType reasonType,
            List<String> evidenceTags,
            List<String> evidenceGaps) {
        if (reasonType == ReturnReasonType.CHANGE_OF_MIND || reasonType == ReturnReasonType.ORDER_MISTAKE) {
            return ReturnAssistResponseDto.EvidenceStatus.builder()
                    .code("NOT_REQUIRED")
                    .label("증빙 필수 아님")
                    .detail(buildPolicyReviewDetail(reasonType))
                    .build();
        }
        boolean imageRequired = isImageEvidenceImportant(reasonType);
        boolean missingRequiredImage = containsAny(evidenceGaps, "REQUIRED_IMAGE_MISSING", "NO_IMAGE_EVIDENCE");

        if (imageRequired && missingRequiredImage) {
            return ReturnAssistResponseDto.EvidenceStatus.builder()
                    .code("NEEDS_MORE_EVIDENCE")
                    .label("증빙 보완 필요")
                    .detail("불량·오배송 계열 사유인데 핵심 이미지가 부족해 추가 자료 요청 후 판단하는 편이 안전합니다.")
                    .build();
        }
        if (!evidenceGaps.isEmpty()) {
            return ReturnAssistResponseDto.EvidenceStatus.builder()
                    .code("PARTIAL")
                    .label("일부 보완 필요")
                    .detail(buildPartialEvidenceDetail(evidenceGaps))
                    .build();
        }
        if (!evidenceTags.isEmpty()) {
            return ReturnAssistResponseDto.EvidenceStatus.builder()
                    .code("SUFFICIENT")
                    .label("기본 증빙 확보")
                    .detail("현재 등록된 이미지와 사유 설명으로 1차 검토는 진행할 수 있습니다.")
                    .build();
        }
        return ReturnAssistResponseDto.EvidenceStatus.builder()
                .code("LIMITED")
                .label("증빙 정보 제한적")
                .detail("확인 가능한 이미지나 상세 설명이 충분하지 않아 수동 검토가 필요합니다.")
                .build();
    }

    private String buildPartialEvidenceDetail(List<String> evidenceGaps) {
        if (containsAny(evidenceGaps, "ADDITIONAL_IMAGE_RECOMMENDED")) {
            return "사진이 1장만 등록되어 있습니다. 내용이 충분하면 그대로 검토하시고, 필요할 때만 추가 촬영을 요청하세요.";
        }
        if (containsAny(evidenceGaps, "REASON_TEXT_TOO_SHORT")) {
            return "고객 상세 사유가 짧아 증빙과 함께 설명 보완을 받는 것이 좋습니다.";
        }
        return "현재 자료만으로도 일부 판단은 가능하지만 보완 자료가 있으면 검토 정확도가 높아집니다.";
    }

    private ReturnAssistResponseDto.ReviewPriority buildReviewPriority(
            ReturnReasonType reasonType,
            List<String> evidenceGaps) {
        if (reasonType == ReturnReasonType.CHANGE_OF_MIND || reasonType == ReturnReasonType.ORDER_MISTAKE) {
            return ReturnAssistResponseDto.ReviewPriority.builder()
                    .code("LOW")
                    .label("정책 확인 중심")
                    .reason("이미지 판정보다 반품 가능 조건과 상품 상태 확인이 우선인 건입니다.")
                    .build();
        }
        boolean imageRequired = isImageEvidenceImportant(reasonType);
        boolean criticalEvidenceGap = imageRequired && containsAny(evidenceGaps, "REQUIRED_IMAGE_MISSING", "NO_IMAGE_EVIDENCE");
        if (criticalEvidenceGap) {
            return ReturnAssistResponseDto.ReviewPriority.builder()
                    .code("HIGH")
                    .label("우선 검토")
                    .reason("현재 주장 확인에 필요한 핵심 증빙이 비어 있어 먼저 자료 보완이 필요한 건입니다.")
                    .build();
        }
        if (!evidenceGaps.isEmpty() || reasonType == ReturnReasonType.DEFECT || reasonType == ReturnReasonType.WRONG_ITEM) {
            return ReturnAssistResponseDto.ReviewPriority.builder()
                    .code("MEDIUM")
                    .label("추가 확인 권장")
                    .reason("현재 건의 주장과 증빙, 주문 정보를 함께 대조하며 확인하는 편이 좋습니다.")
                    .build();
        }
        return ReturnAssistResponseDto.ReviewPriority.builder()
                .code("LOW")
                .label("일반 검토")
                .reason("현재 자료 기준으로 기본 절차에 따라 1차 검토를 진행할 수 있습니다.")
                .build();
    }

    private String buildSummary(
            ReturnReasonType reasonType,
            ReturnAssistResponseDto.ReviewPriority reviewPriority,
            ReturnAssistResponseDto.EvidenceStatus evidenceStatus) {
        String reasonLabel = getReturnReasonTypeLabel(reasonType);
        if (reasonType == ReturnReasonType.CHANGE_OF_MIND) {
            return "고객은 단순 변심 반품을 요청하고 있습니다. "
                    + "이미지 증빙 판단보다 반품 가능 기간, 사용 흔적, 구성품 및 포장 상태 확인이 더 중요한 건입니다. "
                    + "정책 기준과 환불 조건을 먼저 확인하세요.";
        }
        if (reasonType == ReturnReasonType.ORDER_MISTAKE) {
            return "고객은 주문 실수 성격의 반품을 요청하고 있습니다. "
                    + "이미지 판정보다 실제 주문 옵션과 상품 상태, 재판매 가능 여부를 확인하는 것이 핵심입니다. "
                    + "정책상 처리 가능 조건을 먼저 검토하세요.";
        }

        String claimSentence = "고객은 " + reasonLabel + "을 주장하고 있습니다.";
        String evidenceSentence = evidenceStatus.getDetail();
        String nextSentence;
        if ("HIGH".equals(reviewPriority.getCode())) {
            nextSentence = "추가 자료를 먼저 보완받은 뒤 승인/거절 판단으로 넘어가는 편이 안전합니다.";
        } else if ("MEDIUM".equals(reviewPriority.getCode())) {
            nextSentence = "현재 자료와 주문 정보를 함께 대조하며 확인 포인트를 순서대로 검토하세요.";
        } else {
            nextSentence = "현재 자료 기준으로 기본 절차에 따라 1차 검토를 진행할 수 있습니다.";
        }
        return claimSentence + " " + evidenceSentence + " " + nextSentence;
    }

    private List<String> buildCheckPoints(
            ReturnReasonType reasonType,
            List<String> evidenceGaps) {
        List<String> checkPoints = new ArrayList<>();
        boolean imageRequired = isImageEvidenceImportant(reasonType);

        if (reasonType == ReturnReasonType.CHANGE_OF_MIND) {
            addIfAbsent(checkPoints, "반품 가능 기간 확인");
            addIfAbsent(checkPoints, "사용 흔적·구성품 상태 확인");
            addIfAbsent(checkPoints, "배송비 차감 기준 확인");
            return limitList(checkPoints, 3);
        }
        if (reasonType == ReturnReasonType.ORDER_MISTAKE) {
            addIfAbsent(checkPoints, "주문 옵션 일치 여부 확인");
            addIfAbsent(checkPoints, "재판매 가능 상태 확인");
            addIfAbsent(checkPoints, "처리 기준 확인");
            return limitList(checkPoints, 3);
        }
        if (imageRequired && containsAny(evidenceGaps, "REQUIRED_IMAGE_MISSING", "NO_IMAGE_EVIDENCE")) {
            addIfAbsent(checkPoints, "주문 상품과 촬영 상품 일치 여부 확인");
        }
        if (containsAny(evidenceGaps, "ADDITIONAL_IMAGE_RECOMMENDED")) {
            addIfAbsent(checkPoints, "추가 촬영 요청 필요 여부 확인");
        }
        if (containsAny(evidenceGaps, "REASON_TEXT_TOO_SHORT")) {
            addIfAbsent(checkPoints, "상세 사유와 증빙 일치 여부 확인");
        }
        addIfAbsent(checkPoints, "주문 옵션·배송 상태 일치 여부 확인");
        if (checkPoints.isEmpty()) {
            addIfAbsent(checkPoints, "주문 정보와 상품 상태 확인");
        }
        return limitList(checkPoints, 3);
    }

    private String buildPolicyReviewDetail(ReturnReasonType reasonType) {
        if (reasonType == ReturnReasonType.CHANGE_OF_MIND) {
            return "단순 변심은 이미지 증빙보다 반품 가능 기간, 사용 흔적, 구성품 및 포장 상태 확인이 중요합니다.";
        }
        if (reasonType == ReturnReasonType.ORDER_MISTAKE) {
            return "주문 실수는 이미지 판정보다 실제 주문 옵션과 상품 상태, 재판매 가능 여부 확인이 중요합니다.";
        }
        return "정책 기준 확인이 우선인 건입니다.";
    }

    private ReturnAssistResponseDto.Signals buildSignals(List<String> insightCodes) {
        List<String> customerSignals = new ArrayList<>();
        for (String factor : insightCodes) {
            String label = getCustomerHistoryReferenceLabel(factor);
            if (!label.isBlank()) {
                addIfAbsent(customerSignals, label);
            }
        }

        return ReturnAssistResponseDto.Signals.builder()
                .customerSignals(limitList(customerSignals, 2))
                .build();
    }

    private String getCustomerHistoryReferenceLabel(String code) {
        if (code == null) {
            return "";
        }
        return switch (code) {
            case "RECENT_RETURN_FREQUENCY_HIGH" -> "최근 90일 반품 이력이 많은 편입니다.";
            case "RECENT_RETURN_FREQUENCY" -> "최근 90일 반품 이력이 있습니다.";
            case "SAME_ADDRESS_REPEAT_HIGH" -> "동일 배송지 기준 반복 반품 이력이 다수 있습니다.";
            case "SAME_ADDRESS_REPEAT" -> "동일 배송지 기준 반복 반품 이력이 있습니다.";
            case "REJECTED_HISTORY_HIGH" -> "과거 반려 이력이 다수 있습니다.";
            case "REJECTED_HISTORY" -> "과거 반려 이력이 있습니다.";
            case "DISPUTE_LIKE_HISTORY" -> "과거 분쟁성 처리 이력이 확인됩니다.";
            case "HIGH_RETURN_AMOUNT" -> "고액 반품 건입니다.";
            case "NO_SPECIAL_HISTORY" -> "";
            default -> "";
        };
    }

    private String getReturnReasonTypeLabel(ReturnReasonType reasonType) {
        if (reasonType == null) {
            return "기타";
        }
        return switch (reasonType) {
            case CHANGE_OF_MIND -> "단순 변심";
            case ORDER_MISTAKE -> "주문 실수";
            case DEFECT -> "상품 불량·하자";
            case WRONG_ITEM -> "오배송";
            case OTHER -> "기타";
        };
    }

    private boolean isImageEvidenceImportant(ReturnReasonType reasonType) {
        return reasonType == ReturnReasonType.DEFECT
                || reasonType == ReturnReasonType.WRONG_ITEM
                || reasonType == ReturnReasonType.OTHER;
    }

    private boolean containsAny(List<String> values, String... targets) {
        List<String> safeValues = safeList(values);
        for (String target : targets) {
            if (safeValues.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private void addIfAbsent(List<String> target, String value) {
        if (value == null || value.isBlank() || target.contains(value)) {
            return;
        }
        target.add(value);
    }

    private List<String> limitList(List<String> values, int maxSize) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(maxSize)
                .collect(Collectors.toList());
    }

    /**
     * ReturnEntity를 ReturnResponseDto로 변환
     */
    private ReturnResponseDto toReturnResponseDto(ReturnEntity returnEntity) {
        OrderItemEntity orderItem = returnEntity.getOrderItem();
        List<String> imageUrls = getReturnImageUrls(returnEntity);
        
        return ReturnResponseDto.builder()
                .returnNo(returnEntity.getReturnNo())
                .orderItemNo(orderItem.getOrderItemNo())
                .orderNo(orderItem.getOrder().getOrderNo())
                .productNo(orderItem.getProduct().getProductNo())
                .productName(orderItem.getProduct().getProductName())
                .productImageUrl(productCustomerImageUrlResolver.resolveDisplayUrlOrEmpty(orderItem.getProduct()))
                .optionNo(orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null)
                .color(orderItem.getOption() != null ? orderItem.getOption().getColor() : null)
                .size(orderItem.getOption() != null ? orderItem.getOption().getSize() : null)
                .quantity(orderItem.getItemQuantity())
                .itemPrice(orderItem.getItemPrice())
                .itemTotalPrice(orderItem.getItemTotalPrice())
                .returnStatus(returnEntity.getReturnStatus())
                .returnRequestedAt(returnEntity.getReturnRequestedAt())
                .returnReasonType(returnEntity.getReturnReasonType())
                .returnReason(returnEntity.getReturnReason())
                .returnAmount(returnEntity.getReturnAmount())
                .returnTrackingNumber(returnEntity.getReturnTrackingNumber())
                .returnCourier(returnEntity.getReturnCourier())
                .rejectionReason(returnEntity.getRejectionReason())
                .imageUrls(imageUrls)
                .build();
    }

    private List<String> getReturnImageUrls(ReturnEntity returnEntity) {
        try {
            return returnImageRepository.findByReturnEntityOrderByIdAsc(returnEntity).stream()
                    .map(ReturnImageEntity::getImageUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
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

    private static String normalizeReturnReason(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private void validateReturnImagesForType(ReturnReasonType type, List<Long> imageFileIds) {
        boolean required = switch (type) {
            case DEFECT, WRONG_ITEM, OTHER -> true;
            case CHANGE_OF_MIND, ORDER_MISTAKE -> false;
        };
        if (required && imageFileIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "선택하신 반품 사유는 증빙 이미지를 최소 1장 업로드해주세요.");
        }
    }

    private void validateReturnReasonForType(ReturnReasonType type, String raw) {
        String text = raw != null ? raw.trim() : "";
        switch (type) {
            case DEFECT, WRONG_ITEM -> {
                if (text.length() < 5) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST,
                            "불량·쇼핑몰 측 오배송 반품은 상세 사유를 5자 이상 입력해주세요.");
                }
            }
            case OTHER -> {
                if (text.length() < 10) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST,
                            "기타 사유는 10자 이상 입력해주세요.");
                }
                String compact = text.replaceAll("\\s+", "");
                if (compact.length() < 10) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST,
                            "기타 사유는 의미 있는 내용을 10자 이상 입력해주세요.");
                }
                if (text.matches("(?is)^(기타|없음|테스트|test|\\.+|ㅋ+|ㅎ+)$")) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST,
                            "기타 사유에 구체적인 내용을 입력해주세요.");
                }
            }
            case CHANGE_OF_MIND, ORDER_MISTAKE -> {
                // 상세 사유 선택
            }
        }
    }

}
