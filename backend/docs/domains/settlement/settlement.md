# 정산 (Settlement) 도메인

파트너 판매 주문 상품을 묶어 정산 배치(`settlement`)를 만들고, 수수료를 뺀 금액·상태·지급일을 관리한다. 생성·상태 변경은 관리자만, 파트너는 조회·다운로드만 한다.

---

## 1. 상태 (`SettlementStatus`)

소스: `common/enums/SettlementStatus.java`  
`SettlementEntity.settlementStatus`는 JPA `@Enumerated(STRING)`으로 DB에 enum 이름 문자열이 저장된다.

| 값 | 의미 |
|----|------|
| PENDING | 지급 대기 |
| PROCESSING | 사용 안 함(하위 호환용으로 enum만 남음). `getSettlementDashboard`·`getSettlementList` 등 일부 조회에서 레거시 `PROCESSING` 행을 읽을 때 `PENDING`으로 고쳐 저장할 수 있다 |
| COMPLETED | 지급 완료 처리 |
| CANCELLED | 취소. 이 상태가 아닌 정산에 묶인 주문 상품은 정산 대상 조회에서 제외된다 |

---

## 2. 데이터 모델

- `SettlementEntity` — `settlement_id`, `partner_id`, `admin_id`, `total_sales_amount`, `commission_amount`, `settlement_amount`, `settlement_created_at`, `settlement_status`, `settlement_period_start` / `end`, `settlement_paid_date`
- `SettlementOrderItemEntity` (`settlement_order_item`) — 정산과 `OrderItem` 다대일 연결. 주문 엔티티에 정산 FK는 없다.
- `SettlementHistoryEntity` (`settlement_history`) — 관리자가 수행한 생성·상태 변경·지급일 수정 이력. `action_type` 문자열(`CREATE`, `STATUS_CHANGE`, `PAID_DATE_UPDATE`), `old_value` / `new_value`(JSON), `reason`, `changed_at`, `admin_id`

이력 기록 실패 시에도 본 처리(생성·상태 변경)는 롤백하지 않고 진행한다(`SettlementService` try/catch). 의도는 이력 저장(JSON 직렬화·DB 오류 등)이 나도 정산 본건이 커밋되게 하려는 것이다. 감사 추적을 무엇보다 우선하면 같은 트랜잭션에서 이력까지 성공할 때만 커밋하도록 바꾸는 편이 맞고, 그때는 예외를 삼키지 않는다.

---

## 3. 정산 가능 조건

`SettlementService.isSettlementReady`와 정산 대상 JPQL이 함께 쓰인다.

라인이 정산 가능으로 표시되려면(요약, `isSettlementReady`와 정산 대상 JPQL 공통에 가깝다):

- 배송이 있고 상태가 `DELIVERED`
- 정산 대상 조회 JPQL에는 추가로 `delivery.deliveryEndDate IS NOT NULL`이 들어 있다(배송 완료일이 비어 있으면 목록에 안 나온다)
- `OrderItem.completedAt`이 있음(구매 확정)
- `isCancelled == false`
- 반품이 있어도 `ReturnStatus.REFUNDED`가 아니어야 함(환불 완료된 라인은 정산 대상에서 제외)

정산 대상 목록 쿼리(`OrderItemRepository`의 `findSettlementReady*` 등)에서는 추가로, `SettlementOrderItem`에 묶여 있으면서 연결된 `Settlement`의 상태가 `CANCELLED`가 아닌 경우 해당 주문 상품을 빼 준다. 그래서 취소된 정산에만 묶였던 라인은 다시 정산 후보에 나올 수 있다(과거 분석 문서의 이슈는 현재 쿼리에서 반영됨).

---

## 4. 금액·기간

