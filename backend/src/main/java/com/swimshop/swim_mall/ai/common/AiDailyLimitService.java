package com.swimshop.swim_mall.ai.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.ai.common.entity.AiBlockedLogEntity;
import com.swimshop.swim_mall.ai.common.repository.AiBlockedLogRepository;
import com.swimshop.swim_mall.ai.qnadraft.repository.QnaDraftLogRepository;
import com.swimshop.swim_mall.ai.reviewanalysis.repository.ReviewAnalysisLogRepository;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiDailyLimitService {

    private final QnaDraftLogRepository qnaDraftLogRepository;
    private final ReviewAnalysisLogRepository reviewAnalysisLogRepository;
    private final AiBlockedLogRepository aiBlockedLogRepository;

    @Value("${ai.daily-limit.enabled:true}")
    private boolean enabled;

    @Value("${ai.qna-draft.daily-limit-admin:30}")
    private int qnaAdminDailyLimit;

    @Value("${ai.qna-draft.daily-limit-partner:20}")
    private int qnaPartnerDailyLimit;

    @Value("${ai.review-analysis.daily-limit-partner:10}")
    private int reviewPartnerDailyLimit;

    public void assertQnaDraftAdminAllowed(Long adminId) {
        if (!enabled || adminId == null) {
            return;
        }
        long used = qnaDraftLogRepository.countByAdminIdAndCreatedAtGreaterThanEqual(adminId, startOfToday());
        if (used >= qnaAdminDailyLimit) {
            saveBlocked("QNA_DRAFT", "ADMIN", String.valueOf(adminId), "DAILY_LIMIT_EXCEEDED");
            throw new BusinessException(
                    ErrorCode.AI_DAILY_LIMIT_EXCEEDED,
                    String.format("오늘 AI 답변 초안 사용 한도(%d회)에 도달했습니다. 내일 다시 시도해 주세요.", qnaAdminDailyLimit));
        }
    }

    public void assertQnaDraftPartnerAllowed(Long partnerId) {
        if (!enabled || partnerId == null) {
            return;
        }
        long used = qnaDraftLogRepository.countByPartnerIdAndCreatedAtGreaterThanEqual(partnerId, startOfToday());
        if (used >= qnaPartnerDailyLimit) {
            saveBlocked("QNA_DRAFT", "PARTNER", String.valueOf(partnerId), "DAILY_LIMIT_EXCEEDED");
            throw new BusinessException(
                    ErrorCode.AI_DAILY_LIMIT_EXCEEDED,
                    String.format("오늘 AI 답변 초안 사용 한도(%d회)에 도달했습니다. 내일 다시 시도해 주세요.", qnaPartnerDailyLimit));
        }
    }

    public void assertReviewAnalysisPartnerAllowed(String partnerId) {
        if (!enabled || partnerId == null || partnerId.isBlank()) {
            return;
        }
        long used = reviewAnalysisLogRepository.countByPartnerIdAndCreatedAtGreaterThanEqual(partnerId, startOfToday());
        if (used >= reviewPartnerDailyLimit) {
            saveBlocked("REVIEW_ANALYSIS", "PARTNER", partnerId, "DAILY_LIMIT_EXCEEDED");
            throw new BusinessException(
                    ErrorCode.AI_DAILY_LIMIT_EXCEEDED,
                    String.format("오늘 리뷰 AI 분석 사용 한도(%d회)에 도달했습니다. 내일 다시 시도해 주세요.", reviewPartnerDailyLimit));
        }
    }

    private void saveBlocked(String feature, String role, String accountId, String reason) {
        aiBlockedLogRepository.save(AiBlockedLogEntity.builder()
                .feature(feature)
                .role(role)
                .accountId(accountId)
                .reason(reason)
                .build());
    }

    private static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }
}
