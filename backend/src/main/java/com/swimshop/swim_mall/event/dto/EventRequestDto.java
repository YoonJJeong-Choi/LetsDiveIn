package com.swimshop.swim_mall.event.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.PointEventTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EventRequestDto {

    @NotBlank(message = "이벤트 제목은 필수입니다.")
    private String eventTitle;

    @NotBlank(message = "이벤트 내용은 필수입니다.")
    private String eventContent;

    @NotNull(message = "이벤트 상태는 필수입니다.")
    private EventStatus eventStatus;

    @NotNull(message = "고객 이벤트 시작일시는 필수입니다.")
    private LocalDateTime customerEventStartAt;

    @NotNull(message = "고객 이벤트 종료일시는 필수입니다.")
    private LocalDateTime customerEventEndAt;

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
    @NotNull(message = "이벤트 모드는 필수입니다.")
    private EventMode eventMode;
    private String adminMemo;
}
