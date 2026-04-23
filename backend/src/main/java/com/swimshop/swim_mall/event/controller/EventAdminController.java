package com.swimshop.swim_mall.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.event.dto.EventRequestDto;
import com.swimshop.swim_mall.event.dto.EventResponseDto;
import com.swimshop.swim_mall.event.dto.EventStatusUpdateRequestDto;
import com.swimshop.swim_mall.event.service.EventService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@RestController
public class EventAdminController {

    private final EventService eventService;

    /**
     * 이벤트 생성 (관리자)
     * POST /api/admin/events
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponseDto>> createEvent(
            HttpSession session,
            @Valid @RequestBody EventRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.createEvent(session, requestDto)));
    }

    /**
     * 이벤트 수정 (관리자)
     * PUT /api/admin/events/{eventNo}
     */
    @PutMapping("/{eventNo}")
    public ResponseEntity<ApiResponse<EventResponseDto>> updateEvent(
            HttpSession session,
            @PathVariable Long eventNo,
            @Valid @RequestBody EventRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.updateEvent(session, eventNo, requestDto)));
    }

    /**
     * 이벤트 상태 변경 (관리자)
     * PATCH /api/admin/events/{eventNo}/status
     */
    @PatchMapping("/{eventNo}/status")
    public ResponseEntity<ApiResponse<EventResponseDto>> updateEventStatus(
            HttpSession session,
            @PathVariable Long eventNo,
            @Valid @RequestBody EventStatusUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.updateEventStatus(session, eventNo, requestDto)));
    }
}
