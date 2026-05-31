package com.swimshop.swim_mall.qna.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.QnaAuthorType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QnaMessageResponseDto {

    private Long messageNo;
    private QnaAuthorType authorType;
    private String body;
    private String authorName;
    private LocalDateTime createdAt;
}
