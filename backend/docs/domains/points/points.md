# 포인트 (Point) 도메인

고객 잔액·내역(`point_history`)과 주문·구매 확정·이벤트·리뷰가 맞물린다.

---

## 1. 상태 의미 (`PointType`)

내역 `point_type`(`PointType`, `PointHistoryEntity`): `ACCUMULATE`(적립) / `USE`(주문 생성 시 사용액 차감·복구 시 양수 기록 가능) / `MANUAL_ADD`·`MANUAL_DEDUCT` / `EXPIRE`(enum만 존재, 자동 만료 배치는 현재 코드에 없음).

| 값 | 의미 |
|----|------|
| ACCUMULATE | 적립(구매 확정·이벤트·리뷰 등) |
| USE | 주문 생성 시 사용액 차감(`OrderService.createOrder` → `usePoint`, 결제 대기 상태에서 잔액 반영). 내역 금액은 음수. 복구 시 같은 타입으로 양수 한 줄이 추가될 수 있음(`PointService.restorePoint`) |
| MANUAL_ADD | 관리자 수동 지급 |
| MANUAL_DEDUCT | 관리자 수동 차감 |
| EXPIRE | 만료(스케줄 미구현, 필드·enum만 준비) |

적립 내역에는 `expireDate`가 1년 뒤로 채워지나, 만료 처리 로직은 아직 없다.

---

## 2. 데이터 모델

- `CustomerEntity.pointBalance` — 잔액. `PointService`가 `addPoint`·`deductPoint`로 갱신한다.
- `PointHistoryEntity` — `customer_id`, `point_type`, `point_amount`, `point_balance_after`, `order_item_no`, `order_no`, `description`, `expire_date`, `admin_id`(수동 시).
- 주문과의 FK는 내역의 `order_item_no` / `order_no` nullable 컬럼으로만 연결된다(주문 엔티티에 포인트 FK는 없음).

---

## 3. 적립 트리거 (실제 호출)

| 시점 | 호출 경로 | 비고 |
|------|-----------|------|
| 구매 확정(수동) | `OrderService.completeOrderItem` | `completeOrderItem` 후 `customerGradeService.updateCustomerGrade` → 고객 재조회 → 등급의 `pointAccumulationRate`(%)로 `itemTotalPrice` 적립. 그다음 `partnerEventService.handleOrderItemCompleted`, `adminEventRewardService.handleOrderItemCompleted`로 이벤트 포인트 추가 가능 |
| 구매 확정(자동) | `OrderAutoCompleteScheduler` | 배송 완료 7일 후 자동 확정 시에도 동일하게 `accumulatePoint` 호출 |
| 리뷰 작성 | `ReviewService` | 상수 `REVIEW_BASE_POINT`(현재 100)으로 `accumulateReviewPoint` |
| 파트너/관리자 이벤트 | `PartnerEventService`, `AdminEventRewardService` | `accumulateEventPoint`(설명 문자열 지정) |

구매 확정(수동·자동)이 성공하면 `accumulatePoint`가 호출되어 등급 적립률로 포인트가 적립된다.  
같은 주문 상품(`order_item_no`)에 대해 `accumulatePoint`가 다시 호출되면, 이미 `ACCUMULATE` 내역이 있으면 두 번째 적립은 하지 않는다(`existsByCustomerAndOrderItemNoAndPointType`). 보통은 구매 확정 직후 한 번만 적립된다.  
코드상으로는 “그 주문 상품에 `ACCUMULATE`가 하나라도 있으면” `accumulatePoint`가 아무 것도 하지 않으므로, 이벤트·리뷰가 같은 `order_item_no`로 먼저 `ACCUMULATE`를 남긴 뒤에야 구매 확정이 오면 본적립은 스킵될 수 있다. 실서비스 순서에서는 구매 확정이 먼저인 경우가 대부분이다.  
`customer.getCustomerGrade()`가 null이면 구매 확정 시 등급 기반 적립은 실행되지 않는다.

시드 기준 등급 적립률 예(`DataInitializer`): BEGINNER 1.0%, SWIMMER 1.5%, PRO 2.0%, MASTER 2.5%, LEGEND 3.0% — 운영 DB는 관리자가 바꿀 수 있다.

