package com.swimshop.swim_mall.qna.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.qna.dto.QnaCreateRequestDto;
import com.swimshop.swim_mall.qna.dto.QnaResponseDto;
import com.swimshop.swim_mall.qna.service.QnaService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/qna")
@RequiredArgsConstructor
@RestController
public class QnaController {

    private final QnaService qnaService;

    @PostMapping
    public ResponseEntity<ApiResponse<QnaResponseDto>> createQna(
            HttpSession session,
            @Valid @RequestBody QnaCreateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.createQna(session, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QnaResponseDto>>> getMyQnaList(
            HttpSession session,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.getCustomerQnaList(session, status, category)));
    }

    @GetMapping("/{qnaNo}")
    public ResponseEntity<ApiResponse<QnaResponseDto>> getMyQnaDetail(
            HttpSession session,
            @PathVariable Long qnaNo) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.getCustomerQnaDetail(session, qnaNo)));
    }
}
