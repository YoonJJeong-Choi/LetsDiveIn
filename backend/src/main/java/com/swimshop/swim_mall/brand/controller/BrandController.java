package com.swimshop.swim_mall.brand.controller;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.brand.entity.BrandEntity;
import com.swimshop.swim_mall.brand.repository.BrandRepository;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/brands")
@RequiredArgsConstructor
@RestController
public class BrandController {

    private final BrandRepository brandRepository;
    private final PartnerRepository partnerRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Object>>> getActiveBrands() {
        List<BrandEntity> brands = brandRepository.findByIsActiveTrueOrderBySortOrderAscDisplayNameAsc();
        List<Object> resp;

        if (brands.isEmpty()) {
            // brand 마스터가 비어있는 초기/이행 환경에서는 파트너 대표 브랜드 코드로 fallback 제공
            resp = partnerRepository.findAll().stream()
                    .map(p -> p.getRepresentativeBrandCode())
                    .filter(code -> code != null && !code.isBlank())
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .distinct()
                    .sorted()
                    .map(code -> {
                        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                        row.put("code", code);
                        row.put("displayName", code);
                        row.put("slug", null);
                        row.put("logoUrl", null);
                        row.put("country", null);
                        row.put("sortOrder", 999);
                        return row;
                    })
                    .collect(Collectors.toList());
        } else {
            resp = brands.stream().map(b -> {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                row.put("code", b.getCode());
                row.put("displayName", b.getDisplayName());
                row.put("slug", b.getSlug());
                row.put("logoUrl", b.getLogoUrl());
                row.put("country", b.getCountry());
                row.put("sortOrder", b.getSortOrder());
                return row;
            }).collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}