package com.swimshop.swim_mall.return_order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.swimshop.swim_mall.common.enums.ReturnRiskTier;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;

class ReturnFraudServiceTest {

    private final ReturnFraudService returnFraudService = new ReturnFraudService();

    @Test
    void evaluate_shouldKeepLowScoreWhenOnlySameAddressSignalExists() {
        ReturnEntity target = mockReturn(1L, "서울 강남구", "101동", LocalDateTime.now(), ReturnStatus.REQUESTED, null, 50_000L);
        ReturnEntity historyOnlyAddress = mockReturn(2L, "서울 강남구", "101동",
                LocalDateTime.now().minusDays(120), ReturnStatus.REQUESTED, null, 30_000L);

        ReturnFraudService.FraudResult result = returnFraudService.evaluate(target, List.of(historyOnlyAddress));

        assertEquals(0, result.fraudScore());
        assertEquals(ReturnRiskTier.LOW, result.riskLevel());
        assertTrue(result.riskFactors().contains("SAME_ADDRESS_REPEAT"));
    }

    @Test
    void evaluate_shouldIncreaseScoreWhenAddressSignalCombinesWithRecentReturn() {
        ReturnEntity target = mockReturn(10L, "서울 송파구", "202동", LocalDateTime.now(), ReturnStatus.REQUESTED, null, 50_000L);
        ReturnEntity historyRecentAndSameAddress = mockReturn(11L, "서울 송파구", "202동",
                LocalDateTime.now().minusDays(10), ReturnStatus.REQUESTED, null, 20_000L);

        ReturnFraudService.FraudResult result = returnFraudService.evaluate(target, List.of(historyRecentAndSameAddress));

        assertEquals(21, result.fraudScore()); // recent 15 + sameAddress 6 (조건부 가중)
        assertEquals(ReturnRiskTier.LOW, result.riskLevel());
        assertTrue(result.riskFactors().contains("RECENT_RETURN_FREQUENCY"));
        assertTrue(result.riskFactors().contains("SAME_ADDRESS_REPEAT"));
    }

    private ReturnEntity mockReturn(
            Long returnNo,
            String address,
            String addressDetail,
            LocalDateTime requestedAt,
            ReturnStatus status,
            String rejectionReason,
            long amount) {
        ReturnEntity returnEntity = mock(ReturnEntity.class);
        OrderItemEntity orderItem = mock(OrderItemEntity.class);
        OrderEntity order = mock(OrderEntity.class);
        CustomerEntity customer = mock(CustomerEntity.class);

        when(returnEntity.getReturnNo()).thenReturn(returnNo);
        when(returnEntity.getReturnRequestedAt()).thenReturn(requestedAt);
        when(returnEntity.getReturnStatus()).thenReturn(status);
        when(returnEntity.getRejectionReason()).thenReturn(rejectionReason);
        when(returnEntity.getReturnAmount()).thenReturn(amount);
        when(returnEntity.getOrderItem()).thenReturn(orderItem);

        when(orderItem.getOrder()).thenReturn(order);
        when(order.getDeliveryAddress()).thenReturn(address);
        when(order.getDeliveryAddressDetail()).thenReturn(addressDetail);
        when(order.getCustomer()).thenReturn(customer);
        return returnEntity;
    }
}

