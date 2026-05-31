package com.swimshop.swim_mall.admin.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminAiUsageOverviewDto {

    private UsageCountDto todayCalls;
    private UsageCountDto monthCalls;
    private BlockedCountDto todayBlocked;
    private BlockedCountDto monthBlocked;
    private DailyLimitDto dailyLimits;
    private List<RoleBlockedDto> blockedByRoleToday;
    private List<AccountBlockedDto> blockedByAccountToday;
    private List<FailureLogDto> recentFailures;

    @Getter
    @Builder
    public static class UsageCountDto {
        private long qnaDraft;
        private long reviewAnalysis;
        private long total;
    }

    @Getter
    @Builder
    public static class BlockedCountDto {
        private long total;
    }

    @Getter
    @Builder
    public static class DailyLimitDto {
        private int qnaDraftAdmin;
        private int qnaDraftPartner;
        private int reviewAnalysisPartner;
    }

    @Getter
    @Builder
    public static class RoleBlockedDto {
        private String role;
        private long count;
    }

    @Getter
    @Builder
    public static class AccountBlockedDto {
        private String role;
        private String accountId;
        private long count;
    }

    @Getter
    @Builder
    public static class FailureLogDto {
        private String feature;
        private String role;
        private String accountId;
        private String errorMessage;
        private String createdAt;
    }
}
