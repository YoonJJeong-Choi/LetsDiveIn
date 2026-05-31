package com.swimshop.swim_mall.qna.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.enums.QnaScope;
import com.swimshop.swim_mall.common.enums.QnaStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QnaResponseDto {

    private Long qnaNo;
    private InquiryCategory category;
    private String categoryLabel;
    private QnaScope scope;
    private String scopeLabel;
    private QnaStatus status;
    private String statusLabel;
    private String title;
    private Long orderNo;
    private Long orderItemNo;
    private String productName;
    private Long partnerNo;
    private String partnerName;
    private String customerName;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
    private String lastMessagePreview;
    private List<QnaMessageResponseDto> messages;
}
