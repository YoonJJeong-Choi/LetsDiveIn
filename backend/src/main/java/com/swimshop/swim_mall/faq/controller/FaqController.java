package com.swimshop.swim_mall.faq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.faq.dto.FaqRequestDto;
import com.swimshop.swim_mall.faq.dto.FaqResponseDto;
import com.swimshop.swim_mall.faq.dto.FaqUpdateRequestDto;
import com.swimshop.swim_mall.faq.service.FaqService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/faq")
@RequiredArgsConstructor
@RestController
public class FaqController {

    private final FaqService faqService;

    /**
     * FAQ 생성 (관리자만)
     * POST /api/faq
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FaqResponseDto>> createFaq(
            HttpSession session,
            @Valid @RequestBody FaqRequestDto requestDto) {
        FaqResponseDto faq = faqService.createFaq(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(faq));
    }

    /**
     * FAQ 목록 조회 (공개 API)
     * GET /api/faq?category=카테고리명 (선택)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqResponseDto>>> getFaqList(
            @RequestParam(required = false) String category) {
        List<FaqResponseDto> faqs = faqService.getFaqList(category);
        return ResponseEntity.ok(ApiResponse.success(faqs));
    }

    /**
     * FAQ 상세 조회 (공개 API)
     * GET /api/faq/{faqNo}
     */
    @GetMapping("/{faqNo}")
    public ResponseEntity<ApiResponse<FaqResponseDto>> getFaqDetail(
            @PathVariable Long faqNo) {
        FaqResponseDto faq = faqService.getFaqDetail(faqNo);
        return ResponseEntity.ok(ApiResponse.success(faq));
    }

    /**
     * FAQ 수정 (관리자만)
     * PUT /api/faq/{faqNo}
     */
    @PutMapping("/{faqNo}")
    public ResponseEntity<ApiResponse<FaqResponseDto>> updateFaq(
            HttpSession session,
            @PathVariable Long faqNo,
            @Valid @RequestBody FaqUpdateRequestDto requestDto) {
        FaqResponseDto faq = faqService.updateFaq(session, faqNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(faq));
    }

    /**
     * FAQ 삭제 (관리자만)
     * DELETE /api/faq/{faqNo}
     */
    @DeleteMapping("/{faqNo}")
    public ResponseEntity<ApiResponse<String>> deleteFaq(
            HttpSession session,
            @PathVariable Long faqNo) {
        faqService.deleteFaq(session, faqNo);
        return ResponseEntity.ok(ApiResponse.success("FAQ가 삭제되었습니다."));
    }
}
