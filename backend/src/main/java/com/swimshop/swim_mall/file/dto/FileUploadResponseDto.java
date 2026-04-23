package com.swimshop.swim_mall.file.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponseDto {
    private Long fileId;
    private String fileUrl;
    private String originalName;
    private String storedName;
    private String contentType;
    private long size;
    private String category;
}
