package com.swimshop.swim_mall.event.partner.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.event.dto.EventResponseDto;
import com.swimshop.swim_mall.event.partner.service.PartnerEventService;
import com.swimshop.swim_mall.event.service.EventService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/partner/events")
@RequiredArgsConstructor
@RestController
public class PartnerEventController {

    private final PartnerEventService partnerEventService;
    private final EventService eventService;

    /**
     * 파트너 노출 이벤트 목록 조회
     * - eventMode = PARTNER_PARTICIPATION
     * - eventStatus IN (PUBLISHED, ENDED)
     * - optional: upcomingDays
     * GET /api/partner/events
     */
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getPartnerVisibleEvents(
            HttpSession session,
            @RequestParam(required = false) Integer upcomingDays,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        java.util.List<EventResponseDto> rows = eventService.getPartnerVisibleEvents(session, upcomingDays);
        int total = rows != null ? rows.size() : 0;
        int p = page != null ? page : 0; // 0-based 입력
        int s = size != null ? size : 10;
        int from = Math.max(0, Math.min(p * s, total));
        int to = Math.max(from, Math.min(from + s, total));
        java.util.List<EventResponseDto> slice = rows != null ? rows.subList(from, to) : java.util.List.of();
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("events", slice);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", p); // 0-based 그대로
        meta.put("size", s);
        meta.put("total", total);
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * 파트너 이벤트 참여 설정 (MVP: 매장 전체 적용)
     * POST /api/partner/events/{eventNo}/participation
     */
    @PostMapping("/{eventNo}/participation")
    public ResponseEntity<ApiResponse<Void>> participate(
            HttpSession session,
            @PathVariable Long eventNo
    ) {
        partnerEventService.setParticipation(session, eventNo, true);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 파트너 이벤트 참여 해제
     * DELETE /api/partner/events/{eventNo}/participation
     */
    @DeleteMapping("/{eventNo}/participation")
    public ResponseEntity<ApiResponse<Void>> cancelParticipation(
            HttpSession session,
            @PathVariable Long eventNo,
            @RequestParam(required = false, defaultValue = "false") boolean deactivateLinkedSales
    ) {
        partnerEventService.setParticipation(session, eventNo, false, deactivateLinkedSales);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // NOTE: 파트너 참여 여부 조회 API(GET /api/partner/events/{eventNo}/participation)는 미사용으로 제거했습니다.
}

