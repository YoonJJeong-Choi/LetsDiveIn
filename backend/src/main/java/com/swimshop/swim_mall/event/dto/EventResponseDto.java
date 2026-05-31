package com.swimshop.swim_mall.event.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.PointEventTargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDto {
    private Long eventNo;
    private String eventTitle;
    private String eventContent;
    private EventStatus eventStatus;
    private String eventStatusLabel;
    private LocalDateTime customerEventStartAt;
    private LocalDateTime customerEventEndAt;
    private LocalDateTime customerExposeAt;
    private Boolean partnerApplyEnabled;
    private LocalDateTime partnerApplyStartAt;
    private LocalDateTime partnerApplyEndAt;
    private String thumbnailUrl;
    private EventType eventType;
    private DiscountType saleDiscountType;
    private Long saleDiscountValue;
    private Long saleMaxDiscountAmount;
    private PointEventTargetType pointEventTargetType;
    private List<String> pointEventTargetValues;
    private Long pointEventMinOrderAmount;
    private EventMode eventMode;
    private String adminMemo;
    private Long adminNo;
    private String adminName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean visibleToCustomerNow;
    private Boolean participationOpenNow;
    // 파트너의 이벤트 참여 신청(active) 상태
    private Boolean participationEnabled;
    // SALE 이벤트의 경우 "실제 세일정책 등록 완료" 상태를 나타내는 뷰용 필드
    private Boolean participating;
}
