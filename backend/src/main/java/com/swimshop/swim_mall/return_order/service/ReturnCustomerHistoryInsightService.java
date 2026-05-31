package com.swimshop.swim_mall.return_order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;

/**
 * 동일 고객의 과거 반품 이력을 규칙으로 요약한다. 사기 점수·AI 판정은 제공하지 않는다.
 */
@Service
public class ReturnCustomerHistoryInsightService {

    public CustomerHistoryInsight summarize(ReturnEntity targetReturn, List<ReturnEntity> customerReturns) {
        if (targetReturn == null) {
            return new CustomerHistoryInsight(List.of("TARGET_RETURN_MISSING"));
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

        List<String> insightCodes = new ArrayList<>();

        if (recent90Count >= 3) {
            insightCodes.add("RECENT_RETURN_FREQUENCY_HIGH");
        } else if (recent90Count >= 1) {
            insightCodes.add("RECENT_RETURN_FREQUENCY");
        }

        if (sameAddressRepeatCount >= 2) {
            insightCodes.add("SAME_ADDRESS_REPEAT_HIGH");
        } else if (sameAddressRepeatCount >= 1) {
            insightCodes.add("SAME_ADDRESS_REPEAT");
        }

        if (rejectedCount >= 2) {
            insightCodes.add("REJECTED_HISTORY_HIGH");
        } else if (rejectedCount >= 1) {
            insightCodes.add("REJECTED_HISTORY");
        }

        if (disputeLikeCount >= 1) {
            insightCodes.add("DISPUTE_LIKE_HISTORY");
        }

        if (targetAmount >= 100_000L) {
            insightCodes.add("HIGH_RETURN_AMOUNT");
        }

        if (insightCodes.isEmpty()) {
            insightCodes.add("NO_SPECIAL_HISTORY");
        }
        return new CustomerHistoryInsight(List.copyOf(insightCodes));
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

    public record CustomerHistoryInsight(List<String> insightCodes) {
    }
}
