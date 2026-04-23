package com.swimshop.swim_mall.return_order.service;

import java.time.LocalDateTime;
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
import com.swimshop.swim_mall.common.enums.ReturnRiskTier;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.return_order.dto.ReturnRequestDto;
import com.swimshop.swim_mall.return_order.dto.ReturnResponseDto;
import com.swimshop.swim_mall.return_order.dto.ReturnUpdateRequestDto;
import com.swimshop.swim_mall.return_order.dto.ReturnHistoryDto;
import com.swimshop.swim_mall.return_order.dto.ReturnAiAssistResponseDto;
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
import com.swimshop.swim_mall.file.service.SignedFileUrlService;
import com.swimshop.swim_mall.return_order.client.OpenAiReturnAssistVisionClient;

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
    private final ReturnFraudService returnFraudService;
    private final ReturnImageAnalysisService returnImageAnalysisService;
    private final ChecklistGenerator checklistGenerator;
    private final OpenAiReturnAssistVisionClient openAiReturnAssistVisionClient;
    private final SignedFileUrlService signedFileUrlService;

    @Value("${app.public-base-url:${app.base-url:http://localhost:8080}}")
    private String appPublicBaseUrl;

    @Value("${AI_FEATURE_ENABLED:true}")
    private boolean aiFeatureEnabled;

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

        int riskScore = computeReturnRiskScore(customerId, orderItem.getItemTotalPrice(), requestDto.getReturnReasonType());
        ReturnRiskTier riskTier = toRiskTier(riskScore);

        String reasonToStore = normalizeReturnReason(requestDto.getReturnReason());

        // 반품 엔티티 생성
        ReturnEntity returnEntity = ReturnEntity.builder()
                .orderItem(orderItem)
                .returnStatus(ReturnStatus.REQUESTED)
                .returnRequestedAt(LocalDateTime.now())
                .returnReasonType(requestDto.getReturnReasonType())
                .returnRiskScore(riskScore)
                .returnRiskTier(riskTier)
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
            newValueMap.put("returnRiskScore", riskScore);
            newValueMap.put("returnRiskTier", riskTier.name());
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
     * 관리자 반품 AI 보조 결과 조회 (MVP: 규칙 기반 보조만 제공)
     */
    @Transactional
    public ReturnAiAssistResponseDto getAdminAiAssist(HttpSession session, Long returnNo) {
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        if (currentUser.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자만 AI 보조 기능을 사용할 수 있습니다.");
        }

        ReturnEntity targetReturn = returnRepository.findById(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_NOT_FOUND));

        if (!aiFeatureEnabled) {
            ReturnAiAssistResponseDto disabled = ReturnAiAssistResponseDto.builder()
                    .featureEnabled(false)
                    .fraudScore(0)
                    .riskLevel(ReturnRiskTier.LOW.name())
                    .riskFactors(List.of("AI_FEATURE_DISABLED"))
                    .evidenceTags(List.of())
                    .evidenceGaps(List.of())
                    .recommendedActions(List.of("AI 기능이 비활성화되어 기존 관리자 검토 절차를 진행하세요."))
                    .build();
            recordAiAssistAudit(targetReturn, currentUser, disabled);
            return disabled;
        }

        Long customerId = targetReturn.getOrderItem().getOrder().getCustomer().getCustomerId();
        List<ReturnEntity> customerReturns = returnRepository.findByCustomerId(customerId);
        ReturnFraudService.FraudResult fraud = returnFraudService.evaluate(targetReturn, customerReturns);

        List<String> ruleBasedImageUrls = getReturnImageUrls(targetReturn);
        ReturnImageAnalysisService.EvidenceResult ruleBasedEvidence = returnImageAnalysisService.analyze(
                ruleBasedImageUrls,
                targetReturn.getReturnReasonType(),
                targetReturn.getReturnReason());
        List<String> ruleBasedActions = checklistGenerator.generate(fraud, ruleBasedEvidence);

        ReturnImageAnalysisService.EvidenceResult evidence = ruleBasedEvidence;
        List<String> actions = ruleBasedActions;
        boolean shouldAttemptOpenAi = shouldUseOpenAiVision(
                fraud,
                targetReturn.getReturnReasonType(),
                !ruleBasedImageUrls.isEmpty());
        if (!shouldAttemptOpenAi) {
            log.info(
                    "AI assist OpenAI vision skipped by policy. returnNo={}, riskLevel={}, reasonType={}, imageCount={}",
                    targetReturn.getReturnNo(),
                    fraud != null && fraud.riskLevel() != null ? fraud.riskLevel().name() : "UNKNOWN",
                    targetReturn.getReturnReasonType(),
                    ruleBasedImageUrls.size());
        }

        boolean readyForOpenAi = shouldAttemptOpenAi;
        List<String> openAiImageUrls = List.of();
        String fallbackReason = null;
        if (shouldAttemptOpenAi) {
            try {
                openAiImageUrls = getOpenAiImageUrls(targetReturn);
            } catch (Exception e) {
                readyForOpenAi = false;
                fallbackReason = "SIGNED_URL_GENERATION_FAILED";
                log.warn(
                        "AI assist OpenAI vision fallback applied. returnNo={}, reasonType={}, imageCount={}, fallbackReason={}, errorType={}",
                        targetReturn.getReturnNo(),
                        targetReturn.getReturnReasonType(),
                        ruleBasedImageUrls.size(),
                        fallbackReason,
                        e.getClass().getSimpleName());
            }
        }

        if (readyForOpenAi) {
            try {
                OpenAiReturnAssistVisionClient.VisionAssistResult openAiResult = openAiReturnAssistVisionClient.requestVisionAssist(
                        openAiImageUrls,
                        targetReturn.getReturnReasonType(),
                        targetReturn.getReturnReason());
                if (safeList(openAiResult.recommendedActions()).isEmpty()) {
                    fallbackReason = "OPENAI_EMPTY_RECOMMENDED_ACTIONS";
                } else {
                    evidence = new ReturnImageAnalysisService.EvidenceResult(
                            safeList(openAiResult.evidenceTags()),
                            safeList(openAiResult.evidenceGaps()));
                    actions = safeList(openAiResult.recommendedActions());
                }
            } catch (Exception e) {
                fallbackReason = "OPENAI_CALL_OR_PARSE_FAILED";
                log.warn(
                        "AI assist OpenAI vision fallback applied. returnNo={}, reasonType={}, imageCount={}, fallbackReason={}, errorType={}",
                        targetReturn.getReturnNo(),
                        targetReturn.getReturnReasonType(),
                        openAiImageUrls.size(),
                        fallbackReason,
                        e.getClass().getSimpleName());
            }
        }

        if (fallbackReason != null && readyForOpenAi) {
            log.warn(
                    "AI assist OpenAI vision fallback applied. returnNo={}, reasonType={}, imageCount={}, fallbackReason={}",
                    targetReturn.getReturnNo(),
                    targetReturn.getReturnReasonType(),
                    openAiImageUrls.size(),
                    fallbackReason);
        }

        ReturnAiAssistResponseDto response = ReturnAiAssistResponseDto.builder()
                .featureEnabled(true)
                .fraudScore(fraud.fraudScore())
                .riskLevel(fraud.riskLevel().name())
                .riskFactors(safeList(fraud.riskFactors()))
                .evidenceTags(safeList(evidence.evidenceTags()))
                .evidenceGaps(safeList(evidence.evidenceGaps()))
                .recommendedActions(safeList(actions))
                .build();

        recordAiAssistAudit(targetReturn, currentUser, response);
        return response;
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

    private void recordAiAssistAudit(ReturnEntity returnEntity, AuthLoginResponseDto actor, ReturnAiAssistResponseDto response) {
        try {
            AdminEntity admin = adminRepository.findById(actor.getSubjectId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

            Map<String, Object> payload = new HashMap<>();
            payload.put("actorRole", actor.getRole() != null ? actor.getRole().name() : "UNKNOWN");
            payload.put("actorId", actor.getSubjectId());
            payload.put("featureEnabled", response.isFeatureEnabled());
            payload.put("fraudScore", response.getFraudScore());
            payload.put("riskLevel", response.getRiskLevel());
            payload.put("riskFactors", safeList(response.getRiskFactors()));
            payload.put("evidenceTags", safeList(response.getEvidenceTags()));
            payload.put("evidenceGaps", safeList(response.getEvidenceGaps()));
            payload.put("recommendedActions", safeList(response.getRecommendedActions()));

            String newValueJson = objectMapper.writeValueAsString(payload);
            ReturnHistoryEntity history = ReturnHistoryEntity.createByAdmin(
                    returnEntity,
                    admin,
                    "AI_ASSIST",
                    null,
                    newValueJson,
                    "관리자 반품 AI 보조 조회");
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
                .productImageUrl(orderItem.getProduct().getProductImageUrl())
                .optionNo(orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null)
                .color(orderItem.getOption() != null ? orderItem.getOption().getColor() : null)
                .size(orderItem.getOption() != null ? orderItem.getOption().getSize() : null)
                .quantity(orderItem.getItemQuantity())
                .itemPrice(orderItem.getItemPrice())
                .itemTotalPrice(orderItem.getItemTotalPrice())
                .returnStatus(returnEntity.getReturnStatus())
                .returnRequestedAt(returnEntity.getReturnRequestedAt())
                .returnReasonType(returnEntity.getReturnReasonType())
                .returnRiskScore(returnEntity.getReturnRiskScore())
                .returnRiskTier(returnEntity.getReturnRiskTier())
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

    private List<String> getOpenAiImageUrls(ReturnEntity returnEntity) {
        String base = appPublicBaseUrl != null ? appPublicBaseUrl.replaceAll("/+$", "") : "http://localhost:8080";
        return returnImageRepository.findByReturnEntityOrderByIdAsc(returnEntity).stream()
                .map(image -> {
                    UploadedFileEntity file = image.getFile();
                    if (file == null) {
                        return null;
                    }
                    if (Boolean.TRUE.equals(file.getIsPrivate())) {
                        return signedFileUrlService.createSignedDownloadUrl(file.getFileId());
                    }
                    return String.format("%s/uploads/%s/%s", base, file.getCategory(), file.getStoredName());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean shouldUseOpenAiVision(
            ReturnFraudService.FraudResult fraud,
            ReturnReasonType reasonType,
            boolean hasImageEvidence) {
        ReturnRiskTier level = fraud != null ? fraud.riskLevel() : ReturnRiskTier.LOW;
        if (level == ReturnRiskTier.HIGH) {
            return true;
        }
        if (level == ReturnRiskTier.MEDIUM) {
            boolean reasonEligible = reasonType == ReturnReasonType.DEFECT || reasonType == ReturnReasonType.WRONG_ITEM;
            return reasonEligible && hasImageEvidence;
        }
        return false;
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

    /**
     * 신청 시점 기준 위험 점수 (목록/상세 분기용 스냅샷).
     * - 90일 고객 반품 건수는 '이번 신청 직전' 건수 기준 (이번 건 포함 시 임계값에 맞춤).
     */
    private int computeReturnRiskScore(Long customerId, long returnAmount, ReturnReasonType type) {
        int score = switch (type) {
            case CHANGE_OF_MIND, ORDER_MISTAKE -> 0;
            case DEFECT, WRONG_ITEM -> 30;
            case OTHER -> 15;
        };
        if (returnAmount >= 100_000L) {
            score += 20;
        }
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        long cnt90 = returnRepository.countByCustomerRequestedSince(customerId, since90);
        if (cnt90 >= 2) {
            score += 25;
        }
        return score;
    }

    private static ReturnRiskTier toRiskTier(int score) {
        if (score >= 70) {
            return ReturnRiskTier.HIGH;
        }
        if (score >= 40) {
            return ReturnRiskTier.MEDIUM;
        }
        return ReturnRiskTier.LOW;
    }
}
