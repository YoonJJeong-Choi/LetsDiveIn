package com.swimshop.swim_mall.return_order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;

class ReturnCustomerHistoryInsightServiceTest {

    private final ReturnCustomerHistoryInsightService service = new ReturnCustomerHistoryInsightService();

    @Test
    void summarize_shouldReturnNoSpecialHistoryWhenOnlySameAddressRepeat() {
        ReturnEntity target = mockReturn(1L, "서울시 강남구", "101동", 50_000L);
        ReturnEntity historyOnlyAddress = mockReturn(
                2L,
                "서울시 강남구",
                "101동",
                30_000L,
                LocalDateTime.now().minusDays(30),
                ReturnStatus.REQUESTED);

        ReturnCustomerHistoryInsightService.CustomerHistoryInsight result =
                service.summarize(target, List.of(historyOnlyAddress));

        assertTrue(result.insightCodes().contains("SAME_ADDRESS_REPEAT"));
        assertEquals(false, result.insightCodes().contains("NO_SPECIAL_HISTORY"));
    }

    @Test
    void summarize_shouldIncludeRecentReturnFrequencyWithOtherSignals() {
        ReturnEntity target = mockReturn(1L, "서울시 서초구", "202동", 120_000L);
        ReturnEntity historyRecentAndSameAddress = mockReturn(
                3L,
                "서울시 서초구",
                "202동",
                40_000L,
                LocalDateTime.now().minusDays(10),
                ReturnStatus.REJECTED);

        ReturnCustomerHistoryInsightService.CustomerHistoryInsight result =
                service.summarize(target, List.of(historyRecentAndSameAddress));

        assertTrue(result.insightCodes().contains("RECENT_RETURN_FREQUENCY"));
        assertTrue(result.insightCodes().contains("HIGH_RETURN_AMOUNT"));
        assertEquals(false, result.insightCodes().contains("NO_SPECIAL_HISTORY"));
    }

    private ReturnEntity mockReturn(
            Long returnNo,
            String address,
            String addressDetail,
            long amount) {
        return mockReturn(returnNo, address, addressDetail, amount, LocalDateTime.now().minusDays(1), ReturnStatus.REQUESTED);
    }

    private ReturnEntity mockReturn(
            Long returnNo,
            String address,
            String addressDetail,
            long amount,
            LocalDateTime requestedAt,
            ReturnStatus status) {
        ReturnEntity entity = mock(ReturnEntity.class);
        OrderItemEntity orderItem = mock(OrderItemEntity.class);
        OrderEntity order = mock(OrderEntity.class);

        lenient().when(entity.getReturnNo()).thenReturn(returnNo);
        lenient().when(entity.getReturnAmount()).thenReturn(amount);
        lenient().when(entity.getReturnRequestedAt()).thenReturn(requestedAt);
        lenient().when(entity.getReturnStatus()).thenReturn(status);
        lenient().when(entity.getRejectionReason()).thenReturn(null);
        lenient().when(entity.getOrderItem()).thenReturn(orderItem);
        lenient().when(orderItem.getOrder()).thenReturn(order);
        when(order.getDeliveryAddress()).thenReturn(address);
        when(order.getDeliveryAddressDetail()).thenReturn(addressDetail);
        return entity;
    }
}