- 판매액: 각 `OrderItemEntity.itemTotalPrice` 합산
- 수수료: `SettlementService` 상수 `COMMISSION_RATE = 0.10`(판매액의 10%, `Math.round`로 원 단위)
- 정산금액: 판매액 − 수수료
- 정산 기간(`settlement_period_start` / `end`): 생성 시 선택된 주문 상품들의 주문일(`order.getOrderCreatedAt()`의 날짜) 최소·최대로 자동 설정된다. 같은 배치에 들어간 라인은 모두 구매 확정된 상태이지만, 기간 표시에는 구매 확정일(`completedAt`)을 쓰지 않는다. 요청 DTO에 `settlementPeriodStart` / `settlementPeriodEnd`가 있어도 `createSettlement`는 읽지 않는다.  
  참고: 실무에서는 매출 인식일(구매 확정일) 이나 매월 n일~말일 같은 정산 주기로 묶는 경우가 많다. 그렇게 바꾸려면 기간 산정 로직·UI·약관을 함께 설계해야 한다(현재 MVP는 “선택 라인의 주문일 범위” 모델).

### 일자 필드가 헷갈릴 때 (용어)

말로 “정산 기간”이라고 하면 관리자가 파트너에게 돈을 지급한 날·정산 주기를 떠올리기 쉽다. 이 코드베이스에서는 아래처럼 역할이 나뉜다.

| DB 컬럼(엔티티) | 의미 | 비고 |
|-----------------|------|------|
| `settlement_period_start` / `end` | 이번 배치에 넣은 주문들의 주문일 최소·최대 | 지급일·송금일과 무관. UI에는 “매출(주문)일 범위” 등으로 쓰는 편이 덜 오해가 난다. |
| `settlement_created_at` | 정산 배치 레코드를 생성한 날 | 관리자 `POST` 생성 시점. |
| `settlement_paid_date` | 지급 완료를 기록하는 날짜 | 자동이 아니라 관리자가 상태 변경 API의 `paidDate`로 넣는다. 아래 “정산 지급일 로직”. |

### 정산 지급일(`settlement_paid_date`) 로직

- 생성(`POST /api/admin/settlement`): `settlement_paid_date`는 채우지 않는다. 보통 null로 시작한다.
- 변경(`PATCH /api/admin/settlement/{settlementId}/status`): 본문에 `status`는 필수, `paidDate`는 선택이다. `SettlementService#updateSettlementStatus` 분기는 다음과 같다.
  1. `paidDate`가 있고, 요청 `status`와 DB 상태가 모두 `COMPLETED`인 경우: 상태는 그대로 두고 지급일만 바꾼다. 이력 `action_type`은 `PAID_DATE_UPDATE`.
  2. `paidDate`가 있는데 위가 아닌 경우: `updateStatus(status, paidDate)`로 상태와 지급일을 함께 반영한다. 이력은 `STATUS_CHANGE`.
  3. `paidDate`가 null인 경우: `updateStatus(status)`로 상태만 바꾼다. 이때는 지급일 필드를 건드리지 않는다(이미 들어 있던 지급일은 유지).

즉 지급일은 PG·은행 API가 아니라 관리자 입력(또는 프론트가 같은 날짜를 넣는 운영 흐름)에 의존한다.

---

## 5. 처리 흐름

1. 관리자·파트너가 정산 대상 라인을 조회한다(기간·파트너·`SETTLEMENT_READY` / `ALL` 필터 등).
2. 관리자만 동일 파트너 소속 라인만 골라 `POST /api/admin/settlement`로 배치를 만든다. 요청 본문 필수는 `partnerId`, `orderItemIds`(`SettlementCreateRequestDto`). 기간 필드는 §4 참고.
3. 관리자가 `PATCH /api/admin/settlement/{settlementId}/status`로 상태를 바꾼다. `paidDate`는 선택이나, `COMPLETED`로 바꿀 때 함께 넣는 흐름이 일반적이다. 이미 `COMPLETED`인 건에 대해 `status=COMPLETED`와 새 `paidDate`만 주면 지급일만 수정(`PAID_DATE_UPDATE` 이력).
4. 목록·상세·대시보드·PDF·Excel·이력 조회는 아래 API 표를 본다.

---

## 6. REST API

베이스는 세션 인증 전제다. 관리자는 `AccountRole.ADMIN`, 파트너는 `PARTNER`.

