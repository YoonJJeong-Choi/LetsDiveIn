# 상태(Status) 관리 가이드

이 문서는 Swim Mall 프로젝트에서 사용되는 모든 상태(Status) enum을 정리하고 설명합니다.

---

## 📋 목차

1. [주문 상태 (OrderStatus)](#1-주문-상태-orderstatus)
2. [배송 상태 (DeliveryStatus)](#2-배송-상태-deliverystatus)
3. [반품 상태 (ReturnStatus)](#3-반품-상태-returnstatus)
4. [정산 상태 (SettlementStatus)](#4-정산-상태-settlementstatus)
5. [파트너 상태 (PartnerStatus)](#5-파트너-상태-partnerstatus)
6. [관리자 상태 (AdminStatus)](#6-관리자-상태-adminstatus)
7. [활성 상태 (ActiveStatus)](#7-활성-상태-activestatus)
8. [상태 전이 흐름도](#8-상태-전이-흐름도)

---

## 1. 주문 상태 (OrderStatus)

**파일**: `common/enums/OrderStatus.java`

주문의 전체 생명주기를 관리하는 상태입니다.

### 상태 목록

| 상태 | 코드 | 설명 | 다음 가능한 상태 |
|------|------|------|-----------------|
| **결제 대기중** | `PENDING_PAYMENT` | 주문 생성 직후 상태 (결제 대기) | `PAID`, `PAYMENT_FAILED`, `CANCELLED` |
| **결제 완료** | `PAID` | 결제 성공 후 상태 (발주 확인 대기) | `ACTIVE`, `CANCELLED` |
| **결제 실패** | `PAYMENT_FAILED` | 결제 실패 상태 | `PENDING_PAYMENT` (재시도) |
| **주문 진행중** | `ACTIVE` | 주문 처리 중 (일부 또는 전체 상품 발주 확인됨, 배송 준비 → 배송 중 → 배송 완료) | `CANCELLED` (배송 시작 전만) |
| **주문 취소** | `CANCELLED` | 주문 전체 취소 | 변경 불가 |

**⚠️ 중요:** 구매 확정은 `Order.orderStatus`가 아닌 `OrderItem.completedAt`으로 관리합니다. 각 주문 상품별로 개별 구매 확정이 가능하며, 주문 레벨에서는 구매 확정 상태를 관리하지 않습니다.

### 상태 전이 규칙

```
PENDING_PAYMENT (결제 대기중)
    ↓
    ├─→ PAID (결제 완료) → ACTIVE (주문 진행중)
    ├─→ PAYMENT_FAILED (결제 실패) → PENDING_PAYMENT (재시도)
    └─→ CANCELLED (주문 취소) [배송 시작 전만 가능]

주의: 구매 확정은 OrderItem.completedAt으로 관리합니다.
      모든 주문 상품이 발주 확인되면 Order.orderStatus가 ACTIVE로 변경됩니다.
```

### 주요 제약사항

- **취소 가능 시점**: 
  - `PENDING_PAYMENT`: 언제든지 취소 가능
  - `PAID`: 발주 확인 전까지 취소 가능
  - `ACTIVE`: 배송이 시작되지 않은 경우만 취소 가능 (배송 상태가 `READY`일 때)
  - 배송이 시작된 후 (`SHIPPED`, `DELIVERED`)에는 취소 불가, 반품으로 처리

- **구매 확정 조건** (OrderItem.completedAt으로 관리): 
  - 배송이 완료되어야 함 (`DELIVERED`)
  - 반품 신청 중이거나 환불 완료된 주문 상품은 구매 확정 불가
  - 배송 완료 후 7일 경과 시 자동 구매 확정
  - **각 주문 상품별로 개별 구매 확정 가능**
  - **배송 완료와 구매 확정은 별개**: 배송 완료는 `DeliveryStatus`, 구매 확정은 `OrderItem.completedAt`으로 관리됨

- **발주 확인**: 
  - `PAID` 상태에서 파트너/관리자가 발주 확인 API 호출
  - 발주 확인 시 `ACTIVE`로 전이 + 배송 생성 (`READY` 상태)
  - 파트너는 본인 상품 주문만 발주 확인 가능

- **주문 상태 변경 권한**:
  - **관리자**: 
    - ✅ **변경 가능**: 
      - `PENDING_PAYMENT` → `PAID`, `PAYMENT_FAILED`, `CANCELLED`
      - `PAID` → `ACTIVE`, `CANCELLED`
      - `ACTIVE` → `CANCELLED` (배송 시작 전만)
      - `PAYMENT_FAILED` → `PENDING_PAYMENT` (재시도)
    - ❌ **변경 불가**: 
      - 구매 확정된 주문 상품이 있는 주문은 취소 불가 (구매 확정은 `OrderItem.completedAt`으로 관리)
      - `CANCELLED` → 다른 상태 (취소는 되돌릴 수 없음)
      - 배송 시작 후 (`SHIPPED`, `DELIVERED`) 취소 불가
  - **파트너**: 주문 상태 변경 불가 (발주 확인 API만 사용)
  - **고객**: 주문 상품별 구매 확정 가능 (`OrderItem.completedAt` 설정)

---

## 2. 배송 상태 (DeliveryStatus)

**파일**: `common/enums/DeliveryStatus.java`

각 주문 아이템(OrderItem)의 배송 상태를 관리합니다.

### 상태 목록

| 상태 | 코드 | 설명 | 다음 가능한 상태 |
|------|------|------|-----------------|
| **배송 준비** | `READY` | 발주 확인 후 배송 준비 상태 | `SHIPPED` |
| **배송 중** | `SHIPPED` | 배송 시작 (송장번호 입력 후) | `DELIVERED` |
| **배송 완료** | `DELIVERED` | 배송 완료 | 변경 불가 |

### 상태 전이 흐름

```
READY (배송 준비)
    ↓ [배송 시작 API 호출]
SHIPPED (배송 중)
    ↓ [배송 완료 API 호출]
DELIVERED (배송 완료)
```

### 주요 특징

- 각 `OrderItem`마다 별도의 `Delivery` 엔티티가 생성됨 (1:1 관계)
- 주문 상품별 발주 확인 시점에 `Delivery` 생성 (초기 상태: `READY`)
- 배송 시작 시 송장번호와 택배사 정보 입력 필요
- 배송 완료 후 해당 주문 상품의 구매 확정 가능 (`OrderItem.completedAt` 설정)

---

## 3. 반품 상태 (ReturnStatus)

**파일**: `common/enums/ReturnStatus.java`

주문 아이템의 반품 처리 상태를 관리합니다.

### 상태 목록

| 상태 | 코드 | 설명 | 다음 가능한 상태 |
|------|------|------|-----------------|
| **반품신청** | `REQUESTED` | 고객이 반품 신청 | `APPROVED`, `REJECTED` |
| **반품승인** | `APPROVED` | 관리자/파트너가 반품 승인 | `PICKUP_COMPLETED` |
| **반품거절** | `REJECTED` | 관리자/파트너가 반품 거절 | 변경 불가 |
| **수거완료** | `PICKUP_COMPLETED` | 반품 상품 수거 완료 (상품 확인 전) | `REFUNDED`, `REJECTED` |
| **환불완료** | `REFUNDED` | 환불 처리 완료 | 변경 불가 |

### 상태 전이 흐름

```
REQUESTED (반품신청)
    ↓
    ├─→ APPROVED (반품승인) → PICKUP_COMPLETED (수거완료)
    │                           ↓ [상품 확인 후]
    │                           ├─→ REFUNDED (환불완료) [상품 정상]
    │                           └─→ REJECTED (반품거절) [상품 이상]
    └─→ REJECTED (반품거절) [신청 단계에서 거절, 예: 기간 초과 등]
```

### 주요 특징

- 배송 완료 후 반품 신청 가능 (단, 구매 확정되지 않은 주문 상품만 가능)
- 각 `OrderItem`마다 별도의 `Return` 엔티티 생성 (1:1 관계)
- 반품 금액은 주문 아이템의 총 가격 기준
- 반품 신청 후 관리자/파트너의 승인/거절 단계 필수
- 수거 완료 후 상품 확인 단계: 상품이 정상이면 환불, 이상이 있으면 거절
- 거절된 반품은 더 이상 처리 불가 (두 가지 경우)
  - 신청 단계에서 거절: `REQUESTED` → `REJECTED` (예: 반품 기간 초과)
  - 수거 후 거절: `PICKUP_COMPLETED` → `REJECTED` (예: 상품 상태 불량)
- 승인(`APPROVED`) 상태에서는 거절 불가 (승인 = 반품을 받겠다는 의미이므로 수거 진행해야 함)

---

## 4. 정산 상태 (SettlementStatus)

**파일**: `common/enums/SettlementStatus.java`

파트너의 정산 처리 상태를 관리합니다.

### 상태 목록

| 상태 | 코드 | 설명 | 다음 가능한 상태 |
|------|------|------|-----------------|
| **대기** | `PENDING` | 정산 대기 상태 | `PROCESSING` |
| **처리중** | `PROCESSING` | 정산 처리 중 | `COMPLETED`, `CANCELLED` |
| **완료** | `COMPLETED` | 정산 완료 | 변경 불가 |
| **취소** | `CANCELLED` | 정산 취소 | 변경 불가 |

### 상태 전이 흐름

```
PENDING (대기)
    ↓ [정산 시작]
PROCESSING (처리중)
    ↓
    ├─→ COMPLETED (완료)
    └─→ CANCELLED (취소)
```

---

## 5. 파트너 상태 (PartnerStatus)

**파일**: `common/enums/PartnerStatus.java`

파트너(입점업체)의 운영 상태를 관리합니다.

### 상태 목록

| 상태 | 코드 | 설명 | 다음 가능한 상태 |
|------|------|------|-----------------|
| **대기** | `PENDING` | 입점 신청 대기 상태 | `APPROVED`, `INACTIVE` |
| **승인** | `APPROVED` | 입점 승인 완료 (정상 운영) | `INACTIVE` |
| **비활성** | `INACTIVE` | 입점 정지/비활성 상태 | `APPROVED` |

### 상태 전이 흐름

```
PENDING (대기)
    ↓ [관리자 승인]
APPROVED (승인)
    ↓ [정지 처리]
INACTIVE (비활성)
    ↓ [재승인]
APPROVED (승인)
```

---

## 6. 관리자 상태 (AdminStatus)

**파일**: `common/enums/AdminStatus.java`

관리자 계정의 운영 상태를 관리합니다.

### 상태 목록

| 상태 | 코드 | 설명 | 다음 가능한 상태 |
|------|------|------|-----------------|
| **활성** | `ACTIVE` | 활성 상태 (정상 운영) | `INACTIVE` |
| **비활성** | `INACTIVE` | 비활성 상태 (계정 정지) | `ACTIVE` |

### 상태 전이 흐름

```
ACTIVE (활성)
    ↓ [정지 처리]
INACTIVE (비활성)
    ↓ [재활성화]
ACTIVE (활성)
```

---

## 7. 활성 상태 (ActiveStatus)

**파일**: `common/enums/ActiveStatus.java`

상품, 옵션 등의 활성/삭제 상태를 관리합니다.

### 상태 목록

| 상태 | 코드 | 설명 | 사용 대상 |
|------|------|------|----------|
| **활성** | `ACTIVE` | 활성 상태 (정상 사용 가능) | 상품, 옵션 |
| **삭제** | `DELETE` | 삭제 상태 (비활성) | 상품, 옵션 |

### 주요 특징

- 논리 삭제(Soft Delete) 방식 사용
- `DELETE` 상태는 실제로 DB에서 삭제되지 않음
- 고객용 상품 목록 조회 시 `ACTIVE` 상태만 노출

---

## 8. 상태 전이 흐름도

### 전체 주문 흐름 (정상 케이스)

```
[주문 생성] POST /api/orders
    ↓
PENDING_PAYMENT (결제 대기중)
    ↓ [결제 승인] POST /api/payments/approve
PAID (결제 완료)
    ↓ [주문 상품별 발주 확인] POST /api/orders/order-items/{orderItemNo}/confirm ← 파트너/관리자
    ├─→ [배송 생성] → Delivery READY (배송 준비) [자동 생성]
    └─→ [모든 상품 발주 확인 완료 시] → ACTIVE (주문 진행중)
ACTIVE (주문 진행중)
    ├─→ [배송 시작] PATCH /api/deliveries/{deliveryNo}/start → SHIPPED (배송 중)
    ├─→ [배송 완료] PATCH /api/deliveries/{deliveryNo}/complete → DELIVERED (배송 완료)
    └─→ [주문 상품별 구매 확정] POST /api/orders/order-items/{orderItemNo}/complete → OrderItem.completedAt 설정
         또는 [자동 구매 확정] 배송 완료 후 7일 경과 시 OrderItem.completedAt 자동 설정

주의: 구매 확정은 Order.orderStatus가 아닌 OrderItem.completedAt으로 관리합니다.
```

### 취소 흐름

```
PENDING_PAYMENT → CANCELLED (언제든지 가능)
    ↓ [주문 취소] PATCH /api/orders/{orderNo}/status

PAID → CANCELLED (발주 확인 전까지 가능)
    ↓ [주문 취소] PATCH /api/orders/{orderNo}/status

ACTIVE → CANCELLED (배송 시작 전만 가능)
    ↓ [주문 취소] PATCH /api/orders/{orderNo}/status
    조건: 모든 Delivery 상태가 READY일 때만 가능
    (SHIPPED 또는 DELIVERED가 있으면 취소 불가)
```

### 결제 실패 및 재시도 흐름

```
PENDING_PAYMENT → PAYMENT_FAILED (결제 실패)
    ↓ [결제 실패 처리] POST /api/payments/{orderNo}/fail
PAYMENT_FAILED → PENDING_PAYMENT (재시도)
    ↓ [재결제] POST /api/payments/approve
```

### 반품 흐름

```
[배송 완료 후] DELIVERED 상태
    ↓
REQUESTED (반품신청) POST /api/returns
    ↓
    ├─→ APPROVED (반품승인) POST /api/returns/{returnNo}/approve
    │   ↓ [수거 완료 처리] PATCH /api/returns/{returnNo}
    │   PICKUP_COMPLETED (수거완료)
    │   ↓ [상품 확인 후]
    │   ├─→ REFUNDED (환불완료) PATCH /api/returns/{returnNo} [상품 정상]
    │   └─→ REJECTED (반품거절) POST /api/returns/{returnNo}/reject [상품 이상]
    └─→ REJECTED (반품거절) POST /api/returns/{returnNo}/reject [신청 단계에서 거절]
```

**반품과 구매 확정의 관계:**
- 반품 신청 중(`REQUESTED`, `APPROVED`, `PICKUP_COMPLETED`)이거나 환불 완료(`REFUNDED`)된 주문 상품은 구매 확정 불가
- 반품 거절(`REJECTED`) 후에는 구매 확정 가능
- 구매 확정된 주문 상품(`OrderItem.completedAt`이 설정된 경우)은 반품 신청 불가
- 반품 거절 시 자동 구매 확정되지 않음 (고객이 수동으로 구매 확정해야 함)

### 상태 간 관계도

```
OrderStatus (주문 상태)
    │
    ├─→ DeliveryStatus (배송 상태) [1:N 관계]
    │   └─→ OrderItem 1:1 Delivery
    │
    └─→ ReturnStatus (반품 상태) [1:N 관계]
        └─→ OrderItem 1:1 Return
```

**중요**: 
- 하나의 주문(Order)은 여러 주문 상품(OrderItem)을 가질 수 있음
- 각 OrderItem마다 별도의 Delivery, Return, Review가 생성됨 (1:1 관계)
- 주문 상태는 주문 전체의 진행 단계만 관리 (PENDING_PAYMENT, PAID, ACTIVE, CANCELLED)
- 주문 상품별 상세 상태는 OrderItem에서 관리:
  - 발주 확인: `OrderItem.confirmedAt`
  - 구매 확정: `OrderItem.completedAt`
  - 배송 상태: `Delivery.deliveryStatus`
  - 반품 상태: `Return.returnStatus`
- 주문 취소 가능 조건:
  - 모든 Delivery가 READY 상태일 때만 가능
  - 구매 확정된 주문 상품이 있으면 취소 불가
- 배송 완료(`DELIVERED`)와 구매 확정(`COMPLETED`)은 별개 프로세스
  - 배송 완료: `DeliveryStatus`로 관리 (배송사가 처리)
  - 구매 확정: `OrderStatus`로 관리 (고객이 수동 선택 또는 배송 완료 후 7일 자동)

---

## 📌 주요 상태별 역할 정리

| 상태 타입 | 역할 | 주요 사용자 |
|----------|------|------------|
| **OrderStatus** | 주문 전체 생명주기 관리 | 고객, 파트너, 관리자 |
| **DeliveryStatus** | 배송 진행 상황 관리 | 파트너, 관리자 |
| **ReturnStatus** | 반품 처리 과정 관리 | 고객, 파트너, 관리자 |
| **SettlementStatus** | 정산 처리 상태 관리 | 관리자 |
| **PartnerStatus** | 파트너 운영 상태 관리 | 관리자 |
| **AdminStatus** | 관리자 계정 상태 관리 | 관리자 |
| **ActiveStatus** | 상품/옵션 활성화 상태 관리 | 파트너, 관리자 |

---

## 🔍 상태 확인 팁

1. **주문 상태 확인**: `OrderEntity.orderStatus`
2. **배송 상태 확인**: `DeliveryEntity.deliveryStatus`
3. **반품 상태 확인**: `ReturnEntity.returnStatus`
4. **정산 상태 확인**: `SettlementEntity.settlementStatus`
5. **파트너 상태 확인**: `PartnerEntity.partnerStatus`
6. **관리자 상태 확인**: `AdminEntity.status` (또는 `AccountEntity.status`)
7. **상품/옵션 활성 상태**: `ProductEntity.productActiveStatus`, `OptionEntity.optionStatus`

---

## ⚠️ 주의사항

1. **상태 변경은 검증 로직을 통해만 가능**: `changeStatus()` 메서드 사용
2. **자동 상태 변경**: `updateStatus()` 메서드는 검증 없이 변경 (시스템 내부용)
3. **취소된 주문은 상태 변경 불가**: `CANCELLED` 상태는 최종 상태
4. **구매 확정된 주문은 상태 변경 불가**: `COMPLETED` 상태는 최종 상태
5. **배송이 시작된 주문은 취소 불가**: 반품으로 처리해야 함
6. **발주 확인은 필수**: `PAID` 상태에서 `ACTIVE`로 전이하려면 반드시 발주 확인 API 호출 필요
7. **배송 생성 시점**: 발주 확인 시점에 자동 생성됨 (결제 완료 시점이 아님)

## 🔗 상태 변경 API 엔드포인트

| 상태 변경 | API 엔드포인트 | 권한 | 조건 |
|----------|---------------|------|------|
| **결제 승인** | `POST /api/payments/approve` | 고객 | `PENDING_PAYMENT` 상태 |
| **결제 실패** | `POST /api/payments/{orderNo}/fail` | 고객 | `PENDING_PAYMENT` 상태 |
| **발주 확인** | `POST /api/orders/{orderNo}/confirm` | 파트너/관리자 | `PAID` 상태 |
| **배송 시작** | `PATCH /api/deliveries/{deliveryNo}/start` | 파트너/관리자 | `READY` 상태 |
| **배송 완료** | `PATCH /api/deliveries/{deliveryNo}/complete` | 파트너/관리자 | `SHIPPED` 상태 |
| **구매 확정** | `POST /api/orders/{orderNo}/complete` | 고객 | 모든 배송이 `DELIVERED` |
| **주문 상태 변경** | `PATCH /api/orders/{orderNo}/status` | 관리자만 | 예외 상황 처리용 (결제 실패 재시도, 주문 취소 등) |
| **반품 신청** | `POST /api/returns` | 고객 | 배송 완료 후 |
| **반품 승인** | `POST /api/returns/{returnNo}/approve` | 파트너/관리자 | `REQUESTED` 상태 |
| **반품 거절** | `POST /api/returns/{returnNo}/reject` | 파트너/관리자 | `REQUESTED` 또는 `PICKUP_COMPLETED` 상태 |
| **수거 완료** | `PATCH /api/returns/{returnNo}` | 파트너/관리자 | `APPROVED` 상태 |
| **환불 처리** | `PATCH /api/returns/{returnNo}` | 파트너/관리자 | `PICKUP_COMPLETED` 상태 (상품 정상) |

---

**마지막 업데이트**: 2024년 (발주 확인 기능 추가 후)
