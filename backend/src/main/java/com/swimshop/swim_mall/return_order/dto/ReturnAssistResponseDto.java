package com.swimshop.swim_mall.return_order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnAssistResponseDto {
    private String summary;
    private ReviewPriority reviewPriority;
    private EvidenceStatus evidenceStatus;
    private List<String> checkPoints;

    private Signals signals;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReviewPriority {
        private String code;
        private String label;
        private String reason;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class EvidenceStatus {
        private String code;
        private String label;
        private String detail;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Signals {
        private List<String> customerSignals;
    }
}
