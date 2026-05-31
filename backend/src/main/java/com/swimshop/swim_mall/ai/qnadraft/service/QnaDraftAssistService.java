package com.swimshop.swim_mall.ai.qnadraft.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.ai.common.AiDailyLimitService;
import com.swimshop.swim_mall.ai.qnadraft.client.OpenAiQnaDraftClient;
import com.swimshop.swim_mall.ai.qnadraft.dto.QnaDraftAssistResponseDto;
import com.swimshop.swim_mall.common.enums.QnaAuthorType;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.faq.entity.FaqEntity;
import com.swimshop.swim_mall.faq.repository.FaqRepository;
import com.swimshop.swim_mall.qna.entity.QnaEntity;
import com.swimshop.swim_mall.qna.entity.QnaMessageEntity;
import com.swimshop.swim_mall.qna.repository.QnaMessageRepository;
import com.swimshop.swim_mall.qna.repository.QnaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QnaDraftAssistService {

    private final QnaRepository qnaRepository;
    private final QnaMessageRepository qnaMessageRepository;
    private final FaqRepository faqRepository;
    private final OpenAiQnaDraftClient openAiClient;
    private final QnaDraftPromptService promptService;
    private final QnaDraftLogWriter logWriter;
    private final AiDailyLimitService aiDailyLimitService;
    private final ObjectMapper objectMapper;

    @Value("${ai.qna-draft.enabled:true}")
    private boolean enabled;

    @Value("${ai.qna-draft.max-faq-context:5}")
    private int maxFaqContext;

    public QnaDraftAssistResponseDto generateDraft(Long qnaNo, String callerRole, Long adminId, Long partnerId) {
        try {
            if ("ADMIN".equals(callerRole)) {
                aiDailyLimitService.assertQnaDraftAdminAllowed(adminId);
            } else if ("PARTNER".equals(callerRole)) {
                aiDailyLimitService.assertQnaDraftPartnerAllowed(partnerId);
            }

            if (!enabled || !openAiClient.isAvailable()) {
                return buildFallback(qnaNo);
            }

            DraftContext ctx = loadDraftContext(qnaNo);
            long start = System.currentTimeMillis();
            try {
                String rawJson = openAiClient.requestDraftJson(ctx.systemPrompt(), ctx.userPrompt());
                long latency = System.currentTimeMillis() - start;

                QnaDraftAssistResponseDto result = parseResponse(rawJson, ctx.faqs());
                logWriter.save(qnaNo, callerRole, adminId, partnerId, ctx.userPrompt(), rawJson, true, null, latency, openAiClient.getModel());
                return result;

            } catch (Exception e) {
                long latency = System.currentTimeMillis() - start;
                log.error("QnA Draft AI 호출 실패 (qnaNo={}): {}", qnaNo, e.getMessage(), e);
                logWriter.save(qnaNo, callerRole, adminId, partnerId, ctx.userPrompt(), null, false, e.getMessage(), latency, openAiClient.getModel());
                return buildFallback(qnaNo);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("QnA Draft 초안 생성 중 예외 (qnaNo={}): {}", qnaNo, e.getMessage(), e);
            return buildFallback(qnaNo);
        }
    }

    @Transactional(readOnly = true)
    DraftContext loadDraftContext(Long qnaNo) {
        QnaEntity qna = qnaRepository.findByIdForDraftAssist(qnaNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.QNA_NOT_FOUND));

        List<QnaMessageEntity> messages = qnaMessageRepository.findByQnaOrderByCreatedAtAsc(qna);
        QnaMessageEntity customerMessage = messages.stream()
                .filter(m -> m.getAuthorType() == QnaAuthorType.CUSTOMER)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.QNA_NOT_FOUND, "고객 메시지를 찾을 수 없습니다"));

        String categoryCode = qna.getCategory().name();
        List<FaqEntity> faqs = faqRepository.findByFaqCategoryOrderByFaqNoDesc(categoryCode);
        if (faqs.size() > maxFaqContext) {
            faqs = faqs.subList(0, maxFaqContext);
        }

        String systemPrompt = promptService.buildSystemPrompt(faqs);
        String userPrompt = promptService.buildUserPrompt(qna, customerMessage);
        return new DraftContext(systemPrompt, userPrompt, faqs);
    }

    @Transactional(readOnly = true)
    QnaDraftAssistResponseDto buildFallback(Long qnaNo) {
        QnaEntity qna = qnaRepository.findByIdForDraftAssist(qnaNo).orElse(null);
        List<QnaDraftAssistResponseDto.FaqReference> refs = new ArrayList<>();

        if (qna != null) {
            String categoryCode = qna.getCategory().name();
            List<FaqEntity> faqs = faqRepository.findByFaqCategoryOrderByFaqNoDesc(categoryCode);
            int limit = Math.min(3, faqs.size());
            for (int i = 0; i < limit; i++) {
                FaqEntity faq = faqs.get(i);
                refs.add(QnaDraftAssistResponseDto.FaqReference.builder()
                        .faqNo(faq.getFaqNo())
                        .question(faq.getFaqQuestion())
                        .build());
            }
        }

        return QnaDraftAssistResponseDto.builder()
                .draftBody("")
                .referencedFaqs(refs)
                .confidence("FALLBACK")
                .build();
    }

    private QnaDraftAssistResponseDto parseResponse(String rawJson, List<FaqEntity> faqs) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            String draftBody = root.path("draftBody").asText("");
            String confidence = root.path("confidence").asText("MEDIUM");

            List<QnaDraftAssistResponseDto.FaqReference> refs = new ArrayList<>();
            JsonNode faqNosNode = root.path("referencedFaqNos");
            if (faqNosNode.isArray()) {
                for (JsonNode n : faqNosNode) {
                    long faqNo = n.asLong();
                    String question = faqs.stream()
                            .filter(f -> f.getFaqNo().equals(faqNo))
                            .map(FaqEntity::getFaqQuestion)
                            .findFirst()
                            .orElse(null);
                    if (question != null) {
                        refs.add(QnaDraftAssistResponseDto.FaqReference.builder()
                                .faqNo(faqNo)
                                .question(question)
                                .build());
                    }
                }
            }

            return QnaDraftAssistResponseDto.builder()
                    .draftBody(draftBody)
                    .referencedFaqs(refs)
                    .confidence(confidence)
                    .build();

        } catch (Exception e) {
            log.warn("QnA Draft AI 응답 파싱 실패: {}", e.getMessage());
            return QnaDraftAssistResponseDto.builder()
                    .draftBody(rawJson)
                    .referencedFaqs(Collections.emptyList())
                    .confidence("LOW")
                    .build();
        }
    }

    private record DraftContext(String systemPrompt, String userPrompt, List<FaqEntity> faqs) {
    }
}
