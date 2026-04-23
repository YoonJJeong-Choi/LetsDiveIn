package com.swimshop.swim_mall.sale.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.common.enums.SalePolicyHistoryAction;
import com.swimshop.swim_mall.common.enums.SaleStatus;
import com.swimshop.swim_mall.common.enums.SaleScope;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.partner.repository.PartnerEventParticipationRepository;
import com.swimshop.swim_mall.event.repository.EventRepository;
import com.swimshop.swim_mall.option.repository.OptionRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.product.repository.ProductRepository;
import com.swimshop.swim_mall.sale.dto.SalePolicyRequestDto;
import com.swimshop.swim_mall.sale.dto.SalePolicyResponseDto;
import com.swimshop.swim_mall.sale.dto.SaleCampaignRequestDto;
import com.swimshop.swim_mall.sale.dto.SaleCampaignTargetRequestDto;
import com.swimshop.swim_mall.sale.entity.SalePolicyHistoryEntity;
import com.swimshop.swim_mall.sale.entity.SalePolicyEntity;
import com.swimshop.swim_mall.sale.repository.SalePolicyHistoryRepository;
import com.swimshop.swim_mall.sale.repository.SalePolicyRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalePolicyService {

    private final SalePolicyRepository salePolicyRepository;
    private final EventRepository eventRepository;
    private final PartnerEventParticipationRepository partnerEventParticipationRepository;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;
    private final PartnerRepository partnerRepository;
    private final SalePolicyHistoryRepository salePolicyHistoryRepository;
    private final AuthService authService;

    @Transactional
    public SalePolicyResponseDto create(HttpSession session, SalePolicyRequestDto dto) {
        authService.requireRole(session, AccountRole.ADMIN);
        throw new BusinessException(ErrorCode.FORBIDDEN, "세일 정책 생성은 파트너만 가능합니다.");
    }

    @Transactional
    public SalePolicyResponseDto update(HttpSession session, Long id, SalePolicyRequestDto dto) {
        authService.requireRole(session, AccountRole.ADMIN);
        Long adminId = getAdminIdFromSession(session);
        validate(dto);
        validateNoOverlappingPolicy(dto, id);
        SalePolicyEntity entity = salePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "세일 정책을 찾을 수 없습니다."));
        Long beforeEventNo = entity.getEvent() != null ? entity.getEvent().getEventNo() : null;
        EventEntity event = dto.getEventNo() != null
                ? eventRepository.findById(dto.getEventNo())
                        .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND))
                : null;
        entity.update(
                event,
                dto.getScope(),
                dto.getTargetProductNo(),
                dto.getTargetOptionNo(),
                entity.getCreatedByPartnerId(),
                entity.getCampaignId(),
                dto.getDiscountType(),
                dto.getDiscountValue(),
                dto.getMaxDiscountAmount(),
                dto.getStartAt(),
                dto.getEndAt(),
                dto.getStatus()
        );
        SalePolicyEntity saved = salePolicyRepository.save(entity);
        Long afterEventNo = saved.getEvent() != null ? saved.getEvent().getEventNo() : null;
        if (!Objects.equals(beforeEventNo, afterEventNo)) {
            writeHistory(
                    saved,
                    SalePolicyHistoryAction.EVENT_LINK_CHANGED,
                    AccountRole.ADMIN,
                    adminId,
                    "세일-이벤트 연결 변경",
                    saved.getStatus(),
                    saved.getStatus(),
                    beforeEventNo,
                    afterEventNo
            );
        }
        return toResponseDto(saved);
    }

    @Transactional
    public void delete(HttpSession session, Long id) {
        authService.requireRole(session, AccountRole.ADMIN);
        salePolicyRepository.deleteById(id);
    }

    public List<SalePolicyResponseDto> listAdmin(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        return salePolicyRepository.findAll().stream()
                .sorted(Comparator
                        .comparingInt((SalePolicyEntity s) -> saleStatusRank(s.getStatus()))
                        .thenComparing(SalePolicyEntity::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public SalePolicyResponseDto getAdmin(HttpSession session, Long id) {
        authService.requireRole(session, AccountRole.ADMIN);
        SalePolicyEntity entity = salePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "세일 정책을 찾을 수 없습니다."));
        return toResponseDto(entity);
    }

    public SalePolicyResponseDto findApplicable(Long productNo, Long optionNo, LocalDateTime at) {
        LocalDateTime now = at != null ? at : LocalDateTime.now();
        List<SalePolicyEntity> list = salePolicyRepository.findEligibleFor(productNo, optionNo, SaleStatus.ACTIVE, now);
        if (list.isEmpty()) return null;
        return toResponseDto(list.get(0)); // 가장 우선순위 높은 1건
    }

    @Transactional
    public SalePolicyResponseDto createByPartner(HttpSession session, SalePolicyRequestDto dto) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PARTNER_INACTIVE);
        }

        EventEntity event = validatePartnerEventLink(partner, dto.getEventNo(), dto.getStartAt(), dto.getEndAt());
        SalePolicyRequestDto resolvedDto = applyForcedPolicyIfNeeded(dto, event);
        validate(resolvedDto);
        validateOwnership(resolvedDto, partnerId);
        validateNoDuplicateEventLinkedTarget(partnerId, event, resolvedDto);
        validateNoOverlappingPolicy(resolvedDto, null);

        // 강제형(이벤트 연동) SALE은 관리자가 가격/기간을 정한 것으로 보므로 즉시 ACTIVE로 처리합니다.
        SaleStatus resolved = (event != null && event.getEventType() == EventType.SALE)
                ? SaleStatus.ACTIVE
                : resolvePartnerSaleStatus(resolvedDto);
        SalePolicyEntity entity = SalePolicyEntity.builder()
                .event(event)
                .scope(resolvedDto.getScope())
                .targetProductNo(resolvedDto.getTargetProductNo())
                .targetOptionNo(resolvedDto.getTargetOptionNo())
                .createdByPartnerId(partnerId)
                .campaignId(null)
                .discountType(resolvedDto.getDiscountType())
                .discountValue(resolvedDto.getDiscountValue())
                .maxDiscountAmount(resolvedDto.getMaxDiscountAmount())
                .startAt(resolvedDto.getStartAt())
                .endAt(resolvedDto.getEndAt())
                .status(resolved)
                .admin(partner.getAdmin())
                .build();

        return toResponseDto(salePolicyRepository.save(entity));
    }

    public List<SalePolicyResponseDto> listByPartner(HttpSession session) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        return salePolicyRepository.findByCreatedByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .sorted(Comparator
                        .comparingInt((SalePolicyEntity s) -> saleStatusRank(s.getStatus()))
                        .thenComparing(SalePolicyEntity::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 파트너 단독 "캠페인(묶음)" 생성:
     * 캠페인 테이블은 추가하지 않고 SalePolicy에 동일한 campaignId를 저장합니다.
     */
    @Transactional
    public List<SalePolicyResponseDto> createCampaignByPartner(HttpSession session, SaleCampaignRequestDto dto) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PARTNER_INACTIVE);
        }

        dto.validatePeriod();

        // 모든 SalePolicy를 묶기 위한 식별자 (캠페인 테이블 없이 SalePolicy에만 기록)
        String campaignId = java.util.UUID.randomUUID().toString();

        // 캠페인 공통기간
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt();

        EventEntity event = null;
        if (dto.getEventNo() != null) {
            event = validatePartnerEventLink(partner, dto.getEventNo(), startAt, endAt);
            if (event != null && event.getEventType() == EventType.SALE) {
                startAt = event.getCustomerEventStartAt();
                endAt = event.getCustomerEventEndAt();
            }
        }

        List<SalePolicyResponseDto> result = new java.util.ArrayList<>();
        for (SaleCampaignTargetRequestDto target : dto.getTargets()) {
            SalePolicyEntity saved = salePolicyRepository.save(
                    createCampaignPolicy(partner, partnerId, event, dto.getScope(), startAt, endAt, campaignId, target)
            );
            result.add(toResponseDto(saved));
        }
        return result;
    }

    private SalePolicyEntity createCampaignPolicy(
            PartnerEntity partner,
            Long partnerId,
            EventEntity event,
            SaleScope scope,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String campaignId,
            SaleCampaignTargetRequestDto target
    ) {
        SalePolicyRequestDto singleDto = new SalePolicyRequestDto();
        singleDto.setEventNo(event != null ? event.getEventNo() : null);
        singleDto.setScope(scope);
        singleDto.setStartAt(startAt);
        singleDto.setEndAt(endAt);

        if (scope == com.swimshop.swim_mall.common.enums.SaleScope.PRODUCT) {
            singleDto.setTargetProductNo(target.getTargetProductNo());
            singleDto.setTargetOptionNo(null);
        } else {
            singleDto.setTargetProductNo(null);
            singleDto.setTargetOptionNo(target.getTargetOptionNo());
        }

        if (event != null && event.getEventType() == EventType.SALE) {
            singleDto.setDiscountType(event.getSaleDiscountType());
            singleDto.setDiscountValue(event.getSaleDiscountValue());
            singleDto.setMaxDiscountAmount(event.getSaleMaxDiscountAmount());
            singleDto.setStartAt(event.getCustomerEventStartAt());
            singleDto.setEndAt(event.getCustomerEventEndAt());
        } else {
            singleDto.setDiscountType(target.getDiscountType());
            singleDto.setDiscountValue(target.getDiscountValue());
            singleDto.setMaxDiscountAmount(target.getMaxDiscountAmount());
        }

        // validate(dto)는 status를 직접 쓰지 않지만, bean validation/로직 확장 대비용으로 값 채웁니다.
        singleDto.setStatus(SaleStatus.ACTIVE);

        validate(singleDto);
        validateOwnership(singleDto, partnerId);
        validateNoDuplicateEventLinkedTarget(partnerId, event, singleDto);
        validateNoOverlappingPolicy(singleDto, null);

        // 강제형(이벤트 연동) SALE은 관리자가 가격/기간을 정한 것으로 보므로 즉시 ACTIVE로 처리합니다.
        SaleStatus resolved = (event != null && event.getEventType() == EventType.SALE)
                ? SaleStatus.ACTIVE
                : resolvePartnerSaleStatus(singleDto);

        return SalePolicyEntity.builder()
                .event(event)
                .scope(scope)
                .targetProductNo(singleDto.getTargetProductNo())
                .targetOptionNo(singleDto.getTargetOptionNo())
                .createdByPartnerId(partnerId)
                .campaignId(campaignId)
                .discountType(singleDto.getDiscountType())
                .discountValue(singleDto.getDiscountValue())
                .maxDiscountAmount(singleDto.getMaxDiscountAmount())
                .startAt(startAt)
                .endAt(endAt)
                .status(resolved)
                .admin(partner.getAdmin())
                .build();
    }

    @Transactional
    public void cancelCampaignByPartner(HttpSession session, String campaignId) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);

        List<SalePolicyEntity> policies = salePolicyRepository
                .findByCreatedByPartnerIdAndCampaignIdOrderByCreatedAtDesc(partnerId, campaignId);

        if (policies.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "캠페인을 찾을 수 없습니다.");
        }

        boolean hasEventDrivenSale = policies.stream()
                .anyMatch(p -> p.getEvent() != null && p.getEvent().getEventType() == EventType.SALE);
        if (hasEventDrivenSale) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "이벤트 연동 세일은 관리자만 중단할 수 있습니다.");
        }

        for (SalePolicyEntity entity : policies) {
            if (isCancellableStatus(entity.getStatus())) {
                entity.changeStatus(SaleStatus.CANCELLED);
            }
        }

        salePolicyRepository.saveAll(policies);
    }

    public SalePolicyResponseDto getByPartner(HttpSession session, Long id) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        SalePolicyEntity entity = salePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "세일 정책을 찾을 수 없습니다."));
        if (!partnerId.equals(entity.getCreatedByPartnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toResponseDto(entity);
    }

    @Transactional
    public SalePolicyResponseDto approveByAdmin(HttpSession session, Long id) {
        authService.requireRole(session, AccountRole.ADMIN);
        Long adminId = getAdminIdFromSession(session);
        SalePolicyEntity entity = salePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "세일 정책을 찾을 수 없습니다."));
        SaleStatus beforeStatus = entity.getStatus();
        entity.update(
                entity.getEvent(),
                entity.getScope(),
                entity.getTargetProductNo(),
                entity.getTargetOptionNo(),
                entity.getCreatedByPartnerId(),
                entity.getCampaignId(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getMaxDiscountAmount(),
                entity.getStartAt(),
                entity.getEndAt(),
                SaleStatus.ACTIVE
        );
        entity.changeRejectionReason(null);
        SalePolicyEntity saved = salePolicyRepository.save(entity);
        writeHistory(
                saved,
                SalePolicyHistoryAction.APPROVED,
                AccountRole.ADMIN,
                adminId,
                null,
                beforeStatus,
                saved.getStatus(),
                saved.getEvent() != null ? saved.getEvent().getEventNo() : null,
                saved.getEvent() != null ? saved.getEvent().getEventNo() : null
        );
        return toResponseDto(saved);
    }

    @Transactional
    public SalePolicyResponseDto rejectByAdmin(HttpSession session, Long id, String rejectionReason) {
        authService.requireRole(session, AccountRole.ADMIN);
        Long adminId = getAdminIdFromSession(session);
        SalePolicyEntity entity = salePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "세일 정책을 찾을 수 없습니다."));
        SaleStatus beforeStatus = entity.getStatus();
        entity.update(
                entity.getEvent(),
                entity.getScope(),
                entity.getTargetProductNo(),
                entity.getTargetOptionNo(),
                entity.getCreatedByPartnerId(),
                entity.getCampaignId(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getMaxDiscountAmount(),
                entity.getStartAt(),
                entity.getEndAt(),
                SaleStatus.REJECTED
        );
        entity.changeRejectionReason(rejectionReason);
        SalePolicyEntity saved = salePolicyRepository.save(entity);
        writeHistory(
                saved,
                SalePolicyHistoryAction.REJECTED,
                AccountRole.ADMIN,
                adminId,
                rejectionReason,
                beforeStatus,
                saved.getStatus(),
                saved.getEvent() != null ? saved.getEvent().getEventNo() : null,
                saved.getEvent() != null ? saved.getEvent().getEventNo() : null
        );
        return toResponseDto(saved);
    }

    @Transactional
    public SalePolicyResponseDto cancelByAdmin(HttpSession session, Long id, String cancelReason) {
        authService.requireRole(session, AccountRole.ADMIN);
        Long adminId = getAdminIdFromSession(session);
        SalePolicyEntity entity = salePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "세일 정책을 찾을 수 없습니다."));
        if (!isCancellableStatus(entity.getStatus())) {
            return toResponseDto(entity);
        }
        SaleStatus beforeStatus = entity.getStatus();
        entity.changeStatus(SaleStatus.CANCELLED);
        entity.changeRejectionReason(null);
        SalePolicyEntity saved = salePolicyRepository.save(entity);
        writeHistory(
                saved,
                SalePolicyHistoryAction.CANCELLED,
                AccountRole.ADMIN,
                adminId,
                cancelReason,
                beforeStatus,
                saved.getStatus(),
                saved.getEvent() != null ? saved.getEvent().getEventNo() : null,
                saved.getEvent() != null ? saved.getEvent().getEventNo() : null
        );
        return toResponseDto(saved);
    }

    @Transactional
    public SalePolicyResponseDto cancelByPartner(HttpSession session, Long id) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        SalePolicyEntity entity = salePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "세일 정책을 찾을 수 없습니다."));
        if (!partnerId.equals(entity.getCreatedByPartnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (entity.getEvent() != null && entity.getEvent().getEventType() == EventType.SALE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "이벤트 연동 세일은 관리자만 중단할 수 있습니다.");
        }
        if (!isCancellableStatus(entity.getStatus())) {
            return toResponseDto(entity);
        }
        entity.changeStatus(SaleStatus.CANCELLED);
        entity.changeRejectionReason(null);
        return toResponseDto(salePolicyRepository.save(entity));
    }

    private void validate(SalePolicyRequestDto dto) {
        if (dto.getEndAt().isBefore(dto.getStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "세일 종료일시는 시작일시보다 빠를 수 없습니다.");
        }
        switch (dto.getScope()) {
            case PRODUCT -> {
                if (dto.getTargetProductNo() == null) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "상품 단위 세일은 targetProductNo가 필요합니다.");
                }
                if (!productRepository.existsById(dto.getTargetProductNo())) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 상품 번호입니다.");
                }
            }
            case OPTION -> {
                if (dto.getTargetOptionNo() == null) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "옵션 단위 세일은 targetOptionNo가 필요합니다.");
                }
                if (!optionRepository.existsById(dto.getTargetOptionNo())) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 옵션 번호입니다.");
                }
            }
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 세일 범위입니다.");
        }
        if (dto.getDiscountType() == com.swimshop.swim_mall.common.enums.DiscountType.PERCENT) {
            if (dto.getDiscountValue() == null || dto.getDiscountValue() <= 0 || dto.getDiscountValue() > 100) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "퍼센트 할인은 1~100 사이여야 합니다.");
            }
        } else {
            if (dto.getDiscountValue() == null || dto.getDiscountValue() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "정액 할인 금액은 1원 이상이어야 합니다.");
            }
        }
    }

    private void validateNoOverlappingPolicy(SalePolicyRequestDto dto, Long excludeId) {
        List<SaleStatus> overlapStatuses = List.of(
                SaleStatus.ACTIVE,
                SaleStatus.PENDING_APPROVAL,
                SaleStatus.INACTIVE
        );

        long overlapCount = salePolicyRepository.countOverlappingByTarget(
                dto.getScope(),
                dto.getTargetProductNo(),
                dto.getTargetOptionNo(),
                dto.getStartAt(),
                dto.getEndAt(),
                overlapStatuses,
                excludeId
        );

        if (overlapCount > 0) {
            if (dto.getScope() == SaleScope.PRODUCT) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "같은 상품에 기간이 겹치는 세일이 이미 존재합니다."
                );
            }
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "같은 옵션에 기간이 겹치는 세일이 이미 존재합니다."
            );
        }

        // 교차 중복 차단:
        // - 상품 세일 등록 시: 같은 기간에 해당 상품의 옵션 세일이 있으면 차단
        // - 옵션 세일 등록 시: 같은 기간에 해당 옵션의 상위 상품 세일이 있으면 차단
        if (dto.getScope() == SaleScope.PRODUCT) {
            long overlapOptionCount = salePolicyRepository.countOverlappingOptionSalesForProduct(
                    dto.getTargetProductNo(),
                    dto.getStartAt(),
                    dto.getEndAt(),
                    overlapStatuses,
                    excludeId
            );
            if (overlapOptionCount > 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "같은 상품의 옵션 세일과 기간이 겹쳐 등록할 수 없습니다."
                );
            }
            return;
        }

        var option = optionRepository.findById(dto.getTargetOptionNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 옵션 번호입니다."));
        Long parentProductNo = option.getProduct().getProductNo();
        long overlapProductCount = salePolicyRepository.countOverlappingProductSalesForProduct(
                parentProductNo,
                dto.getStartAt(),
                dto.getEndAt(),
                overlapStatuses,
                excludeId
        );
        if (overlapProductCount > 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "같은 상품의 상품 세일과 기간이 겹쳐 등록할 수 없습니다."
            );
        }
    }

    private void validateOwnership(SalePolicyRequestDto dto, Long partnerId) {
        switch (dto.getScope()) {
            case PRODUCT -> {
                var product = productRepository.findById(dto.getTargetProductNo())
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 상품 번호입니다."));
                if (product.getPartner() == null || !partnerId.equals(product.getPartner().getPartnerId())) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "본인 상품만 세일 설정할 수 있습니다.");
                }
            }
            case OPTION -> {
                var option = optionRepository.findById(dto.getTargetOptionNo())
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "존재하지 않는 옵션 번호입니다."));
                if (option.getPartner() == null || !partnerId.equals(option.getPartner().getPartnerId())) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "본인 옵션만 세일 설정할 수 있습니다.");
                }
            }
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 세일 범위입니다.");
        }
    }

    private EventEntity validatePartnerEventLink(
            PartnerEntity partner,
            Long eventNo,
            LocalDateTime saleStartAt,
            LocalDateTime saleEndAt
    ) {
        if (eventNo == null) return null;

        EventEntity event = eventRepository.findById(eventNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        if (event.getAdmin() == null
                || partner.getAdmin() == null
                || !event.getAdmin().getAdminId().equals(partner.getAdmin().getAdminId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "다른 운영 주체의 이벤트에는 세일을 연결할 수 없습니다.");
        }

        if (event.getEventMode() != EventMode.PARTNER_PARTICIPATION) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 참여형 이벤트에만 세일을 연결할 수 있습니다.");
        }

        if (event.getEventType() != EventType.SALE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "SALE 타입 이벤트에만 세일을 연결할 수 있습니다.");
        }

        // 강제형(시즌) SALE은 파트너 신청 기간 내에만 세일 정책을 생성할 수 있습니다.
        // (기간 이후에는 participation이 있어도 신규 sale_policy 생성 자체를 막아야 UX 요구를 만족합니다.)
        LocalDateTime now = LocalDateTime.now();
        if (event.getPartnerApplyStartAt() == null || event.getPartnerApplyEndAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 기간 정보가 누락되었습니다.");
        }
        if (now.isBefore(event.getPartnerApplyStartAt()) || now.isAfter(event.getPartnerApplyEndAt())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너 신청 기간이 아닙니다. (SALE 연동 세일 생성 불가)");
        }

        if (event.getSaleDiscountType() == null || event.getSaleDiscountValue() == null || event.getSaleDiscountValue() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "SALE 이벤트 할인 기준이 설정되지 않아 세일을 생성할 수 없습니다.");
        }

        if (event.getEventStatus() != EventStatus.ACTIVE && event.getEventStatus() != EventStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "진행 예정/진행 중 이벤트에만 세일을 연결할 수 있습니다.");
        }

        // 강제형 SALE 연동은 요청 startAt/endAt을 사용하지 않고 이벤트 기간으로 고정합니다.
        // 따라서 여기서는 요청 기간 검증으로 막지 않습니다.

        var participation = partnerEventParticipationRepository
                .findByPartner_PartnerIdAndEvent_EventNo(partner.getPartnerId(), eventNo);
        if (participation == null || !Boolean.TRUE.equals(participation.getActive())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "이벤트 참여 파트너만 세일을 연결할 수 있습니다.");
        }

        return event;
    }

    private SalePolicyRequestDto applyForcedPolicyIfNeeded(SalePolicyRequestDto dto, EventEntity event) {
        if (event == null || event.getEventType() != EventType.SALE) return dto;

        SalePolicyRequestDto resolved = new SalePolicyRequestDto();
        resolved.setEventNo(dto.getEventNo());
        resolved.setScope(dto.getScope());
        resolved.setTargetProductNo(dto.getTargetProductNo());
        resolved.setTargetOptionNo(dto.getTargetOptionNo());
        resolved.setStartAt(event.getCustomerEventStartAt());
        resolved.setEndAt(event.getCustomerEventEndAt());
        resolved.setStatus(dto.getStatus());
        resolved.setDiscountType(event.getSaleDiscountType());
        resolved.setDiscountValue(event.getSaleDiscountValue());
        resolved.setMaxDiscountAmount(event.getSaleMaxDiscountAmount());
        return resolved;
    }

    private void validateNoDuplicateEventLinkedTarget(Long partnerId, EventEntity event, SalePolicyRequestDto dto) {
        if (event == null || event.getEventType() != EventType.SALE) return;
        long duplicateCount = salePolicyRepository.countEventLinkedByPartnerAndTarget(
                partnerId,
                event.getEventNo(),
                dto.getScope(),
                dto.getTargetProductNo(),
                dto.getTargetOptionNo(),
                List.of(SaleStatus.ACTIVE, SaleStatus.PENDING_APPROVAL, SaleStatus.INACTIVE)
        );
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 이 이벤트에 등록한 대상입니다.");
        }
    }

    private SaleStatus resolvePartnerSaleStatus(SalePolicyRequestDto dto) {
        // 하이브리드 정책:
        // - 퍼센트 40% 초과 또는 기간 14일 초과면 승인대기
        // - 그 외는 즉시 활성
        long durationDays = java.time.Duration.between(dto.getStartAt(), dto.getEndAt()).toDays() + 1;
        boolean highPercent = dto.getDiscountType() == com.swimshop.swim_mall.common.enums.DiscountType.PERCENT
                && dto.getDiscountValue() != null
                && dto.getDiscountValue() > 40;
        boolean longDuration = durationDays > 14;
        if (highPercent || longDuration) {
            return SaleStatus.PENDING_APPROVAL;
        }
        return SaleStatus.ACTIVE;
    }

    private void writeHistory(
            SalePolicyEntity salePolicy,
            SalePolicyHistoryAction action,
            AccountRole changedByRole,
            Long changedById,
            String reason,
            SaleStatus beforeStatus,
            SaleStatus afterStatus,
            Long beforeEventNo,
            Long afterEventNo
    ) {
        salePolicyHistoryRepository.save(
                SalePolicyHistoryEntity.builder()
                        .salePolicy(salePolicy)
                        .action(action)
                        .changedByRole(changedByRole)
                        .changedById(changedById)
                        .reason(reason)
                        .beforeStatus(beforeStatus)
                        .afterStatus(afterStatus)
                        .beforeEventNo(beforeEventNo)
                        .afterEventNo(afterEventNo)
                        .build()
        );
    }

    private boolean isCancellableStatus(SaleStatus status) {
        return status == SaleStatus.PENDING_APPROVAL
                || status == SaleStatus.ACTIVE
                || status == SaleStatus.INACTIVE;
    }

    private int saleStatusRank(SaleStatus status) {
        if (status == null) return 99;
        return switch (status) {
            case ACTIVE -> 0;
            case PENDING_APPROVAL -> 1;
            case INACTIVE -> 2;
            case EXPIRED -> 3;
            case CANCELLED -> 4;
            case REJECTED -> 5;
        };
    }

    private SalePolicyResponseDto toResponseDto(SalePolicyEntity e) {
        String partnerName = null;
        if (e.getCreatedByPartnerId() != null) {
            partnerName = partnerRepository.findById(e.getCreatedByPartnerId())
                    .map(PartnerEntity::getPartnerName)
                    .orElse(null);
        }
        return SalePolicyResponseDto.builder()
                .id(e.getId())
                .campaignId(e.getCampaignId())
                .eventNo(e.getEvent() != null ? e.getEvent().getEventNo() : null)
                .eventType(e.getEvent() != null ? e.getEvent().getEventType() : null)
                .eventTitle(e.getEvent() != null ? e.getEvent().getEventTitle() : null)
                .scope(e.getScope())
                .targetProductNo(e.getTargetProductNo())
                .targetOptionNo(e.getTargetOptionNo())
                .discountType(e.getDiscountType())
                .discountValue(e.getDiscountValue())
                .maxDiscountAmount(e.getMaxDiscountAmount())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .status(e.getStatus())
                .rejectionReason(e.getRejectionReason())
                .createdByPartnerId(e.getCreatedByPartnerId())
                .createdByPartnerName(partnerName)
                .adminNo(e.getAdmin() != null ? e.getAdmin().getAdminId() : null)
                .adminName(e.getAdmin() != null ? e.getAdmin().getAdminName() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private Long getAdminIdFromSession(HttpSession session) {
        Object subject = session.getAttribute("subjectId");
        if (subject instanceof Number) return ((Number) subject).longValue();
        if (subject instanceof Long) return (Long) subject;
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private Long getPartnerIdFromSession(HttpSession session) {
        Object subject = session.getAttribute("subjectId");
        if (subject instanceof Number) return ((Number) subject).longValue();
        if (subject instanceof Long) return (Long) subject;
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}