---

## 4. 사용·복구

포인트 사용 — `OrderService.createOrder`에서 `usePointAmount` 검증 후 `pointService.usePoint`:

- `usePointAmount < 0` → `INVALID_REQUEST`
- `usePointAmount >` 할인 반영 후 장바구니 합계(`totalPrice`) → `INVALID_REQUEST` (“주문 금액을 초과”)
- 잔액 부족 → `INSUFFICIENT_POINT` (`PointService.usePoint`)

포인트 복구 (`PointService.restorePoint(customer, orderNo, reason)`)

- 해당 고객·주문번호의 `PointType.USE` 중에서 금액이 음수인 행만 골라 합산한다. 이 합이 “그 주문에서 쓴 포인트”로 본다.
- 합산만큼 잔액을 올리고, `USE` 타입·양수 한 줄을 남긴다. 설명 문자열에 사유와 주문번호가 붙는다.
- 음수 `USE`가 없으면 잔액·내역을 바꾸지 않는다. 이미 설명에 `복구`가 들어간 양수 `USE`가 있으면 중복 복구를 하지 않는다.
- 호출: 주문 취소 `OrderService.cancelOrder` (`"주문 취소 복구"`), 반품 환불 완료 `ReturnService` (`"반품/환불 완료 복구"`).
- 주문 생성 시 음수 `USE`는 보통 주문당 한 건이다. 반품으로 돌려줄 때도 그 주문에 쓴 포인트 합계를 한 번에 돌려주며, 반품 라인 비율로 나누지는 않는다.

---

## 5. REST API (`PointController`, 베이스 `/api`)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| GET | /api/customer/points/balance | 고객 본인 잔액 |
| GET | /api/customer/points/history | 고객 본인 내역(페이지 1-based, 메모리 페이징) |
| POST | /api/admin/customers/{customerId}/points/add | 관리자 수동 지급. 본문 `PointManualRequestDto`: `pointAmount` ≥1, `description` 필수 |
| POST | /api/admin/customers/{customerId}/points/deduct | 관리자 수동 차감(동일 DTO) |
| GET | /api/admin/customers/{customerId}/points/history | 관리자 고객 내역(`page` 0-based, `meta`에 total 등) |

관리자용 “잔액만 조회” 전용 GET은 없고, 고객 잔액은 위 balance API로 본다.

---

## 6. 에러·제한 요약

| 코드·메시지 | 발생 맥락 |
|-------------|-----------|
| `INSUFFICIENT_POINT` | 주문 시 포인트 사용액이 잔액 초과, 또는 수동 차감액이 잔액 초과 |
| `INVALID_REQUEST` | 수동 지급/차감 금액 0 이하, 포인트 사용액 음수·주문금액 초과, `manualAddPoint`/`manualDeductPoint`에서 금액 0 이하 |
| `CUSTOMER_NOT_FOUND` / `ADMIN_NOT_FOUND` | 경로의 고객·세션 관리자 없음 |
| `UNAUTHORIZED` | 비로그인 등 `getCurrentUser` 실패, 또는 고객 API에서 세션의 `customerId`/`subjectId`를 못 읽은 경우 |
| `FORBIDDEN` | `requireRole` 불일치(예: 관리자 세션으로 고객 전용 `/api/customer/points/*` 호출) |

---

## 7. 구현 위치 참고

- `point/service/PointService.java` — 적립·사용·수동·복구
- `point/controller/PointController.java` — REST
- `point/entity/PointHistoryEntity.java`, `point/repository/PointHistoryRepository.java`
- `order/service/OrderService.java` — 주문 생성 시 사용, 구매 확정 시 적립 트리거
- `order/service/OrderAutoCompleteScheduler.java` — 자동 구매 확정 적립
- `review/service/ReviewService.java` — 리뷰 적립
- `event/partner/service/PartnerEventService.java`, `event/admin/service/AdminEventRewardService.java` — 이벤트 적립
- `customer/service/CustomerGradeService.java`, `customer/entity/CustomerGradeEntity.java` — 등급·적립률
