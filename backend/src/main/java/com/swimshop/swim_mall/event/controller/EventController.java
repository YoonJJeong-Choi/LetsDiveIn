package com.swimshop.swim_mall.event.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.event.dto.EventParticipantDto;
import com.swimshop.swim_mall.event.dto.EventResponseDto;
import com.swimshop.swim_mall.event.service.EventService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/events")
@RequiredArgsConstructor
@RestController
public class EventController {

    private final EventService eventService;

    /**
     * 이벤트 목록 조회 (고객 공개)
     * GET /api/events
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponseDto>>> getPublicEvents() {
        return ResponseEntity.ok(ApiResponse.success(eventService.getPublicEvents()));
    }

    /**
     * 이벤트 상세 조회 (고객 공개)
     * GET /api/events/{eventNo}
     */
    @GetMapping("/{eventNo}")
    public ResponseEntity<ApiResponse<EventResponseDto>> getPublicEventDetail(@PathVariable Long eventNo) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getPublicEventDetail(eventNo)));
    }

    /**
     * 이벤트 목록 조회 (관리자)
     * GET /api/events/admin
     */
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAdminEvents(
            HttpSession session,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        java.util.List<EventResponseDto> rows = eventService.getAdminEvents(session, status, keyword, startAt, endAt);
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
     * 이벤트 상세 조회 (관리자)
     * GET /api/events/{eventNo}/admin
     */
    @GetMapping("/{eventNo}/admin")
    public ResponseEntity<ApiResponse<EventResponseDto>> getAdminEventDetail(
            HttpSession session,
            @PathVariable Long eventNo
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getAdminEventDetail(session, eventNo)));
    }

    /**
     * 이벤트 참여 파트너 목록 조회 (관리자)
     * GET /api/events/{eventNo}/admin/participants
     */
    @GetMapping("/{eventNo}/admin/participants")
    public ResponseEntity<ApiResponse<List<EventParticipantDto>>> getAdminEventParticipants(
            HttpSession session,
            @PathVariable Long eventNo
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getAdminEventParticipants(session, eventNo)));
    }
}
