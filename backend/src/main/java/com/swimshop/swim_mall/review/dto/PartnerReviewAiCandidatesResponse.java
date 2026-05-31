package com.swimshop.swim_mall.review.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 파트너 리뷰 AI 분석용: 상품·옵션·기간 조건에 맞는 최신 리뷰 목록과 메타.
 */
@Getter
@Builder
public class PartnerReviewAiCandidatesResponse {

    private final List<ReviewResponseDto> reviews;
    /** 설정 기본값 (recent-count) */
    private final int recentCountDefault;
    private final int minimumRequired;
    private final int hardCap;
    /** 실제 적용된 상한 건수 */
    private final int requestedLimit;
    /** DB에서 최신순으로 읽어온 후보 풀 크기 (옵션·기간 필터 전) */
    private final int poolScanned;
}
