package com.swimshop.swim_mall.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.admin.dto.AdminAiUsageOverviewDto;
import com.swimshop.swim_mall.ai.common.repository.AiBlockedLogRepository;
import com.swimshop.swim_mall.ai.qnadraft.entity.QnaDraftLogEntity;
import com.swimshop.swim_mall.ai.qnadraft.repository.QnaDraftLogRepository;
import com.swimshop.swim_mall.ai.reviewanalysis.entity.ReviewAnalysisLogEntity;
import com.swimshop.swim_mall.ai.reviewanalysis.repository.ReviewAnalysisLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAiOpsService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QnaDraftLogRepository qnaDraftLogRepository;
    private final ReviewAnalysisLogRepository reviewAnalysisLogRepository;
    private final AiBlockedLogRepository aiBlockedLogRepository;

    @Value("${ai.qna-draft.daily-limit-admin:30}")
    private int qnaDraftAdminLimit;

    @Value("${ai.qna-draft.daily-limit-partner:20}")
    private int qnaDraftPartnerLimit;

    @Value("${ai.review-analysis.daily-limit-partner:10}")
    private int reviewAnalysisPartnerLimit;

    @Transactional(readOnly = true)
    public AdminAiUsageOverviewDto getOverview() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayQna = qnaDraftLogRepository.countByCreatedAtGreaterThanEqual(startOfToday);
        long todayReview = reviewAnalysisLogRepository.countByCreatedAtGreaterThanEqual(startOfToday);
        long monthQna = qnaDraftLogRepository.countByCreatedAtGreaterThanEqual(startOfMonth);
        long monthReview = reviewAnalysisLogRepository.countByCreatedAtGreaterThanEqual(startOfMonth);

        long todayBlocked = aiBlockedLogRepository.countByCreatedAtGreaterThanEqual(startOfToday);
        long monthBlocked = aiBlockedLogRepository.countByCreatedAtGreaterThanEqual(startOfMonth);

        List<AdminAiUsageOverviewDto.RoleBlockedDto> blockedByRoleToday = aiBlockedLogRepository.countByRoleSince(startOfToday)
                .stream()
                .map(row -> AdminAiUsageOverviewDto.RoleBlockedDto.builder()
                        .role((String) row[0])
                        .count((Long) row[1])
                        .build())
                .toList();

        List<AdminAiUsageOverviewDto.AccountBlockedDto> blockedByAccountToday = aiBlockedLogRepository.countByRoleAndAccountSince(startOfToday)
                .stream()
                .limit(10)
                .map(row -> AdminAiUsageOverviewDto.AccountBlockedDto.builder()
                        .role((String) row[0])
                        .accountId((String) row[1])
                        .count((Long) row[2])
                        .build())
                .toList();

        List<AdminAiUsageOverviewDto.FailureLogDto> recentFailures = loadRecentFailures();

        return AdminAiUsageOverviewDto.builder()
                .todayCalls(AdminAiUsageOverviewDto.UsageCountDto.builder()
                        .qnaDraft(todayQna)
                        .reviewAnalysis(todayReview)
                        .total(todayQna + todayReview)
                        .build())
                .monthCalls(AdminAiUsageOverviewDto.UsageCountDto.builder()
                        .qnaDraft(monthQna)
                        .reviewAnalysis(monthReview)
                        .total(monthQna + monthReview)
                        .build())
                .todayBlocked(AdminAiUsageOverviewDto.BlockedCountDto.builder().total(todayBlocked).build())
                .monthBlocked(AdminAiUsageOverviewDto.BlockedCountDto.builder().total(monthBlocked).build())
                .dailyLimits(AdminAiUsageOverviewDto.DailyLimitDto.builder()
                        .qnaDraftAdmin(qnaDraftAdminLimit)
                        .qnaDraftPartner(qnaDraftPartnerLimit)
                        .reviewAnalysisPartner(reviewAnalysisPartnerLimit)
                        .build())
                .blockedByRoleToday(blockedByRoleToday)
                .blockedByAccountToday(blockedByAccountToday)
                .recentFailures(recentFailures)
                .build();
    }

    private List<AdminAiUsageOverviewDto.FailureLogDto> loadRecentFailures() {
        List<AdminAiUsageOverviewDto.FailureLogDto> merged = new ArrayList<>();

        List<QnaDraftLogEntity> qnaFailures = qnaDraftLogRepository.findRecentFailures();
        for (QnaDraftLogEntity log : qnaFailures) {
            merged.add(AdminAiUsageOverviewDto.FailureLogDto.builder()
                    .feature("QNA_DRAFT")
                    .role(log.getCallerRole() != null ? log.getCallerRole() : "-")
                    .accountId(resolveQnaAccountId(log))
                    .errorMessage(log.getErrorMessage())
                    .createdAt(log.getCreatedAt().format(TIME_FORMAT))
                    .build());
        }

        List<ReviewAnalysisLogEntity> reviewFailures = reviewAnalysisLogRepository.findRecentFailures();
        for (ReviewAnalysisLogEntity log : reviewFailures) {
            merged.add(AdminAiUsageOverviewDto.FailureLogDto.builder()
                    .feature("REVIEW_ANALYSIS")
                    .role("PARTNER")
                    .accountId(log.getPartnerId())
                    .errorMessage(log.getErrorMessage())
                    .createdAt(log.getCreatedAt().format(TIME_FORMAT))
                    .build());
        }

        return merged.stream()
                .sorted(Comparator.comparing(AdminAiUsageOverviewDto.FailureLogDto::getCreatedAt).reversed())
                .limit(20)
                .toList();
    }

    private String resolveQnaAccountId(QnaDraftLogEntity log) {
        if ("ADMIN".equals(log.getCallerRole()) && log.getAdminId() != null) {
            return String.valueOf(log.getAdminId());
        }
        if ("PARTNER".equals(log.getCallerRole()) && log.getPartnerId() != null) {
            return String.valueOf(log.getPartnerId());
        }
        return "-";
    }
}