### 관리자 (`/api/admin`)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| GET | /api/admin/settlement | 정산 대상 라인(파트너·기간·status 쿼리) |
| POST | /api/admin/settlement | 정산 생성 |
| PATCH | /api/admin/settlement/{settlementId}/status | 상태·지급일 변경(`status` 필수, `paidDate` 선택) |
| GET | /api/admin/settlements | 생성된 정산 목록 |
| GET | /api/admin/settlements/dashboard | 대시보드 집계 |
| GET | /api/admin/settlements/excel | 목록 Excel |
| GET | /api/admin/settlements/{settlementId}/pdf | 정산서 PDF |
| GET | /api/admin/settlements/{settlementId}/history | 변경 이력 |
| GET | /api/admin/settlements/{settlementId} | 상세(포함 주문 상품) |

개발 참고: `AdminController`에서 `/settlements/excel`, `/settlements/dashboard`는 `/settlements/{settlementId}`보다 먼저 매핑해 두었다. 순서를 바꾸면 `excel` 같은 글자가 정산 ID로 잘못 들어가 요청이 깨질 수 있다.

### 파트너 (`SettlementController` 베이스 `/api/partner/settlement`)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| GET | /api/partner/settlement | 본인 정산 대상(기간·status) |
| GET | /api/partner/settlement/list | 본인 정산 배치 목록 |
| GET | /api/partner/settlement/list/{settlementId} | 본인 정산 상세 |
| GET | /api/partner/settlement/list/{settlementId}/pdf | 본인 정산 PDF |
| GET | /api/partner/settlement/list/excel | 본인 정산 목록 Excel |

파트너는 정산 배치 목록·상세·PDF·Excel은 호출할 수 있으나, 관리자용 변경 이력(`GET .../history`) API는 없다.

---

## 7. 이력 (`SettlementHistoryAction`)

enum 라벨과 별개로 DB `action_type`에는 다음 문자열이 들어간다.

- CREATE — 정산 생성 직후
- STATUS_CHANGE — 상태 변경(지급일 동반 여부 포함)
- PAID_DATE_UPDATE — 완료 상태 유지한 채 지급일만 변경

---

## 8. 오류·권한 요약

| 코드 | 발생 맥락(예) |
|------|----------------|
| FORBIDDEN | 파트너가 타 파트너 정산 상세·파일 요청, 관리자 전용 API에 파트너 세션 |
| INVALID_REQUEST | 정산 불가 라인 포함, 요청 `partnerId`와 실제 상품 소유 파트너 불일치 |
| PARTNER_NOT_FOUND / ORDER_ITEM_NOT_FOUND | 잘못된 ID |
| UNAUTHORIZED | 비로그인 등 `getCurrentUser` 실패 |

일부 내부 예외는 `IllegalArgumentException`("관리자를 찾을 수 없습니다" 등)으로 처리되는 구간이 있으니 API 응답 매핑은 글로벌 예외 핸들러를 함께 본다.

---

## 9. 검증 시나리오(요약)

- 정산 생성: 동일 파트너·정산 가능 라인만. 혼합 파트너·미배송·미구매확정·취소·환불 완료 라인은 거절.
- 상태: `updateSettlementStatus`는 허용 전이를 따로 검사하지 않고, `PATCH`의 `status`·`paidDate`로 엔티티를 갱신한다. 운영상으로는 `PENDING` → `COMPLETED` → `CANCELLED`가 일반적이다. 이미 `COMPLETED`인 건에 `status=COMPLETED`와 새 `paidDate`를 주면 지급일만 바뀐다(`PAID_DATE_UPDATE` 이력).
- 권한: 파트너는 본인 데이터만 조회·다운로드.
- PDF·Excel: 한글·필터 동작은 `SettlementExportService` 및 프론트와 함께 확인.

---

## 10. 구현 위치

- `settlement/service/SettlementService.java` — 핵심 도메인 로직
- `settlement/service/SettlementExportService.java` — PDF·Excel
- `settlement/controller/SettlementController.java` — 파트너 API
- `admin/controller/AdminController.java` — 관리자 정산 API 구간
- `settlement/entity/*`, `settlement/repository/*`
- `order/repository/OrderItemRepository.java` — 정산 대상 JPQL
