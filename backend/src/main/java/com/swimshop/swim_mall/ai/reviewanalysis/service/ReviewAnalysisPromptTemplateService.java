package com.swimshop.swim_mall.ai.reviewanalysis.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisRequestDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewItemDto;

@Component
public class ReviewAnalysisPromptTemplateService {

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "SWIMSUIT", List.of("수영복", "핏", "착용감", "사이즈", "신축", "어깨", "허리"),
            "GOGGLES", List.of("고글", "안경", "김서림", "시야", "렌즈", "물샘"),
            "CAP", List.of("수모", "모자", "압박", "머리", "늘어남"),
            "FINS", List.of("오리발", "핀", "발목", "발등", "추진"));

    public String buildSystemPrompt(ReviewAnalysisRequestDto requestDto) {
        String category = inferCategory(requestDto);
        String categoryGuideline = categoryGuideline(category);

        return "You are a partner review analysis assistant for Korean swim mall. "
                + "Output ONLY strict JSON with keys: "
                + "{\"summary\":\"...\"," 
                + "\"issues\":[{\"type\":\"...\",\"frequency\":0,\"severity\":\"LOW|MEDIUM|HIGH\",\"evidenceReviewNos\":[1]}],"
                + "\"actions\":[{\"area\":\"DETAIL|SIZE_GUIDE|IMAGES|PACKAGING|FAQ|CS_MACRO|INVENTORY\",\"recommendation\":\"...\"}],"
                + "\"topKeywords\":[{\"keyword\":\"...\",\"weight\":0.1}],"
                + "\"representativeQuotes\":[{\"reviewNo\":1,\"quote\":\"...\"}],"
                + "\"stats\":{\"count\":0,\"avgRating\":0.0,\"ratingDist1\":0,\"ratingDist2\":0,\"ratingDist3\":0,\"ratingDist4\":0,\"ratingDist5\":0},"
                + "\"alerts\":{\"pii\":false,\"spam\":false,\"policyViolation\":false,\"policyTypes\":[]}}. "
                + "No markdown, no explanation, no extra keys. "
                + "Language: Korean summary/recommendation. "
                + "Domain category hint: " + category + ". "
                + "Category guideline: " + categoryGuideline;
    }

    String inferCategory(ReviewAnalysisRequestDto requestDto) {
        if (requestDto == null || requestDto.getReviews() == null || requestDto.getReviews().isEmpty()) {
            return "GENERAL";
        }

        int swimsuit = 0;
        int goggles = 0;
        int cap = 0;
        int fins = 0;

        for (ReviewItemDto review : requestDto.getReviews()) {
            String content = review == null || review.getContent() == null ? "" : review.getContent().toLowerCase(Locale.ROOT);
            swimsuit += containsAny(content, CATEGORY_KEYWORDS.get("SWIMSUIT"));
            goggles += containsAny(content, CATEGORY_KEYWORDS.get("GOGGLES"));
            cap += containsAny(content, CATEGORY_KEYWORDS.get("CAP"));
            fins += containsAny(content, CATEGORY_KEYWORDS.get("FINS"));
        }

        int max = Math.max(Math.max(swimsuit, goggles), Math.max(cap, fins));
        if (max <= 0) {
            return "GENERAL";
        }
        if (max == swimsuit) {
            return "SWIMSUIT";
        }
        if (max == goggles) {
            return "GOGGLES";
        }
        if (max == cap) {
            return "CAP";
        }
        return "FINS";
    }

    private int containsAny(String content, List<String> keywords) {
        if (keywords == null) {
            return 0;
        }
        int score = 0;
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private String categoryGuideline(String category) {
        return switch (category) {
            case "SWIMSUIT" -> "Focus on size fit, stretch, and body-shape comfort.";
            case "GOGGLES" -> "Focus on anti-fog durability, leak issues, and field of view.";
            case "CAP" -> "Focus on pressure, slipping, and hair-pull complaints.";
            case "FINS" -> "Focus on foot pain, propulsion, and size mismatch.";
            default -> "Focus on repeated defects, quality, and delivery/packaging issues.";
        };
    }
}

