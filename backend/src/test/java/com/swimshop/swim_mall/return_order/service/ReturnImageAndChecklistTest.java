package com.swimshop.swim_mall.return_order.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.swimshop.swim_mall.common.enums.ReturnReasonType;
import com.swimshop.swim_mall.common.enums.ReturnRiskTier;

class ReturnImageAndChecklistTest {

    private final ReturnImageAnalysisService returnImageAnalysisService = new ReturnImageAnalysisService();
    private final ChecklistGenerator checklistGenerator = new ChecklistGenerator();

    @Test
    void analyze_shouldMapEvidenceTagsFromImagesAndReasonText() {
        ReturnImageAnalysisService.EvidenceResult result = returnImageAnalysisService.analyze(
                List.of("https://cdn/image-1.jpg", "https://cdn/image-2.jpg"),
                ReturnReasonType.DEFECT,
                "상품 파손이 있어 사진 증빙을 첨부했습니다.");

        assertTrue(result.evidenceTags().contains("IMAGE_ATTACHED"));
        assertTrue(result.evidenceTags().contains("MULTI_ANGLE_IMAGE"));
        assertTrue(result.evidenceTags().contains("HAS_REASON_TEXT"));
        assertTrue(result.evidenceTags().contains("DAMAGE_CLAIM_MENTIONED"));
        assertTrue(result.evidenceTags().contains("EVIDENCE_REFERENCE_IN_TEXT"));
        assertFalse(result.evidenceGaps().contains("NO_IMAGE_EVIDENCE"));
        assertFalse(result.evidenceGaps().contains("REQUIRED_IMAGE_MISSING"));
    }

    @Test
    void analyze_shouldMapEvidenceGapsWhenImageAndReasonAreInsufficient() {
        ReturnImageAnalysisService.EvidenceResult result = returnImageAnalysisService.analyze(
                List.of(),
                ReturnReasonType.WRONG_ITEM,
                "불량");

        assertTrue(result.evidenceGaps().contains("NO_IMAGE_EVIDENCE"));
        assertTrue(result.evidenceGaps().contains("REQUIRED_IMAGE_MISSING"));
        assertTrue(result.evidenceGaps().contains("REASON_TEXT_TOO_SHORT"));
        assertTrue(result.evidenceTags().contains("EVIDENCE_SIGNAL_WEAK"));
    }

    @Test
    void generate_shouldNotContainConfidenceActionAfterConfidenceRemoval() {
        ReturnFraudService.FraudResult fraud = new ReturnFraudService.FraudResult(
                40,
                ReturnRiskTier.MEDIUM,
                List.of("SAME_ADDRESS_REPEAT_HIGH"));
        ReturnImageAnalysisService.EvidenceResult weakEvidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of(),
                List.of("NO_IMAGE_EVIDENCE", "REQUIRED_IMAGE_MISSING"));

        List<String> actions = checklistGenerator.generate(fraud, weakEvidence);

        assertTrue(actions.stream().anyMatch(action -> action.contains("추가자료 요청")));
        assertFalse(actions.stream().anyMatch(action -> action.contains("신뢰도")));
    }
}

