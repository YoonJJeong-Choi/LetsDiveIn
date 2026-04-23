package com.swimshop.swim_mall.event.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.PointEventTargetType;
import com.swimshop.swim_mall.common.enums.SaleStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.event.dto.EventRequestDto;
import com.swimshop.swim_mall.event.dto.EventParticipantDto;
import com.swimshop.swim_mall.event.dto.EventResponseDto;
import com.swimshop.swim_mall.event.dto.EventStatusUpdateRequestDto;
import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.entity.EventPointTargetEntity;
import com.swimshop.swim_mall.event.partner.repository.PartnerEventParticipationRepository;
import com.swimshop.swim_mall.event.repository.EventRepository;
import com.swimshop.swim_mall.event.repository.EventPointTargetRepository;
import com.swimshop.swim_mall.sale.entity.SalePolicyEntity;
import com.swimshop.swim_mall.sale.repository.SalePolicyRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final PartnerEventParticipationRepository partnerEventParticipationRepository;
    private final AdminRepository adminRepository;
    private final AuthService authService;
    private final SalePolicyRepository salePolicyRepository;
    private final EventPointTargetRepository eventPointTargetRepository;

    @Transactional
    public EventResponseDto createEvent(HttpSession session, EventRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        Long adminId = getAdminIdFromSession(session);
        AdminEntity admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateCustomerEventPeriod(requestDto.getCustomerEventStartAt(), requestDto.getCustomerEventEndAt());
        validatePartnerApplyPeriod(
                requestDto.getEventMode(),
                requestDto.getPartnerApplyEnabled(),
                requestDto.getPartnerApplyStartAt(),
                requestDto.getPartnerApplyEndAt(),
                requestDto.getCustomerEventStartAt(),
                requestDto.getCustomerEventEndAt());
        validateSaleEventPolicy(
                requestDto.getEventType(),
                requestDto.getEventMode(),
                requestDto.getSaleDiscountType(),
                requestDto.getSaleDiscountValue()
        );
        validatePointEventRewardPolicy(
                requestDto.getEventType(),
                requestDto.getSaleDiscountType(),
                requestDto.getSaleDiscountValue()
        );
        validatePointEventTargetPolicy(
                requestDto.getEventType(),
                requestDto.getPointEventTargetType(),
                resolvePointEventTargetValues(requestDto),
                requestDto.getPointEventMinOrderAmount()
        );

        EventEntity event = EventEntity.builder()
                .eventTitle(requestDto.getEventTitle().trim())
                .eventContent(requestDto.getEventContent().trim())
                .eventStatus(requestDto.getEventStatus())
                .customerEventStartAt(requestDto.getCustomerEventStartAt())
                .customerEventEndAt(requestDto.getCustomerEventEndAt())
                .partnerApplyEnabled(requestDto.getEventMode() == EventMode.PARTNER_PARTICIPATION)
                .partnerApplyStartAt(requestDto.getEventMode() == EventMode.PARTNER_PARTICIPATION ? requestDto.getPartnerApplyStartAt() : null)
                .partnerApplyEndAt(requestDto.getEventMode() == EventMode.PARTNER_PARTICIPATION ? requestDto.getPartnerApplyEndAt() : null)
                .thumbnailUrl(trimToNull(requestDto.getThumbnailUrl()))
                .eventType(requestDto.getEventType())
                .saleDiscountType(requestDto.getSaleDiscountType())
                .saleDiscountValue(requestDto.getSaleDiscountValue())
                .saleMaxDiscountAmount(requestDto.getSaleMaxDiscountAmount())
                .pointEventTargetType(resolvePointEventTargetType(requestDto))
                .pointEventTargetValue(null)
                .pointEventMinOrderAmount(resolvePointEventMinOrderAmount(requestDto))
                .eventMode(requestDto.getEventMode() != null ? requestDto.getEventMode() : EventMode.ADMIN_ONLY)
                .adminMemo(trimToNull(requestDto.getAdminMemo()))
                .admin(admin)
                .build();

        EventEntity saved = eventRepository.save(event);
        savePointTargets(saved, resolvePointEventTargetValues(requestDto));
        return toResponseDto(saved);
    }

    @Transactional
    public EventResponseDto updateEvent(HttpSession session, Long eventNo, EventRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        EventEntity event = eventRepository.findById(eventNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        validateCustomerEventPeriod(requestDto.getCustomerEventStartAt(), requestDto.getCustomerEventEndAt());
        validatePartnerApplyPeriod(
                requestDto.getEventMode(),
                requestDto.getPartnerApplyEnabled(),
                requestDto.getPartnerApplyStartAt(),
                requestDto.getPartnerApplyEndAt(),
                requestDto.getCustomerEventStartAt(),
                requestDto.getCustomerEventEndAt());
        validateSaleEventPolicy(
                requestDto.getEventType(),
                requestDto.getEventMode(),
                requestDto.getSaleDiscountType(),
                requestDto.getSaleDiscountValue()
        );
        validatePointEventRewardPolicy(
                requestDto.getEventType(),
                requestDto.getSaleDiscountType(),
                requestDto.getSaleDiscountValue()
        );
        validatePointEventTargetPolicy(
                requestDto.getEventType(),
                requestDto.getPointEventTargetType(),
                resolvePointEventTargetValues(requestDto),
                requestDto.getPointEventMinOrderAmount()
        );

        event.update(
                requestDto.getEventTitle().trim(),
                requestDto.getEventContent().trim(),
                requestDto.getEventStatus(),
                requestDto.getCustomerEventStartAt(),
                requestDto.getCustomerEventEndAt(),
                requestDto.getEventMode() == EventMode.PARTNER_PARTICIPATION,
                requestDto.getEventMode() == EventMode.PARTNER_PARTICIPATION ? requestDto.getPartnerApplyStartAt() : null,
                requestDto.getEventMode() == EventMode.PARTNER_PARTICIPATION ? requestDto.getPartnerApplyEndAt() : null,
                trimToNull(requestDto.getThumbnailUrl()),
                requestDto.getEventType(),
                requestDto.getSaleDiscountType(),
                requestDto.getSaleDiscountValue(),
                requestDto.getSaleMaxDiscountAmount(),
                resolvePointEventTargetType(requestDto),
                null,
                resolvePointEventMinOrderAmount(requestDto),
                requestDto.getEventMode() != null ? requestDto.getEventMode() : EventMode.ADMIN_ONLY,
                trimToNull(requestDto.getAdminMemo())
        );

        syncLinkedSalePoliciesForSaleEvent(event);
        EventEntity saved = eventRepository.save(event);
        savePointTargets(saved, resolvePointEventTargetValues(requestDto));
        return toResponseDto(saved);
    }

    /**
     * SALE 이벤트는 파트너가 등록한 연동 sale_policy의 기간을 이벤트 고객 노출 기간과 동일하게 강제합니다.
     * (취소/거절된 정책은 이력 보존을 위해 동기화 대상에서 제외)
     */
    private void syncLinkedSalePoliciesForSaleEvent(EventEntity event) {
        if (event.getEventType() != EventType.SALE) {
            return;
        }

        List<SalePolicyEntity> linkedPolicies = salePolicyRepository.findByEvent_EventNoOrderByCreatedAtDesc(event.getEventNo());
        for (SalePolicyEntity salePolicy : linkedPolicies) {
            SaleStatus status = salePolicy.getStatus();
            if (status == SaleStatus.CANCELLED || status == SaleStatus.REJECTED) {
                continue;
            }

            salePolicy.update(
                    event,
                    salePolicy.getScope(),
                    salePolicy.getTargetProductNo(),
                    salePolicy.getTargetOptionNo(),
                    salePolicy.getCreatedByPartnerId(),
                    salePolicy.getCampaignId(),
                    salePolicy.getDiscountType(),
                    salePolicy.getDiscountValue(),
                    salePolicy.getMaxDiscountAmount(),
                    event.getCustomerEventStartAt(),
                    event.getCustomerEventEndAt(),
                    salePolicy.getStatus()
            );
        }
    }

    @Transactional
    public EventResponseDto updateEventStatus(HttpSession session, Long eventNo, EventStatusUpdateRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        EventEntity event = eventRepository.findById(eventNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        validateStatusTransition(event.getEventStatus(), requestDto.getEventStatus());
        event.changeStatus(requestDto.getEventStatus());
        return toResponseDto(eventRepository.save(event));
    }

    public List<EventResponseDto> getAdminEvents(
            HttpSession session,
            EventStatus status,
            String keyword,
            LocalDateTime startAt,
            LocalDateTime endAt) {
        authService.requireRole(session, AccountRole.ADMIN);
        String normalizedKeyword = keyword != null ? keyword.trim() : "";
        LocalDateTime boundedStartAt = startAt != null ? startAt : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime boundedEndAt = endAt != null ? endAt : LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        return eventRepository.searchForAdmin(
                        status,
                        normalizedKeyword,
                        boundedStartAt,
                        boundedEndAt
                ).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public EventResponseDto getAdminEventDetail(HttpSession session, Long eventNo) {
        authService.requireRole(session, AccountRole.ADMIN);
        EventEntity event = eventRepository.findById(eventNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        return toResponseDto(event);
    }

    public List<EventParticipantDto> getAdminEventParticipants(HttpSession session, Long eventNo) {
        authService.requireRole(session, AccountRole.ADMIN);
        EventEntity event = eventRepository.findById(eventNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        List<SalePolicyEntity> linkedSalePolicies = salePolicyRepository.findByEvent_EventNoOrderByCreatedAtDesc(event.getEventNo());
        Map<Long, List<SalePolicyEntity>> salePoliciesByPartner = linkedSalePolicies.stream()
                .filter(s -> s.getCreatedByPartnerId() != null)
                .collect(Collectors.groupingBy(SalePolicyEntity::getCreatedByPartnerId));

        return partnerEventParticipationRepository.findByEvent_EventNoOrderByCreatedAtDesc(event.getEventNo())
                .stream()
                .map(p -> {
                    Long partnerId = p.getPartner().getPartnerId();
                    List<SalePolicyEntity> partnerSales = salePoliciesByPartner.getOrDefault(partnerId, List.of());
                    String targetSummary = partnerSales.stream()
                            .map(this::toAdminTargetLabel)
                            .distinct()
                            .limit(12)
                            .collect(Collectors.joining(", "));

                    return EventParticipantDto.builder()
                            .partnerId(partnerId)
                            .partnerName(p.getPartner().getPartnerName())
                            .partnerContact(p.getPartner().getPartnerContact())
                            .active(Boolean.TRUE.equals(p.getActive()))
                            .participatedAt(p.getCreatedAt())
                            .linkedSalePolicyCount(partnerSales.size())
                            .linkedSaleTargets(targetSummary.isBlank() ? "-" : targetSummary)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<EventResponseDto> getPublicEvents() {
        return eventRepository.findPublicVisibleEvents(
                        List.of(EventStatus.SCHEDULED, EventStatus.ACTIVE, EventStatus.ENDED)
                ).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public EventResponseDto getPublicEventDetail(Long eventNo) {
        EventEntity event = eventRepository.findPublicVisibleEventDetail(
                        eventNo,
                        List.of(EventStatus.SCHEDULED, EventStatus.ACTIVE, EventStatus.ENDED)
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        return toResponseDto(event);
    }

    public List<EventResponseDto> getPartnerVisibleEvents(HttpSession session, Integer upcomingDays) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);

        LocalDateTime latestStartAt = null;
        if (upcomingDays != null) {
            if (upcomingDays < 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "upcomingDays는 0 이상이어야 합니다.");
            }
            latestStartAt = LocalDateTime.now().plusDays(upcomingDays);
        }

        List<EventEntity> events;
        if (latestStartAt == null) {
            events = eventRepository.findPartnerVisibleEvents(
                    EventMode.PARTNER_PARTICIPATION,
                    List.of(EventStatus.SCHEDULED, EventStatus.ACTIVE, EventStatus.ENDED)
            );
        } else {
            events = eventRepository.findPartnerVisibleEventsWithStartAtLimit(
                    EventMode.PARTNER_PARTICIPATION,
                    List.of(EventStatus.SCHEDULED, EventStatus.ACTIVE, EventStatus.ENDED),
                    latestStartAt
            );
        }

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventNos = events.stream()
                .map(EventEntity::getEventNo)
                .collect(Collectors.toList());

        Set<Long> participatingEventNos = partnerEventParticipationRepository.findActiveParticipatingEventNos(
                partnerId, eventNos
        );

        // SALE 타입 이벤트는 `participationEnabled`(active=true 참여 여부)와 별개로,
        // `participating`은 "실제 세일 정책(sale_policy) 등록 여부"를 의미합니다.
        List<Long> saleEventNos = events.stream()
                .filter(e -> e.getEventType() == EventType.SALE)
                .map(EventEntity::getEventNo)
                .collect(Collectors.toList());

        Set<Long> saleEventNosWithRegisteredSales = saleEventNos.isEmpty()
                ? Set.of()
                : Set.copyOf(
                        salePolicyRepository.findRegisteredSaleEventNosForPartnerAt(
                                partnerId,
                                saleEventNos,
                                List.of(SaleStatus.CANCELLED, SaleStatus.REJECTED)
                        )
                );

        return events.stream()
                .map(event -> {
                    EventResponseDto dto = toResponseDto(event);
                    return EventResponseDto.builder()
                            .eventNo(dto.getEventNo())
                            .eventTitle(dto.getEventTitle())
                            .eventContent(dto.getEventContent())
                            .eventStatus(dto.getEventStatus())
                            .eventStatusLabel(dto.getEventStatusLabel())
                            .customerEventStartAt(dto.getCustomerEventStartAt())
                            .customerEventEndAt(dto.getCustomerEventEndAt())
                            .partnerApplyEnabled(dto.getPartnerApplyEnabled())
                            .partnerApplyStartAt(dto.getPartnerApplyStartAt())
                            .partnerApplyEndAt(dto.getPartnerApplyEndAt())
                            .thumbnailUrl(dto.getThumbnailUrl())
                            .eventType(dto.getEventType())
                            .saleDiscountType(dto.getSaleDiscountType())
                            .saleDiscountValue(dto.getSaleDiscountValue())
                            .saleMaxDiscountAmount(dto.getSaleMaxDiscountAmount())
                            .pointEventTargetType(dto.getPointEventTargetType())
                            .pointEventTargetValues(dto.getPointEventTargetValues())
                            .pointEventMinOrderAmount(dto.getPointEventMinOrderAmount())
                            .eventMode(dto.getEventMode())
                            .adminMemo(dto.getAdminMemo())
                            .adminNo(dto.getAdminNo())
                            .adminName(dto.getAdminName())
                            .createdAt(dto.getCreatedAt())
                            .updatedAt(dto.getUpdatedAt())
                            .visibleToCustomerNow(dto.getVisibleToCustomerNow())
                            .participationOpenNow(dto.getParticipationOpenNow())
                            .participationEnabled(participatingEventNos.contains(dto.getEventNo()))
                            .participating(
                                    dto.getEventType() == EventType.SALE
                                            ? saleEventNosWithRegisteredSales.contains(dto.getEventNo())
                                            : participatingEventNos.contains(dto.getEventNo())
                            )
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void validateCustomerEventPeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "고객 이벤트 시작/종료일시는 필수입니다.");
        }
        if (endAt.isBefore(startAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "고객 이벤트 종료일시는 시작일시보다 빠를 수 없습니다.");
        }
    }

    private void validatePartnerApplyPeriod(
            EventMode eventMode,
            Boolean partnerApplyEnabled,
            LocalDateTime partnerApplyStartAt,
            LocalDateTime partnerApplyEndAt,
            LocalDateTime customerEventStartAt,
            LocalDateTime customerEventEndAt) {
        boolean enabled = eventMode == EventMode.PARTNER_PARTICIPATION;
        if (eventMode == EventMode.PARTNER_PARTICIPATION && !Boolean.TRUE.equals(partnerApplyEnabled)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 참여형 이벤트는 파트너 신청 기간 설정이 필수입니다.");
        }
        if (!enabled) return;

        if (partnerApplyStartAt == null || partnerApplyEndAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 기간을 사용하려면 시작/종료일시가 모두 필요합니다.");
        }
        if (partnerApplyEndAt.isBefore(partnerApplyStartAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 종료일시는 시작일시보다 빠를 수 없습니다.");
        }

        // 파트너 신청은 고객 이벤트 시작 전에 시작/마감되어야 합니다.
        if (!partnerApplyStartAt.isBefore(customerEventStartAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 시작일시는 고객 이벤트 시작일시보다 빨라야 합니다.");
        }
        if (!partnerApplyEndAt.isBefore(customerEventStartAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 종료일시는 고객 이벤트 시작일시보다 빨라야 합니다.");
        }
        // 방어적으로 고객 이벤트 종료 이후 신청기간은 금지
        if (partnerApplyEndAt.isAfter(customerEventEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 종료일시는 고객 이벤트 종료일시를 넘을 수 없습니다.");
        }
    }

    private void validateStatusTransition(EventStatus from, EventStatus to) {
        if (from == to) return;

        switch (from) {
            case DRAFT -> {
                if (to != EventStatus.SCHEDULED && to != EventStatus.ACTIVE && to != EventStatus.INACTIVE) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "DRAFT ?곹깭?먯꽌??SCHEDULED/ACTIVE/INACTIVE濡쒕쭔 蹂寃?媛?ν빀?덈떎.");
                }
            }
            case SCHEDULED -> {
                if (to != EventStatus.ACTIVE && to != EventStatus.INACTIVE) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "SCHEDULED ?곹깭?먯꽌??ACTIVE/INACTIVE濡쒕쭔 蹂寃?媛?ν빀?덈떎.");
                }
            }
            case ACTIVE -> {
                if (to != EventStatus.ENDED && to != EventStatus.INACTIVE) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "ACTIVE ?곹깭?먯꽌??ENDED/INACTIVE濡쒕쭔 蹂寃?媛?ν빀?덈떎.");
                }
            }
            case ENDED -> {
                // 일단 ENDED가 되면 더 이상 어떤 상태로도 전환할 수 없습니다. (불가역)
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료된 이벤트는 상태를 변경할 수 없습니다.");
            }
            case INACTIVE -> {
                if (to != EventStatus.SCHEDULED && to != EventStatus.ACTIVE) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "INACTIVE ?곹깭?먯꽌??SCHEDULED/ACTIVE濡쒕쭔 蹂寃?媛?ν빀?덈떎.");
                }
            }
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "吏?먰븯吏 ?딅뒗 ?곹깭 ?꾪솚?낅땲??");
        }
    }

    private void validateSaleEventPolicy(
            EventType eventType,
            EventMode eventMode,
            DiscountType saleDiscountType,
            Long saleDiscountValue
    ) {
        if (eventType != EventType.SALE) return;
        if (eventMode != EventMode.PARTNER_PARTICIPATION) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "SALE 이벤트는 파트너 참여형으로만 생성할 수 있습니다.");
        }
        if (saleDiscountType == null || saleDiscountValue == null || saleDiscountValue <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "SALE 이벤트는 할인 타입/할인값이 필수입니다.");
        }
        if (saleDiscountType == DiscountType.PERCENT && saleDiscountValue > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "퍼센트 할인은 1~100 사이여야 합니다.");
        }
    }

    private void validatePointEventTargetPolicy(
            EventType eventType,
            PointEventTargetType targetType,
            List<String> targetValues,
            Long minOrderAmount
    ) {
        if (eventType != EventType.POINT) return;
        PointEventTargetType resolvedType = targetType != null ? targetType : PointEventTargetType.ALL;

        if (resolvedType == PointEventTargetType.ALL) return;
        if (resolvedType == PointEventTargetType.MIN_ORDER_AMOUNT) {
            if (minOrderAmount == null || minOrderAmount <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "MIN_ORDER_AMOUNT 타겟은 최소 주문금액이 1 이상이어야 합니다.");
            }
            return;
        }
        if (targetValues == null || targetValues.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "선택 적용 타겟 값이 필요합니다.");
        }
    }

    private void validatePointEventRewardPolicy(
            EventType eventType,
            DiscountType rewardType,
            Long rewardValue
    ) {
        if (eventType != EventType.POINT) return;
        if (rewardType == null || rewardValue == null || rewardValue <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "POINT 이벤트는 지급 타입/지급 값이 필수입니다.");
        }
    }

    private PointEventTargetType resolvePointEventTargetType(EventRequestDto requestDto) {
        if (requestDto.getEventType() != EventType.POINT) return null;
        return requestDto.getPointEventTargetType() != null
                ? requestDto.getPointEventTargetType()
                : PointEventTargetType.ALL;
    }

    private Long resolvePointEventMinOrderAmount(EventRequestDto requestDto) {
        if (requestDto.getEventType() != EventType.POINT) return null;
        PointEventTargetType targetType = resolvePointEventTargetType(requestDto);
        if (targetType != PointEventTargetType.MIN_ORDER_AMOUNT) return null;
        return requestDto.getPointEventMinOrderAmount();
    }

    private List<String> resolvePointEventTargetValues(EventRequestDto requestDto) {
        if (requestDto.getEventType() != EventType.POINT) return List.of();
        PointEventTargetType targetType = resolvePointEventTargetType(requestDto);
        if (targetType == PointEventTargetType.ALL || targetType == PointEventTargetType.MIN_ORDER_AMOUNT) return List.of();

        if (requestDto.getPointEventTargetValues() != null && !requestDto.getPointEventTargetValues().isEmpty()) {
            return requestDto.getPointEventTargetValues().stream()
                    .map(this::trimToNull)
                    .filter(v -> v != null)
                    .map(String::toUpperCase)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void savePointTargets(EventEntity event, List<String> targetValues) {
        eventPointTargetRepository.deleteByEvent_EventNo(event.getEventNo());
        // 동일 트랜잭션 내 재삽입 시 유니크 충돌을 피하기 위해 삭제를 즉시 반영합니다.
        eventPointTargetRepository.flush();
        if (event.getEventType() != EventType.POINT) return;
        PointEventTargetType targetType = event.getPointEventTargetType();
        if (targetType == null || targetType == PointEventTargetType.ALL || targetType == PointEventTargetType.MIN_ORDER_AMOUNT) {
            return;
        }
        if (targetValues == null || targetValues.isEmpty()) return;
        List<String> deduplicatedValues = new LinkedHashSet<>(targetValues).stream().toList();
        for (String value : deduplicatedValues) {
            try {
                eventPointTargetRepository.save(new EventPointTargetEntity(event, targetType, value));
            } catch (DataIntegrityViolationException ignored) {
                // 동시 수정 경쟁으로 동일 (event_no, target_type, target_value)가 먼저 생성된 경우 무시
            }
        }
    }

    private List<String> readPointTargetValues(Long eventNo) {
        return eventPointTargetRepository.findByEvent_EventNo(eventNo).stream()
                .map(EventPointTargetEntity::getTargetValue)
                .distinct()
                .collect(Collectors.toList());
    }

    private EventResponseDto toResponseDto(EventEntity event) {
        LocalDateTime now = LocalDateTime.now();
        boolean visibleToCustomer = event.getEventStatus() == EventStatus.ACTIVE
                && !now.isBefore(event.getCustomerEventStartAt())
                && !now.isAfter(event.getCustomerEventEndAt());

        boolean participationOpen = Boolean.TRUE.equals(event.getPartnerApplyEnabled())
                && event.getPartnerApplyStartAt() != null
                && event.getPartnerApplyEndAt() != null
                && !now.isBefore(event.getPartnerApplyStartAt())
                && !now.isAfter(event.getPartnerApplyEndAt());

        return EventResponseDto.builder()
                .eventNo(event.getEventNo())
                .eventTitle(event.getEventTitle())
                .eventContent(event.getEventContent())
                .eventStatus(event.getEventStatus())
                .eventStatusLabel(event.getEventStatus().getLabel())
                .customerEventStartAt(event.getCustomerEventStartAt())
                .customerEventEndAt(event.getCustomerEventEndAt())
                .partnerApplyEnabled(event.getPartnerApplyEnabled())
                .partnerApplyStartAt(event.getPartnerApplyStartAt())
                .partnerApplyEndAt(event.getPartnerApplyEndAt())
                .thumbnailUrl(event.getThumbnailUrl())
                .eventType(event.getEventType())
                .saleDiscountType(event.getSaleDiscountType())
                .saleDiscountValue(event.getSaleDiscountValue())
                .saleMaxDiscountAmount(event.getSaleMaxDiscountAmount())
                .pointEventTargetType(event.getPointEventTargetType())
                .pointEventTargetValues(readPointTargetValues(event.getEventNo()))
                .pointEventMinOrderAmount(event.getPointEventMinOrderAmount())
                .eventMode(event.getEventMode())
                .adminMemo(event.getAdminMemo())
                .adminNo(event.getAdmin().getAdminId())
                .adminName(event.getAdmin().getAdminName())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .visibleToCustomerNow(visibleToCustomer)
                .participationOpenNow(participationOpen)
                .build();
    }

    private String toAdminTargetLabel(SalePolicyEntity s) {
        if (s.getScope() == com.swimshop.swim_mall.common.enums.SaleScope.OPTION) {
            return s.getTargetOptionNo() != null ? "옵션#" + s.getTargetOptionNo() : "옵션";
        }
        return s.getTargetProductNo() != null ? "상품#" + s.getTargetProductNo() : "상품";
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

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


