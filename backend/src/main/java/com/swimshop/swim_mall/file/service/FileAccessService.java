package com.swimshop.swim_mall.file.service;

import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;

/**
 * AI가 접근 가능한 파일 URL을 제공하는 추상화 계층.
 * 현재는 로컬 저장소 구현체를 사용하고, 추후 S3 구현체로 대체할 수 있다.
 */
public interface FileAccessService {

    String getAiReadableUrl(ReturnImageEntity returnImage);

    String getAiReadableUrl(Long fileId);
}
