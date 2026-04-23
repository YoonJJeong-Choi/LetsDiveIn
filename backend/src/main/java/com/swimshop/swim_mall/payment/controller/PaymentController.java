package com.swimshop.swim_mall.payment.controller;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.common.error.ErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	@Value("${payments.toss.secret-key}")
	private String tossSecretKey;

	private final com.swimshop.swim_mall.order.repository.OrderRepository orderRepository;
	private final com.swimshop.swim_mall.payment.PaymentRepository paymentRepository;
	private final com.swimshop.swim_mall.cart.repository.CartRepository cartRepository;
	private final com.swimshop.swim_mall.cart.repository.CartItemRepository cartItemRepository;
	private final com.swimshop.swim_mall.payment.repository.WebhookLogRepository webhookLogRepository;

	/**
	 * 결제 요청 생성 (스켈레톤)
	 * 프론트가 위젯을 열기 전에 주문번호/금액 등을 백엔드에 알릴 때 사용.
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, Object> payload) {
		System.out.println("[Payments] create request => " + payload);
		// TODO: 주문 존재/금액 검증, 거래 레코드 생성
		Map<String, Object> body = new HashMap<>();
		body.put("status", "READY");
		return ResponseEntity.ok(ApiResponse.success(body));
	}

	/**
	 * 결제 승인(confirm)
	 * 리다이렉트 성공 페이지에서 paymentKey/orderId/amount를 받아 승인 처리.
	 */
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<Map<String, Object>>> confirm(@RequestBody Map<String, Object> payload) {
		System.out.println("[Payments] confirm request => " + payload);
		// 간단 승인 플로우 (샌드박스): orderId로 주문 조회 → 금액 검증 → PAID 전이 → 장바구니 정리
		Map<String, Object> body = new HashMap<>();
		try {
			String orderId = String.valueOf(payload.get("orderId"));
			String paymentKey = payload.get("paymentKey") != null ? String.valueOf(payload.get("paymentKey")) : null;
			Number amt = payload.get("amount") instanceof Number ? (Number) payload.get("amount") : null;
			if (orderId == null || orderId.isBlank() || amt == null) {
				return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_REQUEST, "orderId/amount 누락"));
			}
			Long amount = amt.longValue();

			// orderId 형식: ORD-{orderNo}-{ts}
			Long orderNo = null;
			try {
				String[] parts = orderId.split("-");
				if (parts.length >= 2) {
					orderNo = Long.valueOf(parts[1]);
				}
			} catch (Exception ignore) {}
			if (orderNo == null) {
				return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_REQUEST, "orderId 형식 오류"));
			}

			// 주문 조회
			com.swimshop.swim_mall.order.entity.OrderEntity order =
					orderRepository.findByOrderNo(orderNo).orElse(null);
			if (order == null) {
				return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다."));
			}

			// 금액 검증
			if (!java.util.Objects.equals(order.getOrderTotalPrice(), amount)) {
				return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_REQUEST, "금액 불일치"));
			}

			// 멱등 처리: 이미 결제 정보가 있고 paidAt이 있으면 그대로 성공 반환
			com.swimshop.swim_mall.payment.PaymentEntity existing = order.getPayment();
			if (existing != null && existing.getPaidAt() != null) {
				body.put("approved", true);
				body.put("orderNo", orderNo);
				return ResponseEntity.ok(ApiResponse.success(body));
			}

			// Payment 생성 또는 기존 결제 갱신 후 주문 상태 전이
			if (existing != null) {
				// 기존 결제 레코드를 결제 완료로 갱신 (amount/method/paidAt 업데이트)
				existing.markPaid(amount, order.getPaymentMethod(), java.time.LocalDateTime.now());
				// FK 소유자는 Payment이므로, 링크가 없다면 연결
				if (existing.getOrder() == null) {
					existing.setOrder(order);
				}
				paymentRepository.save(existing);
			} else {
				// 신규 결제 생성
			com.swimshop.swim_mall.payment.PaymentEntity payment = com.swimshop.swim_mall.payment.PaymentEntity.builder()
					.paymentAmount(amount)
					.paymentMethod(order.getPaymentMethod())
					.provider("toss")
					.paymentKey(paymentKey)
					.pgOrderId(orderId)
					.status(com.swimshop.swim_mall.payment.enums.PaymentStatus.PAID)
					.paidAt(java.time.LocalDateTime.now())
					.rawApprovePayload(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload))
					.build();
				// FK 소유자는 Payment
				payment.setOrder(order);
				payment = paymentRepository.save(payment);
			}

			// 주문 상태 전이
			order.updateStatus(com.swimshop.swim_mall.common.enums.OrderStatus.PAID);
			orderRepository.save(order);

			// 장바구니 정리: 고객 장바구니에서 주문 상품과 일치하는 항목 제거
			var cartOpt = cartRepository.findByCustomer(order.getCustomer());
			if (cartOpt.isPresent()) {
				var cart = cartOpt.get();
				var cartItems = cartItemRepository.findByCart(cart);
				// 주문 아이템 기준으로 매칭 삭제
				var orderItems = order.getOrderItems();
				var toDelete = new java.util.ArrayList<com.swimshop.swim_mall.cart.entity.CartItemEntity>();
				for (var ci : cartItems) {
					boolean matches = orderItems.stream().anyMatch(oi -> {
						Long oiProd = oi.getProduct() != null ? oi.getProduct().getProductNo() : null;
						Long oiOpt = oi.getOption() != null ? oi.getOption().getOptionNo() : null;
						Long ciProd = ci.getProduct() != null ? ci.getProduct().getProductNo() : null;
						Long ciOpt = ci.getOption() != null ? ci.getOption().getOptionNo() : null;
						return java.util.Objects.equals(oiProd, ciProd) && java.util.Objects.equals(oiOpt, ciOpt);
					});
					if (matches) toDelete.add(ci);
				}
				if (!toDelete.isEmpty()) {
					cartItemRepository.deleteAll(toDelete);
				}
			}

			body.put("approved", true);
			body.put("orderNo", orderNo);
			return ResponseEntity.ok(ApiResponse.success(body));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, "승인 처리 중 오류가 발생했습니다."));
		}
	}

	/**
	 * 웹훅 수신
	 * 토스 서버가 보내는 비동기 이벤트를 수신하고 HMAC 서명 검증 후 처리.
	 */
	@PostMapping("/webhook")
	public ResponseEntity<?> webhook(
			@RequestHeader(value = "Toss-Signature", required = false) String signature,
			@RequestBody String rawBody) {
		System.out.println("[Payments] webhook rawBody => " + rawBody);
		System.out.println("[Payments] webhook signature => " + signature);

		if (!StringUtils.hasText(tossSecretKey)) {
			System.out.println("[Payments] tossSecretKey missing. Rejecting request.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			if (!verifyHmac(signature, rawBody, tossSecretKey)) {
				System.out.println("[Payments] webhook signature verification FAILED");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
			System.out.println("[Payments] webhook signature verification OK");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		// 멱등 처리: eventId 기준 중복 수신 차단
		try {
			String eventId = extractEventId(rawBody);
			String type = extractType(rawBody);
			if (!StringUtils.hasText(eventId)) {
				return ResponseEntity.badRequest().body("eventId required");
			}

			// 승인/취소 등 이벤트 실전이 (간단 분기, 멱등 우선)
			if ("PAYMENT_APPROVED".equalsIgnoreCase(type)) {
				// payload에서 orderId/paymentKey/amount 추출
				var parsed = parseApproved(rawBody);
				var method = parseMethod(rawBody);
				if (parsed != null && parsed.orderNo != null && parsed.amount != null) {
					Long orderNo = parsed.orderNo;
					Long amount = parsed.amount;
					String paymentKey = parsed.paymentKey;
					// 주문 조회
					var order = orderRepository.findByOrderNo(orderNo).orElse(null);
					if (order != null) {
						// 이미 PAID면 상태 전이/금액 처리는 스킵하지만, 결제수단 등 보강 정보는 갱신 허용
						if (order.getOrderStatus() == com.swimshop.swim_mall.common.enums.OrderStatus.PAID) {
							var existing = order.getPayment();
							if (existing != null && method != null) {
								if (existing.getPaymentMethod() == null
										|| existing.getPaymentMethod().isBlank()
										|| !existing.getPaymentMethod().equalsIgnoreCase(method)) {
									existing.setPaymentMethod(method);
									paymentRepository.save(existing);
								}
								// Order 측도 동기화
								if (method != null && (order.getPaymentMethod() == null
										|| order.getPaymentMethod().isBlank()
										|| !order.getPaymentMethod().equalsIgnoreCase(method))) {
									order.setPaymentMethod(method);
									orderRepository.save(order);
								}
							}
						} else {
							// 결제 레코드 갱신/생성
							var existing = order.getPayment();
							if (existing != null) {
								existing.markPaid(amount, order.getPaymentMethod(), java.time.LocalDateTime.now());
								// 실제 결제수단이 파싱되면 덮어씀
								if (method != null) existing.setPaymentMethod(method);
								if (existing.getPaymentKey() == null) existing.setPaymentKey(paymentKey);
								if (existing.getPgOrderId() == null) existing.setPgOrderId(parsed.pgOrderId);
								existing.setStatus(com.swimshop.swim_mall.payment.enums.PaymentStatus.PAID);
								existing.setRawApprovePayload(rawBody);
								paymentRepository.save(existing);
							} else {
								var payment = com.swimshop.swim_mall.payment.PaymentEntity.builder()
										.paymentAmount(amount)
										.paymentMethod(method != null ? method : order.getPaymentMethod())
										.provider("toss")
										.paymentKey(paymentKey)
										.pgOrderId(parsed.pgOrderId)
										.status(com.swimshop.swim_mall.payment.enums.PaymentStatus.PAID)
										.paidAt(java.time.LocalDateTime.now())
										.rawApprovePayload(rawBody)
										.build();
								payment.setOrder(order);
								paymentRepository.save(payment);
							}
							// Order 결제수단 동기화
							if (method != null && (order.getPaymentMethod() == null
									|| order.getPaymentMethod().isBlank()
									|| !order.getPaymentMethod().equalsIgnoreCase(method))) {
								order.setPaymentMethod(method);
							}
							order.updateStatus(com.swimshop.swim_mall.common.enums.OrderStatus.PAID);
							orderRepository.save(order);
						}
					}
				}
			} else if ("PAYMENT_CANCELED".equalsIgnoreCase(type)) {
				// 간략 처리: 결제 상태만 CANCELED로 갱신(부분취소/사유 등은 확장 지점)
				var parsed = parseApproved(rawBody);
				var method = parseMethod(rawBody);
				if (parsed != null && parsed.orderNo != null) {
					var order = orderRepository.findByOrderNo(parsed.orderNo).orElse(null);
					if (order != null && order.getPayment() != null) {
						order.getPayment().setStatus(com.swimshop.swim_mall.payment.enums.PaymentStatus.CANCELED);
						if (method != null) order.getPayment().setPaymentMethod(method);
						order.getPayment().setRawApprovePayload(rawBody);
						paymentRepository.save(order.getPayment());
						// Order 결제수단도 보유(로깅/호환 목적)
						if (method != null && (order.getPaymentMethod() == null
								|| order.getPaymentMethod().isBlank()
								|| !order.getPaymentMethod().equalsIgnoreCase(method))) {
							order.setPaymentMethod(method);
						}
						// 정책상 전액취소면 주문 취소
						try {
							order.changeStatus(com.swimshop.swim_mall.common.enums.OrderStatus.CANCELLED);
						} catch (Exception ignore) {}
						orderRepository.save(order);
					}
				}
			}

			var existing = webhookLogRepository.findByEventId(eventId).orElse(null);
			if (existing != null) {
				// 이미 처리되었으면 OK 반환(멱등 히트)
				if (Boolean.TRUE.equals(existing.getProcessed())) {
					return ResponseEntity.ok().build();
				}
				// 처리 중/미처리 상태면 이 요청에서는 재처리하지 않고 OK
				return ResponseEntity.ok().build();
			}

			// 신규 로그 INSERT
			var log = com.swimshop.swim_mall.payment.entity.WebhookLogEntity.builder()
					.eventId(eventId)
					.type(type)
					.signature(signature)
					.payload(rawBody)
					.processed(false)
					.build();
			log = webhookLogRepository.save(log);

			log.markProcessed();
			webhookLogRepository.save(log);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private boolean verifyHmac(String signature, String rawBody, String secret) throws Exception {
		if (!StringUtils.hasText(signature)) return false;
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		mac.init(secretKeySpec);
		byte[] hmac = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
		String computed = bytesToHex(hmac);
		// 토스는 보통 base64 또는 hex 포맷을 사용. 예시에서는 단순 비교(환경에 맞게 조정 필요)
		return constantTimeEquals(signature, computed);
	}

	private String extractEventId(String rawBody) {
		try {
			var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			var node = mapper.readTree(rawBody);
			var n = node.get("eventId");
			if (n != null && !n.isNull()) {
				return n.asText();
			}
		} catch (Exception ignore) {}
		return null;
	}

	private String extractType(String rawBody) {
		try {
			var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			var node = mapper.readTree(rawBody);
			var n = node.get("type");
			if (n != null && !n.isNull()) {
				return n.asText();
			}
		} catch (Exception ignore) {}
		return null;
	}

	private static class ApprovedPayload {
		Long orderNo;
		Long amount;
		String paymentKey;
		String pgOrderId;
	}

	private ApprovedPayload parseApproved(String rawBody) {
		try {
			var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			var root = mapper.readTree(rawBody);
			var data = root.get("data");
			if (data == null || data.isNull()) return null;
			ApprovedPayload p = new ApprovedPayload();
			var orderIdNode = data.get("orderId");
			if (orderIdNode != null && !orderIdNode.isNull()) {
				p.pgOrderId = orderIdNode.asText();
				// ORD-{orderNo}-{ts}
				try {
					String s = orderIdNode.asText();
					String[] parts = s.split("-");
					if (parts.length >= 2) p.orderNo = Long.valueOf(parts[1]);
				} catch (Exception ignore) {}
			}
			var pk = data.get("paymentKey");
			if (pk != null && !pk.isNull()) p.paymentKey = pk.asText();
			var amt = data.get("amount");
			if (amt != null && !amt.isNull()) p.amount = amt.asLong();
			return p;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 결제수단 파싱: data.method 우선, 없으면 하위 객체 존재로 추론
	 * 카드/계좌이체/가상계좌/휴대폰/간편결제 등
	 */
	private String parseMethod(String rawBody) {
		try {
			var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			var root = mapper.readTree(rawBody);
			var data = root.get("data");
			if (data == null || data.isNull()) return null;
			var method = data.get("method");
			if (method != null && !method.isNull()) {
				return method.asText(); // ex) card, transfer, virtualAccount, mobilePhone, tossPay, naverPay ...
			}
			// 추론(필요 시 확장)
			if (data.hasNonNull("card")) return "card";
			if (data.hasNonNull("virtualAccount")) return "virtualAccount";
			if (data.hasNonNull("transfer")) return "transfer";
			if (data.hasNonNull("mobilePhone")) return "mobilePhone";
			if (data.hasNonNull("easyPay")) {
				var ep = data.get("easyPay");
				if (ep.hasNonNull("provider")) return ep.get("provider").asText(); // tossPay/naverPay 등
				return "easyPay";
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null) return false;
		if (a.length() != b.length()) return false;
		int result = 0;
		for (int i = 0; i < a.length(); i++) {
			result |= a.charAt(i) ^ b.charAt(i);
		}
		return result == 0;
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}


