# 상태 모델

---

## 1. 범위

이 문서는 주문·배송·반품·결제·정산·파트너·관리자·상품·이벤트·세일 등에서 쓰이는 상태 값을 한곳에 모은다.  
소스는 주로 `com.swimshop.swim_mall.common.enums`, `com.swimshop.swim_mall.payment.enums`, 그리고 `OrderEntity`, `DeliveryEntity`, `ReturnService`, `TossPaymentsConfirmService`, `OrderController`, `PaymentController` 등이다.

다음은 이 문서에서 다루지 않는다. 역할이 “상태 전이”보다 분류·로그·타겟 타입에 가깝기 때문이다.

- `AccountRole`, `AuthPortal`, `ProductType`, `ProductSubType`, `EventType`, `EventMode`, `DiscountType`, `PointType`, `PointEventTargetType`, `SaleScope`, `ReturnReasonType`, `CustomerHistoryActionType`, `PartnerHistoryActionType`, `SettlementHistoryAction`, `SalePolicyHistoryAction`, `CustomerActivityType`, `CustomerGradeEnum` 등

---

## 2. 주문 상태 (OrderStatus)

소스: `common/enums/OrderStatus.java`  
엔티티 전이 검증: `OrderEntity.changeStatus`, `validateStatusTransition`  
`updateStatus`는 검증 없이 결제 연동·내부 동기화용으로 쓰인다.

구매 확정은 `OrderStatus`가 아니라 `OrderItem.completedAt`으로만 본다.

### 2.1 값

| 코드 | 의미 |
|------|------|
| PENDING_PAYMENT | 주문 생성 직후, 결제 대기 |
| PAID | 결제 완료, 발주 확인 대기 |
| PAYMENT_FAILED | 결제 실패 기록 후 |
| ACTIVE | 모든 주문 상품 발주 확인 후, 주문 진행 중 |
| CANCELLED | 주문 전체 취소 |

### 2.2 changeStatus 기준 허용 전이

| 현재 | 다음으로 허용 |
|------|----------------|
| PENDING_PAYMENT | PAID, PAYMENT_FAILED, CANCELLED |
| PAID | ACTIVE, CANCELLED |
| ACTIVE | CANCELLED만 |
| PAYMENT_FAILED | PENDING_PAYMENT만 |
| CANCELLED | 없음 |

CANCELLED로 바꿀 때 추가로: 발주 확인된 `OrderItem`이 있으면 불가. 배송이 SHIPPED 또는 DELIVERED인 라인이 있으면 불가.

### 2.3 서비스에서 실제로 바뀌는 경로

| 전이 | 트리거 |
|------|--------|
| PENDING_PAYMENT → PAID | `TossPaymentsConfirmService.confirmAndPersist` 성공 시 `order.updateStatus(PAID)`. 요청: `POST /api/payments/confirm`. 주문은 PENDING_PAYMENT일 때만 승인 처리된다. |
| PENDING_PAYMENT → PAYMENT_FAILED | 토스 승인 HTTP 오류 시 `PaymentService.recordPaymentFailureInternal` 등 |
| PAID → ACTIVE | 모든 `OrderItem`에 `confirmedAt`이 채워지면 `OrderService.checkAndUpdateOrderStatus`에서 `order.updateStatus(ACTIVE)`. 주로 `POST /api/orders/order-items/{orderItemNo}/confirm` 누적 |
| 임의 → CANCELLED (고객) | `PATCH /api/orders/{orderNo}/cancel`, `OrderService.cancelOrder`. 한 줄이라도 발주 확인되었거나 구매 확정(`completedAt`)이 있으면 불가. 이미 CANCELLED면 에러 |

PAYMENT_FAILED 이후 같은 주문으로 재결제: 엔티티 규칙상 PAYMENT_FAILED → PENDING_PAYMENT는 허용되나, `PaymentService.approvePayment`는 PAYMENT_FAILED를 거절하고, `POST /api/payments/confirm`도 PENDING_PAYMENT만 받는다. 운영 문서에서는 재결제 플로우를 별도 설계로 두는 편이 안전하다.

### 2.4 PATCH /api/orders/{orderNo}/status

`OrderService.updateOrderStatus`는 항상 FORBIDDEN으로 거절한다. 주문 상태는 결제·발주 확인·배송·취소·반품 API로만 맞춘다.

### 2.5 발주 확인 API

| API | 설명 |
|-----|------|
| POST /api/orders/order-items/{orderItemNo}/confirm | 권장. 라인 단위 발주 확인, 해당 라인에 Delivery(READY) 생성, 전원 확인 시 주문 ACTIVE |
| POST /api/orders/{orderNo}/confirm | Deprecated. 주문 전체 발주 확인(레거시) |

### 2.6 구매 확정

