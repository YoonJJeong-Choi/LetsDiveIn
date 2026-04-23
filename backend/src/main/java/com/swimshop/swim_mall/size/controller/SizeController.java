package com.swimshop.swim_mall.size.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.size.entity.SizeEntity;
import com.swimshop.swim_mall.size.repository.SizeRepository;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/sizes")
@RequiredArgsConstructor
@RestController
public class SizeController {

    private final SizeRepository sizeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Object>>> getActiveSizes() {
        List<SizeEntity> sizes = sizeRepository.findByIsActiveTrueOrderBySortOrderAscLabelAsc();
        List<Object> resp = sizes.stream().map(s -> java.util.Map.of(
                "code", s.getCode(),
                "label", s.getLabel(),
                "sortOrder", s.getSortOrder()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}
