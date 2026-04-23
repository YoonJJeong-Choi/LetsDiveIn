package com.swimshop.swim_mall.return_order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.common.enums.ReturnRiskTier;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;

@Service
public class ReturnFraudService {

    public FraudResult evaluate(ReturnEntity targetReturn, List<ReturnEntity> customerReturns) {
        if (targetReturn == null) {
            return new FraudResult(0, ReturnRiskTier.LOW, List.of("TARGET_RETURN_MISSING"));
        }

        List<ReturnEntity> safeReturns = customerReturns != null ? customerReturns : List.of();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since90 = now.minusDays(90);
        LocalDateTime since180 = now.minusDays(180);

        Long targetNo = targetReturn.getReturnNo();
        String targetAddr = safeLower(trim(targetReturn.getOrderItem().getOrder().getDeliveryAddress()));
        String targetAddrDetail = safeLower(trim(targetReturn.getOrderItem().getOrder().getDeliveryAddressDetail()));
        long targetAmount = targetReturn.getReturnAmount() != null ? targetReturn.getReturnAmount() : 0L;

        int recent90Count = 0;
        int sameAddressRepeatCount = 0;
        int rejectedCount = 0;
        int disputeLikeCount = 0;

        for (ReturnEntity it : safeReturns) {
            if (it == null || isSameReturn(targetNo, it.getReturnNo())) {
                continue;
            }
            LocalDateTime requestedAt = it.getReturnRequestedAt();
            if (requestedAt != null && !requestedAt.isBefore(since90)) {
                recent90Count++;
            }
            if (requestedAt != null && !requestedAt.isBefore(since180)) {
                String addr = safeLower(trim(it.getOrderItem().getOrder().getDeliveryAddress()));
                String detail = safeLower(trim(it.getOrderItem().getOrder().getDeliveryAddressDetail()));
                if (!targetAddr.isEmpty() && targetAddr.equals(addr) && targetAddrDetail.equals(detail)) {
                    sameAddressRepeatCount++;
                }
            }
            if (it.getReturnStatus() == ReturnStatus.REJECTED) {
                rejectedCount++;
                String rejectionReason = safeLower(trim(it.getRejectionReason()));
                if (rejectionReason.contains("분쟁")
                        || rejectionReason.contains("이의")
                        || rejectionReason.contains("불가")) {
                    disputeLikeCount++;
                }
            }
        }

        int score = 0;
        List<String> factors = new ArrayList<>();

        if (recent90Count >= 3) {
            score += 30;
            factors.add("RECENT_RETURN_FREQUENCY_HIGH");
        } else if (recent90Count >= 1) {
            score += 15;
            factors.add("RECENT_RETURN_FREQUENCY");
        }

        boolean hasNonAddressSignal = score > 0;
        if (sameAddressRepeatCount >= 2) {
            // 동일 배송지 반복은 단독으로는 과탐 가능성이 있어 보조 신호로만 가중한다.
            if (hasNonAddressSignal) {
                score += 15;
            }
            factors.add("SAME_ADDRESS_REPEAT_HIGH");
        } else if (sameAddressRepeatCount >= 1) {
            if (hasNonAddressSignal) {
                score += 6;
            }
            factors.add("SAME_ADDRESS_REPEAT");
        }

        if (rejectedCount >= 2) {
            score += 20;
            factors.add("REJECTED_HISTORY_HIGH");
        } else if (rejectedCount >= 1) {
            score += 10;
            factors.add("REJECTED_HISTORY");
        }

        if (disputeLikeCount >= 1) {
            score += 15;
            factors.add("DISPUTE_LIKE_HISTORY");
        }

        if (targetAmount >= 100_000L) {
            score += 10;
            factors.add("HIGH_RETURN_AMOUNT");
        }

        score = Math.max(0, Math.min(100, score));
        ReturnRiskTier level = toTier(score);
        if (factors.isEmpty()) {
            factors.add("NO_STRONG_FRAUD_SIGNAL");
        }
        return new FraudResult(score, level, factors);
    }

    private static boolean isSameReturn(Long a, Long b) {
        return a != null && b != null && a.equals(b);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static ReturnRiskTier toTier(int score) {
        if (score >= 70) return ReturnRiskTier.HIGH;
        if (score >= 40) return ReturnRiskTier.MEDIUM;
        return ReturnRiskTier.LOW;
    }

    public record FraudResult(int fraudScore, ReturnRiskTier riskLevel, List<String> riskFactors) {
    }
}
