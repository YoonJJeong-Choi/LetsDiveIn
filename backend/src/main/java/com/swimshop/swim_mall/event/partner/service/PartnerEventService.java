package com.swimshop.swim_mall.event.partner.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.partner.entity.PartnerEventParticipationEntity;
import com.swimshop.swim_mall.event.partner.entity.PartnerEventRewardOrderCapEntity;
import com.swimshop.swim_mall.event.partner.entity.PartnerEventRewardEntity;
import com.swimshop.swim_mall.event.partner.repository.PartnerEventParticipationRepository;
import com.swimshop.swim_mall.event.partner.repository.PartnerEventRewardOrderCapRepository;
import com.swimshop.swim_mall.event.partner.repository.PartnerEventRewardRepository;
import com.swimshop.swim_mall.event.repository.EventPointTargetRepository;
import com.swimshop.swim_mall.event.repository.EventRepository;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.point.service.PointService;
import com.swimshop.swim_mall.common.enums.SalePolicyHistoryAction;
import com.swimshop.swim_mall.sale.entity.SalePolicyEntity;
import com.swimshop.swim_mall.sale.entity.SalePolicyHistoryEntity;
import com.swimshop.swim_mall.sale.repository.SalePolicyHistoryRepository;
import com.swimshop.swim_mall.sale.repository.SalePolicyRepository;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.SaleStatus;
import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.PointEventTargetType;
import java.time.Duration;

