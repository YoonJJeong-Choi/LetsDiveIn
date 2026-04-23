package com.swimshop.swim_mall.event.admin.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.PointEventTargetType;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.event.admin.entity.AdminEventRewardOrderCapEntity;
import com.swimshop.swim_mall.event.admin.repository.AdminEventRewardOrderCapRepository;
import com.swimshop.swim_mall.event.admin.entity.AdminEventRewardEntity;
import com.swimshop.swim_mall.event.admin.repository.AdminEventRewardRepository;
import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.repository.EventPointTargetRepository;
import com.swimshop.swim_mall.event.repository.EventRepository;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.point.service.PointService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminEventRewardService {
    private static final long MAX_POINT_REWARDS_PER_CUSTOMER = 3L;

    private final EventRepository eventRepository;
    private final AdminEventRewardRepository adminEventRewardRepository;
    private final AdminEventRewardOrderCapRepository adminEventRewardOrderCapRepository;
    private final EventPointTargetRepository eventPointTargetRepository;
    private final PointService pointService;

    // 스냅샷 파싱용 간단 POJO
    private static class PolicySnapshot {
        public Long eventNo;
        public String eventTitle;
        public String rewardType; // PERCENT|FIXED
        public Long rewardValue;
        public String pointEventTargetType; // enum name
        public List<String> pointEventTargetValues;
        public Long pointEventMinOrderAmount;
    }

    /**
     * 관리자 단독 이벤트(ADMIN_ONLY) 중 POINT 타입 보상 지급.
     * 이벤트 보상은 기본 구매확정 포인트(basePointAmount)에 배수/가산으로 적용합니다.
     */
    @Transactional
    public void handleOrderItemCompleted(OrderItemEntity orderItem, CustomerEntity customer, Long basePointAmount) {
        if (orderItem == null || customer == null) return;
        if (basePointAmount == null || basePointAmount <= 0) return;

        List<EventEntity> events = resolveEligibleEventsFromSnapshotOrRuntime(orderItem);

        if (events.isEmpty()) return;

        for (EventEntity event : events) {
            if (event.getEventType() != EventType.POINT) continue;
            Long orderNo = orderItem.getOrder() != null ? orderItem.getOrder().getOrderNo() : null;
            if (orderNo == null) continue;
            // 스냅샷 정책(주문 시점)이 있으면 이를 우선 사용
            PolicySnapshot snap = findAdminPolicySnapshot(orderItem, event.getEventNo());
            boolean eligible = (snap != null)
                    ? isEligibleForPointTargetSnapshot(snap, orderItem)
                    : isEligibleForPointTarget(event, orderItem);
            if (!eligible) continue;

            boolean alreadyRewarded = adminEventRewardRepository.existsByEvent_EventNoAndOrderItemNo(
                    event.getEventNo(),
                    orderItem.getOrderItemNo()
            );
            if (alreadyRewarded) continue;

            Long eventPointAmount = (snap != null)
                    ? calculateEventPointAmountFromSnapshot(snap, basePointAmount)
                    : calculateEventPointAmount(event, basePointAmount);
            if (eventPointAmount <= 0) continue;

            boolean countedThisOrder = adminEventRewardOrderCapRepository.existsByEvent_EventNoAndCustomer_CustomerIdAndOrderNo(
                    event.getEventNo(),
                    customer.getCustomerId(),
                    orderNo
            );
            if (!countedThisOrder) {
                long usedCount = adminEventRewardOrderCapRepository.countByEvent_EventNoAndCustomer_CustomerId(
                        event.getEventNo(),
                        customer.getCustomerId()
                );
                if (usedCount >= MAX_POINT_REWARDS_PER_CUSTOMER) continue;
                try {
                    adminEventRewardOrderCapRepository.save(
                            new AdminEventRewardOrderCapEntity(event, customer, orderNo)
                    );
                } catch (DataIntegrityViolationException ignored) {
                    // 동일 주문에 대한 동시 처리 충돌은 유니크 제약으로 흡수
                }
            }

            try {
                adminEventRewardRepository.save(
                        new AdminEventRewardEntity(
                                event,
                                customer,
                                orderItem.getOrderItemNo(),
                                eventPointAmount
                        )
                );
            } catch (DataIntegrityViolationException ignored) {
                // 중복 완료 요청/동시 처리 시 (event_no, order_item_no) 유니크 충돌은 무시(멱등)
                continue;
            }

            pointService.accumulateEventPoint(
                    customer,
                    orderItem,
                    eventPointAmount,
                    String.format("%s 보상", (snap != null && snap.eventTitle != null) ? snap.eventTitle : event.getEventTitle())
            );
        }
    }

    private PolicySnapshot findAdminPolicySnapshot(OrderItemEntity orderItem, Long eventNo) {
        try {
            String json = orderItem.getAdminEventPolicySnapshot();
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
                        p.pointEventTargetValues = List.of();
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

    public List<Long> getSnapshotEligibleEventNosAt(LocalDateTime at) {
        if (at == null) return List.of();
        return eventRepository.findActiveEventsByMode(
                        EventStatus.ACTIVE,
                        EventMode.ADMIN_ONLY,
                        at
                ).stream()
                .map(EventEntity::getEventNo)
                .collect(Collectors.toList());
    }

    private List<EventEntity> resolveEligibleEventsFromSnapshotOrRuntime(OrderItemEntity orderItem) {
        List<Long> snapshotEventNos = parseSnapshotCsv(orderItem.getAdminEventSnapshot());
        if (!snapshotEventNos.isEmpty()) {
            return eventRepository.findAllById(snapshotEventNos);
        }

        // 하위 호환: 스냅샷 없는 기존 주문은 기존 런타임 판정 로직 유지
        LocalDateTime at = orderItem.getCompletedAt() != null ? orderItem.getCompletedAt() : LocalDateTime.now();
        return eventRepository.findActiveEventsByMode(
                EventStatus.ACTIVE,
                EventMode.ADMIN_ONLY,
                at
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

    private Long calculateEventPointAmount(EventEntity event, Long basePointAmount) {
        if (event == null || basePointAmount == null || basePointAmount <= 0) return 0L;
        if (event.getSaleDiscountType() == null || event.getSaleDiscountValue() == null) return 0L;

        if (event.getSaleDiscountType() == DiscountType.PERCENT) {
            return Math.max(0L, basePointAmount * event.getSaleDiscountValue() / 100L);
        }
        return Math.max(0L, event.getSaleDiscountValue());
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

    private Long resolvePartnerId(OrderItemEntity orderItem) {
        if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
            return orderItem.getOption().getPartner().getPartnerId();
        }
        if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
            return orderItem.getProduct().getPartner().getPartnerId();
        }
        return null;
    }
}

