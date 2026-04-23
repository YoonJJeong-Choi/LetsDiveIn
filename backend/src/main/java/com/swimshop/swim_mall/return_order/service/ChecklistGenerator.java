package com.swimshop.swim_mall.return_order.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.common.enums.ReturnRiskTier;

@Service
public class ChecklistGenerator {

    public List<String> generate(
            ReturnFraudService.FraudResult fraud,
            ReturnImageAnalysisService.EvidenceResult evidence) {
        List<String> actions = new ArrayList<>();
        int score = fraud != null ? fraud.fraudScore() : 0;
        ReturnRiskTier level = fraud != null ? fraud.riskLevel() : ReturnRiskTier.LOW;
        List<String> gaps = evidence != null && evidence.evidenceGaps() != null ? evidence.evidenceGaps() : List.of();
        List<String> factors = fraud != null && fraud.riskFactors() != null ? fraud.riskFactors() : List.of();

        actions.add("주문/배송/반품 이력을 수동으로 재확인하세요.");

        if (level == ReturnRiskTier.HIGH || score >= 70) {
            actions.add("고위험 건으로 분류되어 관리자 2차 검토를 진행하세요.");
            actions.add("동일 수령지·반품 반복 여부를 주문 이력에서 교차 확인하세요.");
        } else if (level == ReturnRiskTier.MEDIUM) {
            actions.add("중위험 건으로 분류되어 증빙과 사유 일치 여부를 우선 확인하세요.");
        }

        if (!gaps.isEmpty()) {
            actions.add("증빙 누락 항목이 있어 고객에게 추가자료 요청을 권장합니다.");
        }
        if (factors.contains("DISPUTE_LIKE_HISTORY")) {
            actions.add("과거 분쟁성 이력이 있어 CS 기록 확인 후 처리하세요.");
        }
        if (actions.size() == 1) {
            actions.add("현재 신호는 낮지만 최종 승인/거절은 관리자 판단으로 진행하세요.");
        }
        return actions;
    }
}
