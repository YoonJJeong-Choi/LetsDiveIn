# 배송 (Delivery) 도메인

주문 상품(`OrderItem`) 단위 배송이다.

---

## 1. 상태 의미

배송 필드 `delivery_status`(`DeliveryStatus`, `DeliveryEntity`): READY → SHIPPED → DELIVERED. 송장·택배사 수정은 DELIVERED 이전만. 구매 확정(수동·자동)은 배송 전이와 별개다.

| 값 | 의미 |
|----|------|
| READY | 발주 확인 후 배송 준비 |
| SHIPPED | 배송 중 |
| DELIVERED | 배송 완료 |

배송 행은 발주 확인 뒤 생기므로 `PAID`인데 배송이 없는 것은 흔하다. `OrderStatus`·화면용 `OrderItemStatus`까지 포함한 상태 전체는 위 `status-model.md`를 본다.

---

## 2. 데이터 모델

- `Delivery`는 `OrderItem`과 1:1이다. 테이블 `delivery.order_item_no`가 유니크다.
- 레코드는 결제 직후가 아니라, 해당 `OrderItem`에 대해 발주 확인이 이루어질 때 생성된다 (`OrderService.confirmOrderItem`, 레거시 `confirmOrder` 등에서 `Delivery` 생성, 초기 `delivery_status`는 READY). 발주 확인 API: `POST /api/orders/order-items/{orderItemNo}/confirm` (권장), `POST /api/orders/{orderNo}/confirm`(deprecated).
- 따라서 주문이 `PAID`이어도 아직 한 줄도 발주 확인하지 않았다면 배송 행이 없을 수 있다.

---

## 3. REST API

베이스 경로: `/api/deliveries` (`DeliveryController`)

| 메서드 | 경로 | 역할 | 허용 역할 (서비스 기준) |
|--------|------|------|-------------------------|
| GET | /api/deliveries | 전체 배송 목록 | ADMIN, PARTNER(본인 상품만) |
| GET | /api/deliveries/{deliveryNo} | 배송 단건 | CUSTOMER(본인 주문), PARTNER(본인 상품), ADMIN |
| PATCH | /api/deliveries/{deliveryNo}/start | 배송 시작 (READY→SHIPPED) | ADMIN, PARTNER(본인 상품, 활성 파트너) |
| PATCH | /api/deliveries/{deliveryNo}/complete | 배송 완료 (SHIPPED→DELIVERED) | ADMIN, PARTNER(동일) |
| PATCH | /api/deliveries/{deliveryNo} | 송장·택배사 수정 | ADMIN, PARTNER(동일). 본문: `DeliveryUpdateRequestDto` (trackingNumber, courier nullable) |

배송 시작 본문 (`DeliveryStartRequestDto`): `trackingNumber`, `courier` 모두 필수(NotBlank).

주문·아이템 기준 조회는 다른 컨트롤러다.

| 메서드 | 경로 | 역할 | 비고 |
|--------|------|------|------|
| GET | /api/orders/{orderNo}/deliveries | 해당 주문의 배송 목록 | `OrderController` → `DeliveryService.getDeliveriesByOrderNo`. CUSTOMER·PARTNER·ADMIN, 파트너는 본인 상품 라인만 필터 |
| GET | /api/order-items/{orderItemNo}/delivery | 한 주문 상품의 배송 단건 | `OrderItemController` → `DeliveryService.getDeliveryByOrderItemNo`. 동일 권한 패턴 |

---

## 4. 조회 API가 나뉜 이유

입력 식별자가 다르다.

- `GET /api/orders/{orderNo}/deliveries` — 주문 번호만 알 때, 그 주문에 묶인 모든 배송을 한 번에.
- `GET /api/order-items/{orderItemNo}/delivery` — 주문 상품 번호만 알 때, 그 줄의 배송 한 건(1:1).
- `GET /api/deliveries/{deliveryNo}` — 배송 PK만 알 때. 목록 화면·CS·다른 API 응답에 `deliveryNo`가 붙은 뒤 이어지는 흐름에 맞다.