- 필드: `OrderItem.completedAt` (주문 레벨 COMPLETED 상태는 없다)
- API: `POST /api/orders/order-items/{orderItemNo}/complete`
- 조건: `OrderItemEntity.completeOrderItem` — Delivery 존재, DELIVERED, 반품이 REQUESTED/APPROVED/PICKUP_COMPLETED/REFUNDED면 불가, 이미 completedAt이면 불가
- 자동 구매 확정: 배송 완료 후 7일, `OrderItemEntity.autoCompleteOrderItem` (스케줄러)
- 포인트 적립·주문 시 사용·내역 API는 `../../domains/points/points.md`를 본다.

---

## 3. 주문 상품 표시 상태 (OrderItemStatus)

소스: `common/enums/OrderItemStatus.java`  
DB 컬럼이 아니라 `OrderItemEntity.getStatus()`에서 `confirmedAt`, `completedAt`, `delivery`, `returnEntity`, `isCancelled` 등을 조합해 계산한다. API 응답·화면 요약용이다.

| 코드 | 의미 |
|------|------|
| PENDING_CONFIRMATION | 발주 확인 전 |
| CONFIRMED | 발주 확인됨 |
| READY | 배송 준비 |
| SHIPPED | 배송 중 |
| DELIVERED | 배송 완료 |
| COMPLETED | 구매 확정 |
| RETURN_IN_PROGRESS | 반품 진행 중 |
| RETURN_REJECTED | 반품 거절 |
| REFUNDED | 환불 완료 |
| CANCELLED | 주문 상품 취소 |

---

## 4. 배송 상태 (DeliveryStatus)

배송 REST·생성 시점·조회 API 분기·빈 목록 등은 `../../domains/delivery/delivery.md`를 본다.

소스: `common/enums/DeliveryStatus.java`  
엔티티: `DeliveryEntity`

### 4.1 값과 의미

| 코드 | 의미 |
|------|------|
| READY | 발주 확인 후 배송 준비 |
| SHIPPED | 배송 중 |
| DELIVERED | 배송 완료 |

### 4.2 DeliveryEntity 전이

- READY → SHIPPED: `startDelivery` (READY만 허용). 송장·택배사 설정
- SHIPPED → DELIVERED: `completeDelivery` (SHIPPED만 허용)
- 배송 완료 후: `updateTrackingInfo`는 불가 (DELIVERED면 예외)

### 4.3 API

- PATCH /api/deliveries/{deliveryNo}/start
- PATCH /api/deliveries/{deliveryNo}/complete
- PATCH /api/deliveries/{deliveryNo} (송장·택배사 수정, DELIVERED 제외)
- GET /api/orders/{orderNo}/deliveries
- GET /api/order-items/{orderItemNo}/delivery

---

## 5. 반품 상태 (ReturnStatus)

소스: `common/enums/ReturnStatus.java`  
전이 검증: `ReturnService.validateStatusTransition`

### 5.1 값

| 코드 | 의미 |
|------|------|
| REQUESTED | 신청 |
| APPROVED | 승인 |
| REJECTED | 거절 |
| PICKUP_COMPLETED | 수거 완료 |
| REFUNDED | 환불 완료 |

### 5.2 허용 전이 (validateStatusTransition)

| 현재 | 다음 |
|------|------|
| REQUESTED | APPROVED, REJECTED만 |
| APPROVED | PICKUP_COMPLETED만 |
| PICKUP_COMPLETED | REFUNDED, REJECTED만 |
| REJECTED | 없음 |
| REFUNDED | 없음 |

거절 API(`rejectReturn`): REQUESTED 또는 PICKUP_COMPLETED만. APPROVED에서는 거절하지 않는다.

### 5.3 API

- POST /api/returns
- POST /api/returns/{returnNo}/approve
- POST /api/returns/{returnNo}/reject
- PATCH /api/returns/{returnNo}
- GET /api/returns/{returnNo}/history

구매 확정과 겹치면: 반품이 진행 중이거나 REFUNDED이면 `completeOrderItem`에서 구매 확정 불가 (코드 주석·검증 참고).

### 5.4 관리자 반품 검토 보조

`GET /api/returns/{returnNo}/return-assist` — 규칙 기반(증빙·정책·고객 반품 이력 참고). OpenAI·사기 점수는 사용하지 않는다. `ReturnStatus` 전이와는 별개다.

---

## 6. 결제 레코드 상태 (PaymentStatus)

REST·토스 승인·웹훅·조회는 `../../domains/payment/payment.md`를 본다.

소스: `payment/enums/PaymentStatus.java`  
`PaymentEntity.status`에 저장. `OrderStatus`와 혼동하지 않는다.

| 코드 | 의미 |
|------|------|
| READY | 준비 |
| PAID | 승인 완료 |
| CANCELED | 취소 |
| FAILED | 실패 |

토스 승인 성공 시 PAID로 맞추는 처리는 `TossPaymentsConfirmService` 등을 본다.

---

## 7. 정산 상태 (SettlementStatus)

소스: `common/enums/SettlementStatus.java`

