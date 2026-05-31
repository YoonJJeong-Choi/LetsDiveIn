package com.swimshop.swim_mall.qna.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.ai.qnadraft.dto.QnaDraftAssistResponseDto;
import com.swimshop.swim_mall.ai.qnadraft.service.QnaDraftAssistService;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.qna.dto.QnaReplyRequestDto;
import com.swimshop.swim_mall.qna.dto.QnaResponseDto;
import com.swimshop.swim_mall.qna.service.QnaService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/partner/qna")
@RequiredArgsConstructor
@RestController
public class PartnerQnaController {

    private final QnaService qnaService;
    private final QnaDraftAssistService draftAssistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<QnaResponseDto>>> getPartnerQnaList(HttpSession session) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.getPartnerQnaList(session)));
    }

    @GetMapping("/{qnaNo}")
    public ResponseEntity<ApiResponse<QnaResponseDto>> getPartnerQnaDetail(
            HttpSession session,
            @PathVariable Long qnaNo) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.getPartnerQnaDetail(session, qnaNo)));
    }

    @PostMapping("/{qnaNo}/reply")
    public ResponseEntity<ApiResponse<QnaResponseDto>> reply(
            HttpSession session,
            @PathVariable Long qnaNo,
            @Valid @RequestBody QnaReplyRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.replyAsPartner(session, qnaNo, request)));
    }

    @PostMapping("/{qnaNo}/draft-assist")
    public ResponseEntity<ApiResponse<QnaDraftAssistResponseDto>> draftAssist(
            HttpSession session,
            @PathVariable Long qnaNo) {
        qnaService.getPartnerQnaDetail(session, qnaNo);
        Long partnerId = (Long) session.getAttribute("subjectId");
        return ResponseEntity.ok(ApiResponse.success(draftAssistService.generateDraft(qnaNo, "PARTNER", null, partnerId)));
    }
}