권한: 고객은 본인 주문의 배송만, 파트너는 자사 상품이 들어간 배송만, 관리자는 전부 (`DeliveryService` 주석과 동일).

---

## 5. 빈 목록·404가 나올 때

`GET /api/orders/{orderNo}/deliveries`가 빈 배열인 경우:

- 해당 `orderNo`에 대한 `delivery` 행이 없음. 흔한 정상 케이스: `PENDING_PAYMENT`, 또는 `PAID`이지만 아직 어떤 `OrderItem`도 발주 확인되지 않음(배송은 발주 확인 시 생성).
- 파트너로 호출했는데, 그 주문의 배송이 모두 다른 파트너 상품이면 필터 결과가 빈 배열일 수 있음.
- 존재하지 않는 주문 번호로 조회하면 쿼리 결과가 없어 빈 배열일 수 있음(별도 주문 존재 검증 없이 배송만 조회하는 구조).

비정상 의심: 모든 라인이 발주 확인되어 `Delivery`가 있어야 하는데도 계속 빈 배열이면 데이터·플로우 점검.

`GET /api/order-items/{orderItemNo}/delivery`: 해당 아이템에 배송이 없으면 `DELIVERY_NOT_FOUND` 예외.

---

## 6. 수동 처리와 자동 처리

- 배송 상태 READY→SHIPPED→DELIVERED는 현재 코드 기준으로 관리자·파트너가 REST로 수동 갱신하는 흐름이 본류다. 택배사 연동·웹훅 자동 반영은 없다.
- 매일 새벽 2시 스케줄러(`OrderAutoCompleteScheduler`)는 배송이 아니라 주문 상품의 자동 구매 확정(`OrderItem.autoCompleteOrderItem`)이다. 배송 자동 전이와 혼동하지 않는다.

테스트·운영에서는 발주 확인 후 `PATCH .../start`, 이어서 `PATCH .../complete` 순서를 맞추는 것이 실제와 같다.

---

## 7. 주문과 배송의 연계 (요약)

- `OrderStatus`는 `status-model.md`를 따른다.
- 주문 전체를 `CANCELLED`로 바꿀 때 `OrderEntity.changeStatus`는 먼저 발주 확인된 `OrderItem`이 하나라도 있으면 취소를 막는다(`confirmedAt` 검사). 발주 확인 시점에 `Delivery`가 생기므로, 배송이 아직 `READY`여도 이미 취소 불가인 경우가 대부분이다. 그다음 단계로 배송이 SHIPPED 또는 DELIVERED이면 취소 불가(추가 안전장치)다.
- 구매 확정은 주문 상태가 COMPLETED로 바뀌는 것이 아니라 `OrderItem.completedAt`으로만 표현된다.
- 배송이 생기는 시점은 발주 확인이다. 결제 승인(`PAID`)만으로는 `Delivery`가 생기지 않는다.

---

## 8. 구현 위치 참고

- 컨트롤러: `delivery/controller/DeliveryController.java`, `order/controller/OrderController.java` (deliveries), `order/controller/OrderItemController.java` (delivery)
- 서비스: `delivery/service/DeliveryService.java`
- 엔티티: `delivery/entity/DeliveryEntity.java`, `order/entity/OrderItemEntity.java`
- 레포지토리: `delivery/repository/DeliveryRepository.java`

---

## 9. Postman·수동 호출 검증

요청 본문·세션 쿠키·응답 예시는 [testing/POSTMAN_LEGACY_REFERENCE.md](../../testing/POSTMAN_LEGACY_REFERENCE.md)의 `## 11. 배송 관리 API (Admin/Partner용)` 절을 본다(같은 파일에 다른 `## 11` 제목이 있어 번호만으로는 구분되지 않음). 엔드포인트 URL만은 [testing/POSTMAN_CURRENT_REFERENCE.md](../../testing/POSTMAN_CURRENT_REFERENCE.md)와 함께 본다.
