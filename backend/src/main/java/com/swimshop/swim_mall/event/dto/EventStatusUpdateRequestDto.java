package com.swimshop.swim_mall.event.dto;

import com.swimshop.swim_mall.common.enums.EventStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EventStatusUpdateRequestDto {

    @NotNull(message = "변경할 이벤트 상태는 필수입니다.")
    private EventStatus eventStatus;
}
