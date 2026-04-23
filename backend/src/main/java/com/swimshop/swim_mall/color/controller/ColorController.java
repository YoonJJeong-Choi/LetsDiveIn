package com.swimshop.swim_mall.color.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.color.entity.ColorEntity;
import com.swimshop.swim_mall.color.repository.ColorRepository;
import com.swimshop.swim_mall.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/colors")
@RequiredArgsConstructor
@RestController
public class ColorController {

    private final ColorRepository colorRepository;

    private String fallbackHex(String code) {
        if (code == null) return "#cccccc";
        String k = code.toUpperCase();
        return switch (k) {
            case "BLACK" -> "#000000";
            case "WHITE" -> "#FFFFFF";
            case "RED" -> "#FF3B30";
            case "BLUE" -> "#007AFF";
            case "NAVY" -> "#001F3F";
            case "GREEN" -> "#34C759";
            case "YELLOW" -> "#FFCC00";
            case "PINK" -> "#FF2D55";
            case "ORANGE" -> "#FF9500";
            case "GRAY", "GREY" -> "#8E8E93";
            default -> "#cccccc";
        };
    }

    /**
     * 활성 컬러 목록 (공개)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Object>>> getActiveColors() {
        List<ColorEntity> colors = colorRepository.findByIsActiveTrueOrderBySortOrderAscLabelAsc();
        List<Object> resp = colors.stream().map(c -> {
            // 주의: Map.of 는 null 값을 허용하지 않으므로 LinkedHashMap 사용
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("code", c.getCode());
            m.put("label", c.getLabel());
            String hex = c.getHex() != null ? c.getHex() : fallbackHex(c.getCode());
            m.put("hex", hex);
            m.put("sortOrder", c.getSortOrder());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}
