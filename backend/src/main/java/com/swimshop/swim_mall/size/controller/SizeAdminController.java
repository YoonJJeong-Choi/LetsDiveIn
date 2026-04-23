package com.swimshop.swim_mall.size.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.size.entity.SizeEntity;
import com.swimshop.swim_mall.size.entity.SizeSynonymEntity;
import com.swimshop.swim_mall.size.service.SizeService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/admin/sizes")
@RequiredArgsConstructor
@RestController
public class SizeAdminController {

    private final AuthService authService;
    private final SizeService sizeService;

    @Getter @NoArgsConstructor @AllArgsConstructor
    static class CreateRequest {
        private String code;
        private String label;
        private Integer sortOrder;
        private Boolean isActive;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor
    static class UpdateStatusRequest {
        private Boolean isActive;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor
    static class AddSynonymRequest {
        private String synonym;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(
            HttpSession session, @RequestBody CreateRequest req) {
        authService.requireRole(session, AccountRole.ADMIN);
        SizeEntity saved = sizeService.createSize(req.getCode(), req.getLabel(), req.getSortOrder(), req.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of(
                        "code", saved.getCode(),
                        "label", saved.getLabel(),
                        "isActive", saved.getIsActive(),
                        "sortOrder", saved.getSortOrder()
                )
        ));
    }

    @PatchMapping("/{code}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            HttpSession session, @PathVariable String code, @RequestBody UpdateStatusRequest req) {
        authService.requireRole(session, AccountRole.ADMIN);
        sizeService.updateSizeStatus(code, req.getIsActive());
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    @PostMapping("/{code}/synonyms")
    public ResponseEntity<ApiResponse<Object>> addSynonym(
            HttpSession session, @PathVariable String code, @RequestBody AddSynonymRequest req) {
        authService.requireRole(session, AccountRole.ADMIN);
        SizeSynonymEntity s = sizeService.addSynonym(code, req.getSynonym());
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("id", s.getId(), "code", s.getSize().getCode(), "synonym", s.getSynonym())
        ));
    }

    @DeleteMapping("/synonyms/{synonymId}")
    public ResponseEntity<ApiResponse<String>> removeSyn(
            HttpSession session, @PathVariable Long synonymId) {
        authService.requireRole(session, AccountRole.ADMIN);
        sizeService.removeSynonym(synonymId);
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }
}
