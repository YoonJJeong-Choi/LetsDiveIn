package com.swimshop.swim_mall.ai.qnadraft.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.ai.qnadraft.entity.QnaDraftLogEntity;
import com.swimshop.swim_mall.ai.qnadraft.repository.QnaDraftLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QnaDraftLogWriter {

    private final QnaDraftLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Long qnaNo, String callerRole, Long adminId, Long partnerId,
                     String request, String response,
                     boolean success, String error, long latencyMs, String model) {
        try {
            logRepository.save(QnaDraftLogEntity.builder()
                    .qnaNo(qnaNo)
                    .callerRole(callerRole)
                    .adminId(adminId)
                    .partnerId(partnerId)
                    .requestPayload(request)
                    .responsePayload(response)
                    .success(success)
                    .errorMessage(error != null && error.length() > 1000 ? error.substring(0, 1000) : error)
                    .latencyMs(latencyMs)
                    .model(model)
                    .build());
        } catch (Exception e) {
            log.warn("QnA Draft 로그 저장 실패: {}", e.getMessage());
        }
    }
}
