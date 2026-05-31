package com.swimshop.swim_mall.qna.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.qna.dto.InquiryCategoryResponseDto;

@RestController
@RequestMapping("/api/inquiry-categories")
public class InquiryCategoryController {

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryCategoryResponseDto>>> list() {
        List<InquiryCategoryResponseDto> items = Arrays.stream(InquiryCategory.values())
                .map(c -> new InquiryCategoryResponseDto(c.name(), c.getLabel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
