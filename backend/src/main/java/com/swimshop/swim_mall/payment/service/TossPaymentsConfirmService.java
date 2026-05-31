package com.swimshop.swim_mall.payment.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.cart.service.CartService;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.repository.OrderRepository;
import com.swimshop.swim_mall.payment.PaymentEntity;
import com.swimshop.swim_mall.payment.PaymentRepository;
import com.swimshop.swim_mall.payment.enums.PaymentStatus;

import lombok.RequiredArgsConstructor;

/**
 * 토스 결제위젯 4·5단계: 서버에서 {@code POST /v1/payments/confirm} 호출 후 DB 반영.
 */
@Service
@RequiredArgsConstructor
public class TossPaymentsConfirmService {

	private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

	private final RestTemplate restTemplate;
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentService paymentService;
	private final CartService cartService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${payments.toss.secret-key:}")
	private String tossSecretKey;

	/** 비어 있으면 헤더 미전송. 로컬에서 실패 재현 시에만 설정 권장. */
	@Value("${payments.toss.test-code:}")
	private String tossTestCode;

	/**
	 * @return 응답 바디 맵 (approved, orderNo, …)
	 */
	@Transactional
	public Map<String, Object> confirmAndPersist(Long orderNo, String paymentKey, String orderIdStr, Long amount) {
		Map<String, Object> body = new HashMap<>();

		if (!StringUtils.hasText(tossSecretKey)) {
			body.put("approved", false);
			body.put("message", "payments.toss.secret-key 가 설정되지 않았습니다.");
			return body;
		}
		if (!StringUtils.hasText(paymentKey)) {
			body.put("approved", false);
			body.put("message", "paymentKey 가 필요합니다.");
			return body;
		}

		OrderEntity order = orderRepository.findByOrderNoWithRelations(orderNo)
				.orElse(null);
		if (order == null) {
			body.put("approved", false);
			body.put("message", "주문을 찾을 수 없습니다.");
			return body;
		}

		if (!java.util.Objects.equals(order.getOrderTotalPrice(), amount)) {
			body.put("approved", false);
			body.put("message", "결제 금액이 주문 금액과 일치하지 않습니다.");
			return body;
		}

		PaymentEntity existing = order.getPayment();
		if (existing != null && existing.getPaidAt() != null) {
			body.put("approved", true);
			body.put("orderNo", orderNo);
			body.put("idempotent", true);
			return body;
		}

		if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
			body.put("approved", false);
			body.put("message", "결제 대기 상태의 주문만 승인할 수 있습니다. 현재: " + order.getOrderStatus().getLabel());
			return body;
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(tossSecretKey, "");
		if (StringUtils.hasText(tossTestCode)) {
			headers.add("TossPayments-Test-Code", tossTestCode.trim());
		}

		Map<String, Object> tossReq = new HashMap<>();
		tossReq.put("paymentKey", paymentKey);
		tossReq.put("orderId", orderIdStr);
		tossReq.put("amount", amount);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(tossReq, headers);

		try {
			ResponseEntity<String> res = restTemplate.postForEntity(TOSS_CONFIRM_URL, entity, String.class);
			String raw = res.getBody();
			JsonNode root = objectMapper.readTree(raw != null ? raw : "{}");

			LocalDateTime paidAt = LocalDateTime.now();
			JsonNode approvedAt = root.get("approvedAt");
			if (approvedAt != null && !approvedAt.isNull() && approvedAt.isTextual()) {
				try {
					paidAt = OffsetDateTime.parse(approvedAt.asText()).toLocalDateTime();
				} catch (Exception ignored) {
					// keep now
				}
			}

			String method = root.path("method").asText(order.getPaymentMethod());
			String receiptUrl = root.path("receipt").path("url").asText(null);

			if (existing != null) {
				existing.setPaymentKey(paymentKey);
				existing.setPgOrderId(orderIdStr);
				existing.setProvider("toss");
				existing.setStatus(PaymentStatus.PAID);
				existing.setRawApprovePayload(raw);
				if (StringUtils.hasText(receiptUrl)) {
					existing.setReceiptUrl(receiptUrl);
				}
				existing.markPaid(amount, method, paidAt);
				if (existing.getOrder() == null) {
					existing.setOrder(order);
				}
				paymentRepository.save(existing);
			} else {
				PaymentEntity payment = PaymentEntity.builder()
						.paymentAmount(amount)
						.paymentMethod(method)
						.provider("toss")
						.paymentKey(paymentKey)
						.pgOrderId(orderIdStr)
						.status(PaymentStatus.PAID)
						.rawApprovePayload(raw)
						.build();
				if (StringUtils.hasText(receiptUrl)) {
					payment.setReceiptUrl(receiptUrl);
				}
				payment.markPaid(amount, method, paidAt);
				payment.setOrder(order);
				payment = paymentRepository.save(payment);
				order.setPayment(payment);
			}

			order.updateStatus(OrderStatus.PAID);
			orderRepository.save(order);

			cartService.removeOrderedLinesFromCart(order);

			body.put("approved", true);
			body.put("orderNo", orderNo);
			return body;
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			String errBody = e.getResponseBodyAsString();
			String code = "";
			String message = e.getMessage();
			try {
				JsonNode err = objectMapper.readTree(errBody != null ? errBody : "{}");
				code = err.path("code").asText("");
				message = err.path("message").asText(message);
			} catch (Exception ignored) {
				if (StringUtils.hasText(errBody)) {
					message = errBody;
				}
			}
			String reason = StringUtils.hasText(code) ? code + ": " + message : message;
			paymentService.recordPaymentFailureInternal(order, reason);

			body.put("approved", false);
			body.put("orderNo", orderNo);
			body.put("code", code);
			body.put("message", message);
			return body;
		} catch (Exception ex) {
			ex.printStackTrace();
			body.put("approved", false);
			body.put("message", "토스 승인 처리 중 오류: " + ex.getMessage());
			return body;
		}
	}
}