import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PartnerEventService {
    private static final long MAX_POINT_REWARDS_PER_CUSTOMER = 3L;

    private final PartnerRepository partnerRepository;
    private final EventRepository eventRepository;
    private final PartnerEventParticipationRepository participationRepository;
    private final PartnerEventRewardRepository rewardRepository;
    private final PartnerEventRewardOrderCapRepository rewardOrderCapRepository;
    private final EventPointTargetRepository eventPointTargetRepository;
    private final PointService pointService;
    private final AuthService authService;
    private final SalePolicyRepository salePolicyRepository;
    private final SalePolicyHistoryRepository salePolicyHistoryRepository;

    /**
     * 파트너가 특정 이벤트에 참여 설정(=대상화)합니다.
     * MVP: 매장 전체(STORE_LEVEL)만 지원합니다.
     */
    @Transactional
    public void setParticipation(HttpSession session, Long eventNo, boolean active) {
        setParticipation(session, eventNo, active, false);
    }

    @Transactional
    public void setParticipation(HttpSession session, Long eventNo, boolean active, boolean deactivateLinkedSales) {
        authService.requireRole(session, AccountRole.PARTNER);

        Long partnerId = getPartnerIdFromSession(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        if (partner.getPartnerStatus() == com.swimshop.swim_mall.common.enums.PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PARTNER_INACTIVE);
        }

        EventEntity event = eventRepository.findById(eventNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 파트너 참여형 이벤트만 신청/해제 가능
        if (event.getEventMode() != EventMode.PARTNER_PARTICIPATION) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 참여형 이벤트가 아닙니다.");
        }

        if (Boolean.FALSE.equals(event.getPartnerApplyEnabled())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 기간이 설정되지 않은 이벤트입니다.");
        }

        // 신청/해제 가능 기간 체크: now가 신청 시작/종료 범위 내여야 함
        LocalDateTime now = LocalDateTime.now();
        if (event.getPartnerApplyStartAt() == null || event.getPartnerApplyEndAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청 기간 정보가 누락되었습니다.");
        }
        if (now.isBefore(event.getPartnerApplyStartAt()) || now.isAfter(event.getPartnerApplyEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파트너 신청/해제 가능 기간이 아닙니다.");
        }

        PartnerEventParticipationEntity participation = participationRepository
                .findByPartner_PartnerIdAndEvent_EventNo(partnerId, eventNo);

        if (participation == null) {
            if (!active) return; // 이미 미참여 상태
            participation = new PartnerEventParticipationEntity(partner, event);
            // scope는 현재 기본 STORE_LEVEL
        } else {
            if (active) {
                participation.activate();
            } else {
                participation.deactivate();
            }
        }

        participationRepository.save(participation);

        if (event.getEventType() == EventType.SALE) {
            if (!active && deactivateLinkedSales) {
                deactivateLinkedSalesForEvent(partnerId, eventNo);
            }
            if (active) {
                reactivateLinkedSalesForEvent(partnerId, eventNo);
            }
        }
    }

    private void deactivateLinkedSalesForEvent(Long partnerId, Long eventNo) {
        List<SalePolicyEntity> linkedSales = salePolicyRepository.findByCreatedByPartnerIdOrderByCreatedAtDesc(partnerId)
                .stream()
                .filter(s -> s.getEvent() != null && eventNo.equals(s.getEvent().getEventNo()))
                .collect(Collectors.toList());

        for (SalePolicyEntity sale : linkedSales) {
            if (sale.getStatus() == SaleStatus.ACTIVE || sale.getStatus() == SaleStatus.PENDING_APPROVAL) {
                SaleStatus beforeStatus = sale.getStatus();
                sale.changeStatus(SaleStatus.INACTIVE);

                // 참여 해제로 인해 비활성화된 건만 재참여 시 복구 대상이 되도록 이력을 남깁니다.
                salePolicyHistoryRepository.save(
                        SalePolicyHistoryEntity.builder()
                                .salePolicy(sale)
                                .action(SalePolicyHistoryAction.PARTICIPATION_DEACTIVATED)
                                .changedByRole(AccountRole.PARTNER)
                                .changedById(partnerId)
                                .reason("파트너 이벤트 참여 해제에 의해 연동 세일 정책 비활성화")
                                .beforeStatus(beforeStatus)
                                .afterStatus(SaleStatus.INACTIVE)
                                .beforeEventNo(sale.getEvent() != null ? sale.getEvent().getEventNo() : null)
                                .afterEventNo(sale.getEvent() != null ? sale.getEvent().getEventNo() : null)
                                .build()
                );
            }
        }
        if (!linkedSales.isEmpty()) {
            salePolicyRepository.saveAll(linkedSales);
        }
    }

    private void reactivateLinkedSalesForEvent(Long partnerId, Long eventNo) {
        LocalDateTime now = LocalDateTime.now();
        List<SalePolicyEntity> linkedSales = salePolicyRepository.findByCreatedByPartnerIdOrderByCreatedAtDesc(partnerId)
                .stream()
                .filter(s -> s.getEvent() != null && eventNo.equals(s.getEvent().getEventNo()))
                .filter(s -> s.getStatus() == SaleStatus.INACTIVE)
                .collect(Collectors.toList());

        for (SalePolicyEntity sale : linkedSales) {
            // 1) 이미 종료된 세일은 절대 복구하지 않습니다.
            if (sale.getEndAt() != null && sale.getEndAt().isBefore(now)) {
                sale.changeStatus(SaleStatus.EXPIRED);
                continue;
            }

            // 2) 마지막 이력이 "참여해제"였던 INACTIVE만 복구합니다. (아무 INACTIVE나 복구하지 않기 위함)
            var latestHistoryOpt = salePolicyHistoryRepository.findTopBySalePolicy_IdOrderByChangedAtDesc(sale.getId());
            if (latestHistoryOpt.isEmpty()) continue;
            var latestHistory = latestHistoryOpt.get();
            if (latestHistory.getAction() != SalePolicyHistoryAction.PARTICIPATION_DEACTIVATED) continue;

            // 복구는 "참여해제 직전 상태"로 되돌립니다.
            SaleStatus restoreStatus = latestHistory.getBeforeStatus();
            if (restoreStatus != SaleStatus.ACTIVE && restoreStatus != SaleStatus.PENDING_APPROVAL) {
                restoreStatus = resolveStatusForSalePolicy(sale);
            }

            sale.changeStatus(restoreStatus);
            salePolicyHistoryRepository.save(
                    SalePolicyHistoryEntity.builder()
                            .salePolicy(sale)
                            .action(SalePolicyHistoryAction.PARTICIPATION_REACTIVATED)
                            .changedByRole(AccountRole.PARTNER)
                            .changedById(partnerId)
                            .reason("파트너 이벤트 재참여에 의해 연동 세일 정책 복구")
                            .beforeStatus(SaleStatus.INACTIVE)
                            .afterStatus(restoreStatus)
                            .beforeEventNo(sale.getEvent() != null ? sale.getEvent().getEventNo() : null)
                            .afterEventNo(sale.getEvent() != null ? sale.getEvent().getEventNo() : null)
                            .build()
            );
        }

        if (!linkedSales.isEmpty()) {
            salePolicyRepository.saveAll(linkedSales);
        }
    }

    private SaleStatus resolveStatusForSalePolicy(SalePolicyEntity sale) {
        // 하이브리드 정책:
        // - PERCENT 할인값이 40% 초과 또는 기간 14일 초과면 PENDING_APPROVAL
        // - 그 외 ACTIVE
        if (sale == null) return SaleStatus.ACTIVE;

        long durationDays = Duration.between(sale.getStartAt(), sale.getEndAt()).toDays() + 1;
        boolean highPercent = sale.getDiscountType() == DiscountType.PERCENT
                && sale.getDiscountValue() != null
                && sale.getDiscountValue() > 40;
        boolean longDuration = durationDays > 14;

        return (highPercent || longDuration) ? SaleStatus.PENDING_APPROVAL : SaleStatus.ACTIVE;
    }

    @Transactional
    public boolean getParticipationEnabled(HttpSession session, Long eventNo) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);

        PartnerEventParticipationEntity participation = participationRepository
                .findByPartner_PartnerIdAndEvent_EventNo(partnerId, eventNo);

        return participation != null && Boolean.TRUE.equals(participation.getActive());
    }

    /**
     * 주문 상품 구매 확정 시점에, 해당 파트너가 참여한 이벤트 보상을 지급합니다.
     * - POINT 이벤트: 기본 구매확정 포인트(basePointAmount)에 배수/가산으로 지급
     * - SALE 이벤트: 추가 포인트 지급 없음
     */
    @Transactional
    public void handleOrderItemCompleted(OrderItemEntity orderItem, CustomerEntity customer, Long basePointAmount) {
        if (orderItem == null || customer == null) return;
        if (basePointAmount == null || basePointAmount <= 0) return;

        // MVP 대상: 옵션/상품을 통해 파트너 식별
        Long partnerId = resolvePartnerId(orderItem);
        if (partnerId == null) return;

        List<EventEntity> eligibleEvents = resolveEligibleEventsFromSnapshotOrRuntime(orderItem, partnerId);

        if (eligibleEvents.isEmpty()) return;

        for (EventEntity event : eligibleEvents) {
            if (event.getEventType() != EventType.POINT) {
                continue;
            }
            Long eventNo = event.getEventNo();
            Long orderNo = orderItem.getOrder() != null ? orderItem.getOrder().getOrderNo() : null;
            if (orderNo == null) continue;
            PolicySnapshot snap = findPartnerPolicySnapshot(orderItem, eventNo);
            boolean eligible = (snap != null)
                    ? isEligibleForPointTargetSnapshot(snap, orderItem)
                    : isEligibleForPointTarget(event, orderItem);
            if (!eligible) continue;

            boolean alreadyRewarded = rewardRepository.existsByEvent_EventNoAndOrderItemNo(
                    eventNo,
                    orderItem.getOrderItemNo()
            );
            if (alreadyRewarded) continue;

            Long eventPointAmount = (snap != null)
                    ? calculateEventPointAmountFromSnapshot(snap, basePointAmount)
                    : calculateEventPointAmount(event, basePointAmount);
            if (eventPointAmount <= 0) continue;

            boolean countedThisOrder = rewardOrderCapRepository.existsByEvent_EventNoAndCustomer_CustomerIdAndOrderNo(
                    eventNo,
                    customer.getCustomerId(),
                    orderNo
            );
            if (!countedThisOrder) {
                long usedCount = rewardOrderCapRepository.countByEvent_EventNoAndCustomer_CustomerId(
                        eventNo,
                        customer.getCustomerId()
                );
                if (usedCount >= MAX_POINT_REWARDS_PER_CUSTOMER) continue;
                try {
                    rewardOrderCapRepository.save(
                            new PartnerEventRewardOrderCapEntity(event, customer, orderNo)
                    );
                } catch (DataIntegrityViolationException ignored) {
                    // 동일 주문에 대한 동시 처리 충돌은 유니크 제약으로 흡수
                }
            }

            // 기록
            PartnerEventRewardEntity reward = new PartnerEventRewardEntity(
                    event,
                    customer,
                    partnerId,
                    orderItem.getOrderItemNo(),
                    eventPointAmount
            );
            try {
                rewardRepository.save(reward);
            } catch (DataIntegrityViolationException ignored) {
                // 중복 완료 요청/동시 처리 시 (event_no, order_item_no) 유니크 충돌은 무시(멱등)
                continue;
            }

            // 포인트 지급
            pointService.accumulateEventPoint(
                    customer,
                    orderItem,
                    eventPointAmount,
                    String.format("%s 보상", (snap != null && snap.eventTitle != null) ? snap.eventTitle : event.getEventTitle())
            );
        }
    }

    public List<Long> getSnapshotEligibleEventNosForPartnerAt(Long partnerId, LocalDateTime at) {
        if (partnerId == null || at == null) return List.of();
        return participationRepository.findSnapshotEligibleEventNosForPartnerAt(
                partnerId,
                at,
                com.swimshop.swim_mall.common.enums.EventStatus.ACTIVE
        );
    }

    private List<EventEntity> resolveEligibleEventsFromSnapshotOrRuntime(OrderItemEntity orderItem, Long partnerId) {
        List<Long> snapshotEventNos = parseSnapshotCsv(orderItem.getPartnerEventSnapshot());
        if (!snapshotEventNos.isEmpty()) {
            return eventRepository.findAllById(snapshotEventNos);
        }

        // 하위 호환: 스냅샷 없는 기존 주문은 기존 런타임 판정 로직 유지
        LocalDateTime completedAt = orderItem.getCompletedAt() != null ? orderItem.getCompletedAt() : LocalDateTime.now();
        return participationRepository.findEligibleEventsForPartnerAt(
                partnerId,
                completedAt,
                com.swimshop.swim_mall.common.enums.EventStatus.ACTIVE
        );
    }

    private List<Long> parseSnapshotCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private Long resolvePartnerId(OrderItemEntity orderItem) {
        if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
            return orderItem.getOption().getPartner().getPartnerId();
        }
        if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
            return orderItem.getProduct().getPartner().getPartnerId();
        }
        return null;
    }

    private Long calculateEventPointAmount(EventEntity event, Long basePointAmount) {
        if (event == null || basePointAmount == null || basePointAmount <= 0) return 0L;
        if (event.getSaleDiscountType() == null || event.getSaleDiscountValue() == null) return 0L;

        if (event.getSaleDiscountType() == DiscountType.PERCENT) {
            return Math.max(0L, basePointAmount * event.getSaleDiscountValue() / 100L);
        }
        return Math.max(0L, event.getSaleDiscountValue());
    }

    // ===== 스냅샷 판독 로직 =====
    private static class PolicySnapshot {
        public Long eventNo;
        public String eventTitle;
        public String rewardType;
        public Long rewardValue;
        public String pointEventTargetType;
        public java.util.List<String> pointEventTargetValues;
        public Long pointEventMinOrderAmount;
    }

    private PolicySnapshot findPartnerPolicySnapshot(OrderItemEntity orderItem, Long eventNo) {
        try {
            String json = orderItem.getPartnerEventPolicySnapshot();
            if (json == null || json.isBlank()) return null;
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(json);
            var arr = node.get("policies");
            if (arr == null || !arr.isArray()) return null;
            for (var it : arr) {
                Long en = it.has("eventNo") && !it.get("eventNo").isNull() ? it.get("eventNo").asLong() : null;
                if (en != null && en.equals(eventNo)) {
                    PolicySnapshot p = new PolicySnapshot();
                    p.eventNo = en;
                    p.eventTitle = it.has("eventTitle") && !it.get("eventTitle").isNull() ? it.get("eventTitle").asText() : null;
                    p.rewardType = it.has("rewardType") && !it.get("rewardType").isNull() ? it.get("rewardType").asText() : null;
                    p.rewardValue = it.has("rewardValue") && !it.get("rewardValue").isNull() ? it.get("rewardValue").asLong() : null;
                    p.pointEventTargetType = it.has("pointEventTargetType") && !it.get("pointEventTargetType").isNull() ? it.get("pointEventTargetType").asText() : null;
                    p.pointEventMinOrderAmount = it.has("pointEventMinOrderAmount") && !it.get("pointEventMinOrderAmount").isNull() ? it.get("pointEventMinOrderAmount").asLong() : null;
                    if (it.has("pointEventTargetValues") && it.get("pointEventTargetValues").isArray()) {
                        p.pointEventTargetValues = new java.util.ArrayList<>();
                        it.get("pointEventTargetValues").forEach(v -> {
                            if (v != null && !v.isNull()) p.pointEventTargetValues.add(v.asText());
                        });
                    } else {
                        p.pointEventTargetValues = java.util.List.of();
                    }
                    return p;
                }
            }
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean isEligibleForPointTargetSnapshot(PolicySnapshot snap, OrderItemEntity orderItem) {
        String tt = snap.pointEventTargetType != null ? snap.pointEventTargetType : "ALL";
        if ("ALL".equals(tt)) return true;
        if ("MIN_ORDER_AMOUNT".equals(tt)) {
            Long min = snap.pointEventMinOrderAmount;
            Long orderTotal = orderItem.getOrder() != null ? orderItem.getOrder().getOrderTotalPrice() : null;
            return min != null && min > 0 && orderTotal != null && orderTotal >= min;
        }
        return switch (tt) {
            case "PARTNER" -> {
                Long partnerId = resolvePartnerId(orderItem);
                yield partnerId != null && snap.pointEventTargetValues != null
                        && snap.pointEventTargetValues.contains(String.valueOf(partnerId));
            }
            case "PRODUCT" -> {
                Long productNo = orderItem.getProduct() != null ? orderItem.getProduct().getProductNo() : null;
                yield productNo != null && snap.pointEventTargetValues != null
                        && snap.pointEventTargetValues.contains(String.valueOf(productNo));
            }
            case "OPTION" -> {
                Long optionNo = orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null;
                yield optionNo != null && snap.pointEventTargetValues != null
                        && snap.pointEventTargetValues.contains(String.valueOf(optionNo));
            }
            case "CATEGORY" -> {
                String productType = orderItem.getProduct() != null && orderItem.getProduct().getProductType() != null
                        ? orderItem.getProduct().getProductType().name()
                        : null;
                yield productType != null && snap.pointEventTargetValues != null
                        && snap.pointEventTargetValues.contains(productType.toUpperCase());
            }
            default -> true;
        };
    }

    private Long calculateEventPointAmountFromSnapshot(PolicySnapshot snap, Long basePointAmount) {
        if (basePointAmount == null || basePointAmount <= 0) return 0L;
        if (snap.rewardType == null || snap.rewardValue == null || snap.rewardValue <= 0) return 0L;
        if ("PERCENT".equals(snap.rewardType)) {
            return Math.max(0L, basePointAmount * snap.rewardValue / 100L);
        }
        return Math.max(0L, snap.rewardValue);
    }

    private boolean isEligibleForPointTarget(EventEntity event, OrderItemEntity orderItem) {
        PointEventTargetType targetType = event.getPointEventTargetType() != null
                ? event.getPointEventTargetType()
                : PointEventTargetType.ALL;
        if (targetType == PointEventTargetType.ALL) return true;

        if (targetType == PointEventTargetType.MIN_ORDER_AMOUNT) {
            Long minOrderAmount = event.getPointEventMinOrderAmount();
            Long orderTotal = orderItem.getOrder() != null ? orderItem.getOrder().getOrderTotalPrice() : null;
            return minOrderAmount != null && minOrderAmount > 0 && orderTotal != null && orderTotal >= minOrderAmount;
        }

        return switch (targetType) {
            case PARTNER -> {
                Long partnerId = resolvePartnerId(orderItem);
                yield partnerId != null && eventPointTargetRepository.existsByEvent_EventNoAndTargetTypeAndTargetValue(
                        event.getEventNo(),
                        PointEventTargetType.PARTNER,
                        String.valueOf(partnerId)
                );
            }
            case PRODUCT -> {
                Long productNo = orderItem.getProduct() != null ? orderItem.getProduct().getProductNo() : null;
                yield productNo != null && eventPointTargetRepository.existsByEvent_EventNoAndTargetTypeAndTargetValue(
                        event.getEventNo(),
                        PointEventTargetType.PRODUCT,
                        String.valueOf(productNo)
                );
            }
            case OPTION -> {
                Long optionNo = orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null;
                yield optionNo != null && eventPointTargetRepository.existsByEvent_EventNoAndTargetTypeAndTargetValue(
                        event.getEventNo(),
                        PointEventTargetType.OPTION,
                        String.valueOf(optionNo)
                );
            }
            case CATEGORY -> {
                String productType = orderItem.getProduct() != null && orderItem.getProduct().getProductType() != null
                        ? orderItem.getProduct().getProductType().name()
                        : null;
                yield productType != null && eventPointTargetRepository.existsByEvent_EventNoAndTargetTypeAndTargetValue(
                        event.getEventNo(),
                        PointEventTargetType.CATEGORY,
                        productType.toUpperCase()
                );
            }
            default -> false;
        };
    }

    private Long getPartnerIdFromSession(HttpSession session) {
        Object subject = session.getAttribute("subjectId");
        if (subject instanceof Number) return ((Number) subject).longValue();
        if (subject instanceof Long) return (Long) subject;
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}

