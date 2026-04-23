package com.swimshop.swim_mall.color.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.color.entity.ColorEntity;
import com.swimshop.swim_mall.color.entity.ColorSynonymEntity;
import com.swimshop.swim_mall.color.repository.ColorRepository;
import com.swimshop.swim_mall.color.repository.ColorSynonymRepository;
import com.swimshop.swim_mall.color.service.ColorService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/admin/colors")
@RequiredArgsConstructor
@RestController
public class ColorAdminController {

    private final AuthService authService;
    private final ColorService colorService;
    private final ColorRepository colorRepository;
    private final ColorSynonymRepository colorSynonymRepository;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class CreateColorRequest {
        private String code;
        private String label;
        private String hex;
        private Integer sortOrder;
        private Boolean isActive;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class UpdateStatusRequest {
        private Boolean isActive;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class AddSynonymRequest {
        private String synonym;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createColor(
            HttpSession session,
            @RequestBody CreateColorRequest req
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        ColorEntity saved = colorService.createColor(req.getCode(), req.getLabel(), req.getHex(), req.getSortOrder(), req.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of(
                        "code", saved.getCode(),
                        "label", saved.getLabel(),
                        "hex", saved.getHex(),
                        "isActive", saved.getIsActive(),
                        "sortOrder", saved.getSortOrder()
                )
        ));
    }

    @PatchMapping("/{code}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            HttpSession session,
            @PathVariable String code,
            @RequestBody UpdateStatusRequest req
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        colorService.updateColorStatus(code, req.getIsActive());
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    @PostMapping("/{code}/synonyms")
    public ResponseEntity<ApiResponse<Object>> addSynonym(
            HttpSession session,
            @PathVariable String code,
            @RequestBody AddSynonymRequest req
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        ColorSynonymEntity saved = colorService.addSynonym(code, req.getSynonym());
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of(
                        "id", saved.getId(),
                        "code", saved.getColor().getCode(),
                        "synonym", saved.getSynonym()
                )
        ));
    }

    @DeleteMapping("/synonyms/{synonymId}")
    public ResponseEntity<ApiResponse<String>> deleteSynonym(
            HttpSession session,
            @PathVariable Long synonymId
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        colorService.removeSynonym(synonymId);
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> listColors(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        List<ColorEntity> colors = colorRepository.findByIsActiveTrueOrderBySortOrderAscLabelAsc();
        List<Object> resp = colors.stream().map(c -> {
            List<String> synonyms = colorSynonymRepository.findByColor_Code(c.getCode()).stream()
                    .map(ColorSynonymEntity::getSynonym)
                    .toList();
            return java.util.Map.of(
                    "code", c.getCode(),
                    "label", c.getLabel(),
                    "isActive", c.getIsActive(),
                    "sortOrder", c.getSortOrder(),
                    "synonyms", synonyms
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}
