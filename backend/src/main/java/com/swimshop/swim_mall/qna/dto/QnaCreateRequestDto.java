package com.swimshop.swim_mall.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QnaCreateRequestDto {

    @NotBlank(message = "category는 필수입니다")
    private String category;

    private String scope;

    @NotBlank(message = "title은 필수입니다")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "body는 필수입니다")
    private String body;

    private Long orderNo;

    private Long orderItemNo;
}
