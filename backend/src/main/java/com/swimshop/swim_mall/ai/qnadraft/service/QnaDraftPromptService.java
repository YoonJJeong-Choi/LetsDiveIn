package com.swimshop.swim_mall.ai.qnadraft.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.delivery.entity.DeliveryEntity;
import com.swimshop.swim_mall.faq.entity.FaqEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.qna.entity.QnaEntity;
import com.swimshop.swim_mall.qna.entity.QnaMessageEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;

@Service
public class QnaDraftPromptService {

    public String buildSystemPrompt(List<FaqEntity> faqs) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                당신은 수영용품 쇼핑몰 '렛츠다이브인'의 고객 문의 답변 도우미입니다.
                고객의 1:1 문의에 대해 정중하고 도움이 되는 답변 초안을 작성해 주세요.

                규칙:
                - 존댓말을 사용하세요.
                - draftBody는 반드시 아래 형식을 따르세요.
                  · 첫 줄: "안녕하세요, Let's Dive In 고객센터입니다."
                  · 본문: 문의에 대한 답변 (2~4문단, 문단당 1~3문장)
                  · 마지막 줄: "추가 문의 사항이 있으시면 언제든지 말씀해 주세요. 감사합니다."
                - 답변은 간결하게 작성하고, 같은 내용을 반복하지 마세요.
                - 구체적이고 실용적인 답변을 작성하세요.
                - 확인되지 않은 정보는 추측하지 마세요. 확인이 필요하면 "확인 후 안내드리겠습니다"라고 작성하세요.
                - 주문/배송 정보가 제공되면 참고하여 답변하세요.
                - 아래 FAQ를 참고하되, 실제로 참고한 FAQ의 faqNo만 referencedFaqNos에 넣으세요. 목록에 없는 번호는 넣지 마세요.

                반드시 다음 JSON 형식으로만 응답하세요:
                {
                  "draftBody": "답변 초안 텍스트",
                  "referencedFaqNos": [1, 3],
                  "confidence": "HIGH 또는 MEDIUM 또는 LOW"
                }
                - confidence: 주어진 맥락으로 정확한 답변이 가능하면 HIGH, 추가 확인이 필요하면 MEDIUM, 정보가 부족하면 LOW
                """);

        if (!faqs.isEmpty()) {
            sb.append("\n--- 참고 FAQ ---\n");
            for (FaqEntity faq : faqs) {
                sb.append(String.format("[faqNo=%d] Q: %s\nA: %s\n\n", faq.getFaqNo(), faq.getFaqQuestion(), faq.getFaqAnswer()));
            }
        }

        return sb.toString();
    }

    public String buildUserPrompt(QnaEntity qna, QnaMessageEntity customerMessage) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 문의 정보\n");
        sb.append("- 카테고리: ").append(qna.getCategory().getLabel()).append("\n");
        if (qna.getScope() != null) {
            sb.append("- 세부 유형: ").append(qna.getScope().getLabel()).append("\n");
        }
        sb.append("- 제목: ").append(qna.getTitle()).append("\n");
        sb.append("- 고객명: ").append(qna.getCustomer().getCustomerName()).append("\n");
        sb.append("\n## 문의 내용\n").append(customerMessage.getBody()).append("\n");

        OrderItemEntity orderItem = qna.getOrderItem();
        if (orderItem != null) {
            sb.append("\n## 주문/상품 정보\n");
            sb.append("- 상품명: ").append(orderItem.getProduct().getProductName()).append("\n");
            sb.append("- 수량: ").append(orderItem.getItemQuantity()).append("\n");
            sb.append("- 금액: ").append(String.format("%,d원", orderItem.getItemTotalPrice())).append("\n");

            if (orderItem.getOption() != null) {
                String optionDesc = orderItem.getOption().getColor() + " / " + orderItem.getOption().getSize();
                sb.append("- 옵션: ").append(optionDesc).append("\n");
            }

            sb.append("- 주문 상태: ").append(orderItem.getStatusLabel()).append("\n");

            DeliveryEntity delivery = orderItem.getDelivery();
            if (delivery != null) {
                sb.append("- 배송 상태: ").append(delivery.getDeliveryStatus().getLabel()).append("\n");
                if (delivery.getDeliveryCourier() != null) {
                    sb.append("- 택배사: ").append(delivery.getDeliveryCourier()).append("\n");
                }
                if (delivery.getDeliveryTrackingNumber() != null) {
                    sb.append("- 송장번호: ").append(delivery.getDeliveryTrackingNumber()).append("\n");
                }
            }

            ReturnEntity ret = orderItem.getReturnEntity();
            if (ret != null) {
                sb.append("- 반품 상태: ").append(ret.getReturnStatus().getLabel()).append("\n");
                if (ret.getReturnReason() != null) {
                    sb.append("- 반품 사유: ").append(ret.getReturnReason()).append("\n");
                }
            }
        }

        return sb.toString();
    }
}
