# 결제 (Payment) 도메인

주문(`Order`)과 결제 레코드(`Payment`)·토스 승인을 한 문서에서 묶어 본다.

---

## 1. 상태 의미

주문 필드 `order_status`(`OrderStatus`, `OrderEntity`): 결제 본류는 `PENDING_PAYMENT` → `PAID` → `ACTIVE`(모든 주문 상품 발주 확인 후).

| 코드 | 의미 |
|------|------|
| PENDING_PAYMENT | 주문 직후, 결제 대기 |
| PAID | 결제 완료, 발주 확인 대기 |
| PAYMENT_FAILED | 결제 실패가 기록된 뒤 |
| ACTIVE | 전 상품 발주 확인 후, 주문 진행 중 |
| CANCELLED | 주문 전체 취소 |

결제 레코드 필드 `status`(`PaymentStatus`, `PaymentEntity`): `READY` → `PAID`(승인 성공). 취소·실패 처리 시 `CANCELED`·`FAILED`로 바뀐다.

| 값 | 의미 |
|----|------|
| READY | 결제 레코드 준비 |
| PAID | PG 승인 완료 |
| CANCELED | 결제 취소 |
| FAILED | 결제 실패 |

주문과 결제 레코드는 1:1이며 FK는 `payment.order_no`이다.

고객 API의 `paymentStatus` 문자열은 DB `Payment.status`가 아니라, 당시 주문 상태를 `PaymentService.getPaymentStatusFromOrder`로 붙인 표시다. (승인·내부 실패 처리 시 레코드 `status`에는 `PAID`·`FAILED` 등이 저장된다.)

| 주문 상태(요지) | 응답 paymentStatus |
|-----------------|---------------------|
| PAID, ACTIVE | APPROVED |
| PAYMENT_FAILED | FAILED |
| PENDING_PAYMENT | PENDING |
| CANCELLED | CANCELED |
| 그 외 | UNKNOWN |

---

## 2. 운영 흐름 요약

- `POST /api/orders` — 주문 생성(결제 대기). 아직 `Payment` 행은 없을 수 있다.
- `POST /api/payments/confirm` — 토스 `POST /v1/payments/confirm` 연동, 성공 시 결제 저장·장바구니 정리(`CartService.removeOrderedLinesFromCart`). 웹훅 `PAYMENT_APPROVED` 처리 시에도 동일 호출이 있을 수 있다.
- `POST /api/orders/order-items/{orderItemNo}/confirm` 등 — 발주 확인(주문 도메인). 전 라인 확인 후 주문이 진행 상태로 바뀌는 부분은 `status-model.md`·배송 문서와 맞닿는다.

가짜 승인 API `POST /api/payments/approve`는 컨트롤러에 노출되어 있지 않다. `PaymentService.approvePayment`는 서비스에만 남아 있으며, 문서·테스트는 `confirm` 기준으로 맞춘다.

---

## 3. REST API (현재 노출)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| POST | /api/payments | 결제 요청 생성 스켈레톤 (`{ "status": "READY" }` 수준). 위젯 연동 전 정리용. |
| POST | /api/payments/confirm | 토스 위젯 성공 후 `paymentKey`, `orderId`, `amount`로 서버 승인·DB 반영. 세션 검증 없음 — `paymentKey`/금액·주문 상태로 방어. |
| POST | /api/payments/webhook | 토스 웹훅. `Toss-Signature` HMAC 검증, `eventId` 멱등 로그(`WebhookLogEntity`). `PAYMENT_APPROVED` / `PAYMENT_CANCELED` 등 분기. |
| GET | /api/orders/{orderNo}/payment | 본인 주문의 결제 요약(고객 세션). 결제 행이 없으면 404. |

`GET /api/payments/{paymentNo}`는 `PaymentService.getPayment`만 있고 컨트롤러 매핑은 없다. 결제 단건 조회는 주문 번호 기준 API를 쓴다.

컨트롤러: `payment/controller/PaymentController.java`  
조회(주문 기준): `order/controller/OrderController.java`의 `GET .../payment`

---

## 4. 설정·보안

- `payments.toss.secret-key`: 없으면 `confirm`은 `approved: false`와 메시지로 끝나고, `webhook`은 401으로 거절된다.
- `payments.toss.test-code`: 로컬에서 토스 테스트 헤더가 필요할 때만 선택 설정.
- 웹훅 서명: `PaymentController`에서 HMAC-SHA256(hex) 비교. 토스 실제 포맷과 다르면 검증 실패할 수 있으므로, 스테이징에서 서명 알고리즘을 반드시 맞출 것.

---

## 5. 승인 실패

- `TossPaymentsConfirmService`에서 토스 HTTP 오류 시 `PaymentService.recordPaymentFailureInternal`로 주문을 `PAYMENT_FAILED`로 두고 실패 사유를 남길 수 있다.
- 공개 `POST /api/payments/{orderNo}/fail` 같은 엔드포인트는 없다. 실패는 위젯·confirm 오류·웹훅 등 실제 연동 경로로만 반영된다.

---

## 6. 구현 위치 참고

- `payment/service/TossPaymentsConfirmService.java` — 토스 승인 HTTP·주문 `PAID`·장바구니 정리
- `payment/service/PaymentService.java` — (내부/잔존) 가짜 승인·실패 기록·주문별 조회
- `payment/controller/PaymentController.java` — create, confirm, webhook
- `cart/service/CartService.java` — 승인 후 주문 라인 장바구니 제거
