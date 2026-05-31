package com.swimshop.swim_mall.return_order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.common.enums.ReturnReasonType;

@Service
public class ReturnImageAnalysisService {

    public EvidenceResult analyze(List<String> imageUrls, ReturnReasonType reasonType, String returnReason) {
        List<String> safeImages = imageUrls != null ? imageUrls.stream().filter(s -> s != null && !s.isBlank()).toList() : List.of();
        String reason = returnReason != null ? returnReason.trim() : "";
        String reasonLower = reason.toLowerCase(Locale.ROOT);
        boolean imageRequired = reasonType == ReturnReasonType.DEFECT
                || reasonType == ReturnReasonType.WRONG_ITEM
                || reasonType == ReturnReasonType.OTHER;
        boolean detailedNarrativeRequired = imageRequired;

        List<String> evidenceTags = new ArrayList<>();
        List<String> evidenceGaps = new ArrayList<>();

        if (!safeImages.isEmpty()) {
            evidenceTags.add("IMAGE_ATTACHED");
            if (safeImages.size() >= 2) {
                evidenceTags.add("MULTI_ANGLE_IMAGE");
            }
            if (safeImages.size() >= 4) {
                evidenceTags.add("SUFFICIENT_IMAGE_VOLUME");
            }
        } else if (imageRequired) {
            evidenceGaps.add("NO_IMAGE_EVIDENCE");
        }

        if (reason.length() >= 10) {
            evidenceTags.add("HAS_REASON_TEXT");
        } else if (detailedNarrativeRequired) {
            evidenceGaps.add("REASON_TEXT_TOO_SHORT");
        }

        if (reasonLower.contains("파손") || reasonLower.contains("깨짐") || reasonLower.contains("찢")) {
            evidenceTags.add("DAMAGE_CLAIM_MENTIONED");
        }
        if (reasonLower.contains("오배송") || reasonLower.contains("다른") || reasonLower.contains("옵션")) {
            evidenceTags.add("WRONG_ITEM_CLAIM_MENTIONED");
        }
        if (reasonLower.contains("사진") || reasonLower.contains("증빙")) {
            evidenceTags.add("EVIDENCE_REFERENCE_IN_TEXT");
        }

        if (imageRequired && safeImages.isEmpty()) {
            evidenceGaps.add("REQUIRED_IMAGE_MISSING");
        } else if (imageRequired && safeImages.size() == 1) {
            evidenceGaps.add("ADDITIONAL_IMAGE_RECOMMENDED");
        }

        if (evidenceTags.isEmpty() && (imageRequired || detailedNarrativeRequired)) {
            evidenceTags.add("EVIDENCE_SIGNAL_WEAK");
        }
        return new EvidenceResult(evidenceTags, evidenceGaps);
    }

    public record EvidenceResult(List<String> evidenceTags, List<String> evidenceGaps) {
    }
}