| 코드 | 의미 |
|------|------|
| PENDING | 대기 |
| PROCESSING | enum에만 남아 있는 Deprecated 값 |
| COMPLETED | 완료 |
| CANCELLED | 취소 |

신규 설명은 PENDING, COMPLETED, CANCELLED 중심이 안전하다.

---

## 8. 파트너 상태 (PartnerStatus)

소스: `common/enums/PartnerStatus.java`

| 코드 | 의미 |
|------|------|
| PENDING | 입점 신청 대기 |
| APPROVED | 승인·운영 중 |
| REJECTED | 거절 |
| INACTIVE | 승인됐으나 비활성 |

---

## 9. 관리자 운영 상태 (AdminStatus)

소스: `common/enums/AdminStatus.java`  
필드: `AdminEntity.adminStatus`. 계정 잠금과는 별개로 볼 수 있으며, `AccountEntity.status` 문자열과 역할이 다를 수 있다.

| 코드 | 의미 |
|------|------|
| ACTIVE | 활성 |
| INACTIVE | 비활성 |

---

## 10. 상품·옵션 심사 상태 (ActiveStatus)

소스: `common/enums/ActiveStatus.java`  
`ProductEntity.productActiveStatus`, 옵션 상태 등에 사용.

| 코드 | 의미 |
|------|------|
| PENDING | 승인 대기 |
| ACTIVE | 활성 |
| REJECTED | 거절 |
| INACTIVE | 비활성 |
| PENDING_UPDATE | 수정 재심사 대기 |

---

## 11. 이벤트 공개 상태 (EventStatus)

소스: `common/enums/EventStatus.java`

| 코드 | 의미 |
|------|------|
| PRIVATE | 비공개 |
| PUBLISHED | 공개 |
| ENDED | 종료 |

---

## 12. 세일 정책 상태 (SaleStatus)

소스: `common/enums/SaleStatus.java`

| 코드 | 의미 |
|------|------|
| PENDING_APPROVAL | 승인 대기 |
| ACTIVE | 활성 |
| INACTIVE | 비활성 |
| REJECTED | 거절 |
| EXPIRED | 종료 |
| CANCELLED | 취소 |

---

## 13. 파트너 변경 요청 상태 (PartnerChangeRequestStatus)

소스: `common/enums/PartnerChangeRequestStatus.java`

| 코드 | 의미 |
|------|------|
| PENDING | 대기 |
| APPROVED | 승인 |
| REJECTED | 거절 |

---

## 14. 계정 문자열 상태 (AccountEntity.status)

`AccountEntity`의 `status`는 String이다. enum이 아니다. 주석상 PARTNER/ADMIN의 로그인 가능·잠금 등 계정 레벨에 쓰이고, 파트너·관리자의 “운영 승인 여부”는 각각 `PartnerStatus`, `AdminStatus`를 우선 본다.

---

## 15. 결제 HTTP와 주문 상태 (요약)

| API | 역할 |
|-----|------|
| POST /api/payments/confirm | 토스 승인 반영, 성공 시 주문 PAID 등 |
| POST /api/payments/webhook | 토스 웹훅 |
| POST /api/payments | 결제 준비용 스켈레톤 성격, 운영 기준은 confirm 쪽 |

공개 REST에서 주문을 PAID로 올리는 주된 진입점은 `POST /api/payments/confirm`이다. `PaymentService.approvePayment` / `failPayment`는 서비스에 존재하나 가짜 승인용 HTTP는 노출되지 않는다 — 상세는 `../../domains/payment/payment.md`를 본다.

---

## 16. 상태 관련 API 한눈에

| 동작 | 메서드 | 경로 |
|------|--------|------|
| 주문 생성 | POST | /api/orders |
| 결제 승인(토스) | POST | /api/payments/confirm |
| 주문 취소(고객) | PATCH | /api/orders/{orderNo}/cancel |
| 발주 확인(권장) | POST | /api/orders/order-items/{orderItemNo}/confirm |
| 발주 확인(레거시) | POST | /api/orders/{orderNo}/confirm |
| 구매 확정 | POST | /api/orders/order-items/{orderItemNo}/complete |
| 주문 상태 수동 변경 | PATCH | /api/orders/{orderNo}/status (403) |
| 배송 시작 | PATCH | /api/deliveries/{deliveryNo}/start |
| 배송 완료 | PATCH | /api/deliveries/{deliveryNo}/complete |
| 반품 신청 | POST | /api/returns |
| 반품 승인 | POST | /api/returns/{returnNo}/approve |
| 반품 거절 | POST | /api/returns/{returnNo}/reject |
| 반품 갱신 | PATCH | /api/returns/{returnNo} |

---

## 17. 엔티티 관계 요약

- 하나의 Order에 여러 OrderItem
- 각 OrderItem은 0 또는 1 Delivery, 0 또는 1 Return
- 구매 확정은 항상 OrderItem.completedAt
- 고객 주문 취소는 PATCH /api/orders/{orderNo}/cancel의 검증이 실무 기준
