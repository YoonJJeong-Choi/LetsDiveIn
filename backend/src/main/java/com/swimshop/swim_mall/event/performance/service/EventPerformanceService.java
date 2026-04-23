package com.swimshop.swim_mall.event.performance.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.swimshop.swim_mall.event.admin.repository.AdminEventRewardRepository;
import com.swimshop.swim_mall.event.partner.repository.PartnerEventRewardRepository;
import com.swimshop.swim_mall.event.performance.dto.EventPerformanceResponseDto;
import com.swimshop.swim_mall.event.performance.dto.EventPerformanceListItemDto;
import com.swimshop.swim_mall.event.performance.dto.EventTimeseriesPointDto;
import com.swimshop.swim_mall.event.performance.dto.EventTopItemDto;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.event.repository.EventRepository;
import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.common.enums.EventStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventPerformanceService {

    private final AdminEventRewardRepository adminEventRewardRepository;
    private final PartnerEventRewardRepository partnerEventRewardRepository;
    private final OrderItemRepository orderItemRepository;
    private final EventRepository eventRepository;

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public EventPerformanceResponseDto getAdminPerformance(Long eventNo, LocalDateTime fromAt, LocalDateTime toAt) {
        if (eventNo == null) {
            throw new IllegalArgumentException("eventNo is required");
        }

        // 1) 보상 포인트 합계 (관리자/파트너)
        Long adminRewardSum;
        Long partnerRewardSum;
        // 2) 이벤트에 귀속된 주문아이템 집합 (보상 발생 기준)
        List<Long> adminOrderItemNos;
        List<Long> partnerOrderItemNos;

        boolean noRange = (fromAt == null && toAt == null);
        if (noRange) {
            adminRewardSum = safeLong(() -> adminEventRewardRepository.sumPointAmountByEventNo(eventNo));
            partnerRewardSum = safeLong(() -> partnerEventRewardRepository.sumPointAmountByEventNo(eventNo));
            adminOrderItemNos = safeList(() -> adminEventRewardRepository.findOrderItemNosByEventNo(eventNo));
            partnerOrderItemNos = safeList(() -> partnerEventRewardRepository.findOrderItemNosByEventNo(eventNo));
        } else {
            adminRewardSum = safeLong(() ->
                    adminEventRewardRepository.sumPointAmountByEventNoAndCompletedBetween(eventNo, fromAt, toAt));
            partnerRewardSum = safeLong(() ->
                    partnerEventRewardRepository.sumPointAmountByEventNoAndCompletedBetween(eventNo, fromAt, toAt));
            adminOrderItemNos = safeList(() ->
                    adminEventRewardRepository.findOrderItemNosByEventNoAndCompletedBetween(eventNo, fromAt, toAt));
            partnerOrderItemNos = safeList(() ->
                    partnerEventRewardRepository.findOrderItemNosByEventNoAndCompletedBetween(eventNo, fromAt, toAt));
        }

        Set<Long> orderItemNoSet = new LinkedHashSet<>();
        orderItemNoSet.addAll(adminOrderItemNos);
        orderItemNoSet.addAll(partnerOrderItemNos);

        List<OrderItemEntity> items = orderItemNoSet.isEmpty()
                ? new ArrayList<>()
                : orderItemRepository.findAllById(orderItemNoSet);

        Long totalOrderItems = (long) items.size();
        Long totalOrders = items.stream()
                .map(oi -> oi.getOrder() != null ? oi.getOrder().getOrderNo() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet())
                .stream().count();
        Long totalNetAmount = items.stream()
                .map(OrderItemEntity::getItemTotalPrice)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        String payoutType = (partnerRewardSum != null && partnerRewardSum > 0) ? "PARTNER_PARTICIPATION" : "ADMIN_ONLY";

        return EventPerformanceResponseDto.builder()
                .eventNo(eventNo)
                .fromAt(fromAt)
                .toAt(toAt)
                .totalOrders(totalOrders)
                .totalOrderItems(totalOrderItems)
                .totalNetAmount(totalNetAmount)
                .adminRewardPoint(adminRewardSum)
                .partnerRewardPoint(partnerRewardSum)
                .payoutType(payoutType)
                .sampleOrderItemNos(orderItemNoSet.stream().limit(20).collect(Collectors.toList()))
                .build();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public EventPerformanceResponseDto getPartnerPerformance(Long partnerId, Long eventNo, LocalDateTime fromAt, LocalDateTime toAt) {
        // 파트너 보상 합계 및 아이템 범위를 파트너 보상 레코드로 한정
        Long partnerReward = partnerEventRewardRepository.sumPointAmountByEventNoAndPartnerId(eventNo, partnerId);
        List<Long> partnerOrderItemNos;
        if (fromAt == null && toAt == null) {
            partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdOnly(eventNo, partnerId);
        } else if (fromAt != null && toAt != null) {
            partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdBetweenRequired(eventNo, partnerId, fromAt, toAt);
        } else if (fromAt != null) {
            partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdFrom(eventNo, partnerId, fromAt);
        } else {
            partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdTo(eventNo, partnerId, toAt);
        }
        if (partnerOrderItemNos.isEmpty()) {
            return EventPerformanceResponseDto.builder()
                    .eventNo(eventNo).fromAt(fromAt).toAt(toAt)
                    .totalOrders(0L).totalOrderItems(0L).totalNetAmount(0L)
                    .adminRewardPoint(0L).partnerRewardPoint(0L).rewardToNetRatio(0.0)
                    .sampleOrderItemNos(List.of())
                    .build();
        }
        List<OrderItemEntity> items = orderItemRepository.findAllWithOptionAndProductPartnerByIdIn(new java.util.ArrayList<>(partnerOrderItemNos));
        Long orders = items.stream().map(oi -> oi.getOrder().getOrderNo()).filter(java.util.Objects::nonNull).distinct().count();
        Long itemsCnt = (long) items.size();
        Long net = items.stream().map(OrderItemEntity::getItemTotalPrice).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
        double ratio = net != null && net > 0 ? ((double) partnerReward / (double) net) : 0.0;
        return EventPerformanceResponseDto.builder()
                .eventNo(eventNo).fromAt(fromAt).toAt(toAt)
                .totalOrders(orders).totalOrderItems(itemsCnt).totalNetAmount(net)
                .adminRewardPoint(0L).partnerRewardPoint(partnerReward)
                .rewardToNetRatio(ratio)
                .payoutType("PARTNER_PARTICIPATION")
                .sampleOrderItemNos(List.of())
                .build();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<EventPerformanceListItemDto> getPartnerEndedEventsPerformance(Long partnerId, LocalDateTime fromAt, LocalDateTime toAt) {
        List<Long> partnerEventNos = partnerEventRewardRepository.findEventNosByPartnerId(partnerId);
        if (partnerEventNos.isEmpty()) return List.of();
        List<EventEntity> events = eventRepository.findAllById(partnerEventNos);
        return events.stream()
                .filter(e -> e.getEventStatus() == EventStatus.ENDED)
                .map(e -> {
                    EventPerformanceResponseDto dto = getPartnerPerformance(partnerId, e.getEventNo(), fromAt, toAt);
                    return EventPerformanceListItemDto.builder()
                            .eventNo(e.getEventNo())
                            .eventTitle(e.getEventTitle())
                            .eventStatus(e.getEventStatus().name())
                            .customerEventStartAt(e.getCustomerEventStartAt())
                            .customerEventEndAt(e.getCustomerEventEndAt())
                            .totalOrders(dto.getTotalOrders())
                            .totalOrderItems(dto.getTotalOrderItems())
                            .totalNetAmount(dto.getTotalNetAmount())
                            .adminRewardPoint(dto.getAdminRewardPoint())
                            .partnerRewardPoint(dto.getPartnerRewardPoint())
                            .rewardToNetRatio(dto.getRewardToNetRatio())
                            .build();
                })
                .collect(Collectors.toList());
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<EventTimeseriesPointDto> getTimeseries(Long eventNo, LocalDateTime fromAt, LocalDateTime toAt) {
        List<Long> adminOrderItemNos;
        List<Long> partnerOrderItemNos;
        if (eventNo == null) {
            List<Long> endedEventNos = eventRepository.findByEventStatus(EventStatus.ENDED)
                    .stream().map(EventEntity::getEventNo).toList();
            if (endedEventNos.isEmpty()) return List.of();
            if (fromAt == null && toAt == null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNos(endedEventNos);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNos(endedEventNos);
            } else if (fromAt != null && toAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNosAndCompletedBetweenRequired(endedEventNos, fromAt, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedBetweenRequired(endedEventNos, fromAt, toAt);
            } else if (fromAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNosAndCompletedFrom(endedEventNos, fromAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedFrom(endedEventNos, fromAt);
            } else {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNosAndCompletedTo(endedEventNos, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedTo(endedEventNos, toAt);
            }
        } else {
            if (fromAt == null && toAt == null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoOnly(eventNo);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoOnly(eventNo);
            } else if (fromAt != null && toAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoBetweenRequired(eventNo, fromAt, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoBetweenRequired(eventNo, fromAt, toAt);
            } else if (fromAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoFrom(eventNo, fromAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoFrom(eventNo, fromAt);
            } else {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoTo(eventNo, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoTo(eventNo, toAt);
            }
        }
        Set<Long> orderItemNoSet = new LinkedHashSet<>();
        orderItemNoSet.addAll(adminOrderItemNos);
        orderItemNoSet.addAll(partnerOrderItemNos);
        if (orderItemNoSet.isEmpty()) return List.of();
        List<OrderItemEntity> items = orderItemRepository.findAllWithOptionAndProductPartnerByIdIn(new java.util.ArrayList<>(orderItemNoSet));
        var grouped = items.stream()
                .filter(oi -> oi.getCompletedAt() != null)
                .filter(oi -> fromAt == null || !oi.getCompletedAt().isBefore(fromAt))
                .filter(oi -> toAt == null || !oi.getCompletedAt().isAfter(toAt))
                .collect(Collectors.groupingBy(oi -> oi.getCompletedAt().toLocalDate(),
                        java.util.TreeMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(e -> {
                    var list = e.getValue();
                    Long orders = list.stream().map(oi -> oi.getOrder().getOrderNo()).filter(java.util.Objects::nonNull).distinct().count();
                    Long itemsCnt = (long) list.size();
                    Long net = list.stream().map(OrderItemEntity::getItemTotalPrice).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
                    return EventTimeseriesPointDto.builder()
                            .date(e.getKey())
                            .orders(orders)
                            .items(itemsCnt)
                            .netAmount(net)
                            .adminReward(0L)
                            .partnerReward(0L)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<EventTopItemDto> getTop(Long eventNo, String type, Integer limit, LocalDateTime fromAt, LocalDateTime toAt) {
        int topN = (limit != null && limit > 0) ? limit : 10;
        List<Long> adminOrderItemNos;
        List<Long> partnerOrderItemNos;
        if (eventNo == null) {
            List<Long> endedEventNos = eventRepository.findByEventStatus(EventStatus.ENDED)
                    .stream().map(EventEntity::getEventNo).toList();
            if (endedEventNos.isEmpty()) return List.of();
            if (fromAt == null && toAt == null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNos(endedEventNos);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNos(endedEventNos);
            } else if (fromAt != null && toAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNosAndCompletedBetweenRequired(endedEventNos, fromAt, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedBetweenRequired(endedEventNos, fromAt, toAt);
            } else if (fromAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNosAndCompletedFrom(endedEventNos, fromAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedFrom(endedEventNos, fromAt);
            } else {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNosAndCompletedTo(endedEventNos, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedTo(endedEventNos, toAt);
            }
        } else {
            if (fromAt == null && toAt == null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoOnly(eventNo);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoOnly(eventNo);
            } else if (fromAt != null && toAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoBetweenRequired(eventNo, fromAt, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoBetweenRequired(eventNo, fromAt, toAt);
            } else if (fromAt != null) {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoFrom(eventNo, fromAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoFrom(eventNo, fromAt);
            } else {
                adminOrderItemNos = adminEventRewardRepository.findOrderItemNosByEventNoTo(eventNo, toAt);
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoTo(eventNo, toAt);
            }
        }
        Set<Long> orderItemNoSet = new LinkedHashSet<>();
        orderItemNoSet.addAll(adminOrderItemNos);
        orderItemNoSet.addAll(partnerOrderItemNos);
        if (orderItemNoSet.isEmpty()) return List.of();
        List<OrderItemEntity> items = orderItemRepository.findAllWithOptionAndProductPartnerByIdIn(new java.util.ArrayList<>(orderItemNoSet));

        if ("partner".equalsIgnoreCase(type)) {
            var byPartner = items.stream().collect(Collectors.groupingBy(oi -> {
                if (oi.getOption() != null && oi.getOption().getPartner() != null) {
                    return String.valueOf(oi.getOption().getPartner().getPartnerId());
                }
                if (oi.getProduct() != null && oi.getProduct().getPartner() != null) {
                    return String.valueOf(oi.getProduct().getPartner().getPartnerId());
                }
                return "unknown";
            }));
            return byPartner.entrySet().stream().map(e -> {
                var list = e.getValue();
                Long orders = list.stream().map(oi -> oi.getOrder().getOrderNo()).filter(java.util.Objects::nonNull).distinct().count();
                Long itemsCnt = (long) list.size();
                Long net = list.stream().map(OrderItemEntity::getItemTotalPrice).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
                String name = "파트너 " + e.getKey();
                if (!list.isEmpty()) {
                    var p = (list.get(0).getOption() != null && list.get(0).getOption().getPartner() != null)
                            ? list.get(0).getOption().getPartner()
                            : (list.get(0).getProduct() != null ? list.get(0).getProduct().getPartner() : null);
                    if (p != null && p.getPartnerName() != null) name = p.getPartnerName();
                }
                return EventTopItemDto.builder()
                        .id(e.getKey())
                        .name(name)
                        .orders(orders)
                        .items(itemsCnt)
                        .netAmount(net)
                        .reward(0L)
                        .build();
            }).sorted((a, b) -> Long.compare(b.getNetAmount(), a.getNetAmount()))
                    .limit(topN).collect(Collectors.toList());
        } else {
            // 옵션이 기본, 옵션이 없으면 상품 기준으로 그룹핑
            var byOptionOrProduct = items.stream().collect(Collectors.groupingBy(oi -> {
                if (oi.getOption() != null && oi.getOption().getOptionNo() != null) {
                    return "OPT_" + oi.getOption().getOptionNo();
                }
                if (oi.getProduct() != null && oi.getProduct().getProductNo() != null) {
                    return "PROD_" + oi.getProduct().getProductNo();
                }
                return "unknown";
            }));
            return byOptionOrProduct.entrySet().stream().map(e -> {
                var list = e.getValue();
                Long orders = list.stream().map(oi -> oi.getOrder().getOrderNo()).filter(java.util.Objects::nonNull).distinct().count();
                Long itemsCnt = (long) list.size();
                Long net = list.stream().map(OrderItemEntity::getItemTotalPrice).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();

                String name = "상품";
                if (!list.isEmpty()) {
                    var oi = list.get(0);
                    String productName = (oi.getProduct() != null && oi.getProduct().getProductName() != null)
                            ? oi.getProduct().getProductName()
                            : "상품";
                    if (oi.getOption() != null) {
                        String color = oi.getOption().getColor();
                        String size = oi.getOption().getSize();
                        String optLabel;
                        if (color != null && size != null) {
                            optLabel = color + " / " + size;
                        } else if (color != null) {
                            optLabel = color;
                        } else if (size != null) {
                            optLabel = size;
                        } else {
                            optLabel = "옵션";
                        }
                        name = productName + " - " + optLabel;
                    } else {
                        name = productName;
                    }
                }

                return EventTopItemDto.builder()
                        .id(e.getKey())
                        .name(name)
                        .orders(orders)
                        .items(itemsCnt)
                        .netAmount(net)
                        .reward(0L)
                        .build();
            }).sorted((a, b) -> Long.compare(b.getNetAmount(), a.getNetAmount()))
                    .limit(topN).collect(Collectors.toList());
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<EventTimeseriesPointDto> getPartnerTimeseries(Long partnerId, Long eventNo, LocalDateTime fromAt, LocalDateTime toAt) {
        List<Long> partnerOrderItemNos;
        if (eventNo == null) {
            // 파트너가 보상을 받은 종료 이벤트 전체
            List<Long> partnerEventNos = partnerEventRewardRepository.findEventNosByPartnerId(partnerId);
            if (partnerEventNos.isEmpty()) return List.of();
            if (fromAt == null && toAt == null) {
                // 모든 보상 아이템
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNos(partnerEventNos);
            } else if (fromAt != null && toAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedBetweenRequired(partnerEventNos, fromAt, toAt);
            } else if (fromAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedFrom(partnerEventNos, fromAt);
            } else {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedTo(partnerEventNos, toAt);
            }
        } else {
            if (fromAt == null && toAt == null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdOnly(eventNo, partnerId);
            } else if (fromAt != null && toAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdBetweenRequired(eventNo, partnerId, fromAt, toAt);
            } else if (fromAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdFrom(eventNo, partnerId, fromAt);
            } else {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdTo(eventNo, partnerId, toAt);
            }
        }
        if (partnerOrderItemNos.isEmpty()) return List.of();
        List<OrderItemEntity> items = orderItemRepository.findAllWithOptionAndProductPartnerByIdIn(new java.util.ArrayList<>(partnerOrderItemNos));
        var grouped = items.stream()
                .filter(oi -> oi.getCompletedAt() != null)
                .filter(oi -> fromAt == null || !oi.getCompletedAt().isBefore(fromAt))
                .filter(oi -> toAt == null || !oi.getCompletedAt().isAfter(toAt))
                .collect(Collectors.groupingBy(oi -> oi.getCompletedAt().toLocalDate(), java.util.TreeMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(e -> {
            var list = e.getValue();
            Long orders = list.stream().map(oi -> oi.getOrder().getOrderNo()).filter(java.util.Objects::nonNull).distinct().count();
            Long itemsCnt = (long) list.size();
            Long net = list.stream().map(OrderItemEntity::getItemTotalPrice).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
            return EventTimeseriesPointDto.builder()
                    .date(e.getKey())
                    .orders(orders)
                    .items(itemsCnt)
                    .netAmount(net)
                    .adminReward(0L)
                    .partnerReward(0L)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<EventTopItemDto> getPartnerTop(Long partnerId, Long eventNo, String type, Integer limit, LocalDateTime fromAt, LocalDateTime toAt) {
        int topN = (limit != null && limit > 0) ? limit : 10;
        List<Long> partnerOrderItemNos;
        if (eventNo == null) {
            List<Long> partnerEventNos = partnerEventRewardRepository.findEventNosByPartnerId(partnerId);
            if (partnerEventNos.isEmpty()) return List.of();
            if (fromAt == null && toAt == null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNos(partnerEventNos);
            } else if (fromAt != null && toAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedBetweenRequired(partnerEventNos, fromAt, toAt);
            } else if (fromAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedFrom(partnerEventNos, fromAt);
            } else {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNosAndCompletedTo(partnerEventNos, toAt);
            }
        } else {
            if (fromAt == null && toAt == null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdOnly(eventNo, partnerId);
            } else if (fromAt != null && toAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdBetweenRequired(eventNo, partnerId, fromAt, toAt);
            } else if (fromAt != null) {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdFrom(eventNo, partnerId, fromAt);
            } else {
                partnerOrderItemNos = partnerEventRewardRepository.findOrderItemNosByEventNoAndPartnerIdTo(eventNo, partnerId, toAt);
            }
        }
        if (partnerOrderItemNos.isEmpty()) return List.of();
        List<OrderItemEntity> items = orderItemRepository.findAllWithOptionAndProductPartnerByIdIn(new java.util.ArrayList<>(partnerOrderItemNos));
        if ("product".equalsIgnoreCase(type)) {
            var byOptionOrProduct = items.stream().collect(Collectors.groupingBy(oi -> {
                if (oi.getOption() != null && oi.getOption().getOptionNo() != null) return "OPT_" + oi.getOption().getOptionNo();
                if (oi.getProduct() != null && oi.getProduct().getProductNo() != null) return "PROD_" + oi.getProduct().getProductNo();
                return "unknown";
            }));
            return byOptionOrProduct.entrySet().stream().map(e -> {
                var list = e.getValue();
                Long orders = list.stream().map(oi -> oi.getOrder().getOrderNo()).filter(java.util.Objects::nonNull).distinct().count();
                Long itemsCnt = (long) list.size();
                Long net = list.stream().map(OrderItemEntity::getItemTotalPrice).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
                String name = "상품";
                if (!list.isEmpty()) {
                    var oi = list.get(0);
                    String productName = (oi.getProduct() != null && oi.getProduct().getProductName() != null) ? oi.getProduct().getProductName() : "상품";
                    if (oi.getOption() != null) {
                        String color = oi.getOption().getColor();
                        String size = oi.getOption().getSize();
                        String optLabel = (color != null && size != null) ? (color + " / " + size) : (color != null ? color : (size != null ? size : "옵션"));
                        name = productName + " - " + optLabel;
                    } else {
                        name = productName;
                    }
                }
                return EventTopItemDto.builder().id(e.getKey()).name(name).orders(orders).items(itemsCnt).netAmount(net).reward(0L).build();
            }).sorted((a, b) -> Long.compare(b.getNetAmount(), a.getNetAmount())).limit(topN).collect(Collectors.toList());
        } else {
            var byPartner = items.stream().collect(Collectors.groupingBy(oi -> {
                if (oi.getOption() != null && oi.getOption().getPartner() != null) return String.valueOf(oi.getOption().getPartner().getPartnerId());
                if (oi.getProduct() != null && oi.getProduct().getPartner() != null) return String.valueOf(oi.getProduct().getPartner().getPartnerId());
                return "unknown";
            }));
            return byPartner.entrySet().stream().map(e -> {
                var list = e.getValue();
                Long orders = list.stream().map(oi -> oi.getOrder().getOrderNo()).filter(java.util.Objects::nonNull).distinct().count();
                Long itemsCnt = (long) list.size();
                Long net = list.stream().map(OrderItemEntity::getItemTotalPrice).filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
                String name = "파트너 " + e.getKey();
                if (!list.isEmpty()) {
                    var p = (list.get(0).getOption() != null && list.get(0).getOption().getPartner() != null)
                            ? list.get(0).getOption().getPartner()
                            : (list.get(0).getProduct() != null ? list.get(0).getProduct().getPartner() : null);
                    if (p != null && p.getPartnerName() != null) name = p.getPartnerName();
                }
                return EventTopItemDto.builder().id(e.getKey()).name(name).orders(orders).items(itemsCnt).netAmount(net).reward(0L).build();
            }).sorted((a, b) -> Long.compare(b.getNetAmount(), a.getNetAmount())).limit(topN).collect(Collectors.toList());
        }
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public java.util.List<EventPerformanceListItemDto> getEndedEventsPerformance(LocalDateTime fromAt, LocalDateTime toAt) {
        // 상태가 ENDED인 모든 이벤트(조기 종료 포함)
        List<EventEntity> ended = eventRepository.findByEventStatus(EventStatus.ENDED);
        return ended.stream().map(e -> {
            EventPerformanceResponseDto p = getAdminPerformance(e.getEventNo(), fromAt, toAt);
            return EventPerformanceListItemDto.builder()
                    .eventNo(e.getEventNo())
                    .eventTitle(e.getEventTitle())
                    .eventStatus(e.getEventStatus() != null ? e.getEventStatus().name() : null)
                    .customerEventStartAt(e.getCustomerEventStartAt())
                    .customerEventEndAt(e.getCustomerEventEndAt())
                    .totalOrders(p.getTotalOrders())
                    .totalOrderItems(p.getTotalOrderItems())
                    .totalNetAmount(p.getTotalNetAmount())
                    .adminRewardPoint(p.getAdminRewardPoint())
                    .partnerRewardPoint(p.getPartnerRewardPoint())
                    .rewardToNetRatio(p.getRewardToNetRatio())
                    .payoutType(p.getPayoutType())
                    .build();
        }).collect(Collectors.toList());
    }

    private Long safeLong(java.util.concurrent.Callable<Long> c) {
        try {
            Long v = c.call();
            return v != null ? v : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private <T> List<T> safeList(java.util.concurrent.Callable<List<T>> c) {
        try {
            List<T> v = c.call();
            return v != null ? v : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}

