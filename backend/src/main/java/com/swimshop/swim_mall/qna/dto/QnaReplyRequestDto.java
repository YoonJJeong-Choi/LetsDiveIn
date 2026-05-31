package com.swimshop.swim_mall.qna.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QnaReplyRequestDto {

    @NotBlank(message = "답변 내용은 필수입니다")
    private String body;
}
