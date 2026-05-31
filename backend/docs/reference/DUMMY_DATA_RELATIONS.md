# 더미 데이터 연관관계 정리

엔티티 FK·생성 순서만 다룬다. **건수·데모 시나리오·계정 이메일**은 [SEED_DEMO_SPEC.md](SEED_DEMO_SPEC.md)가 정본이다.

---

## 생성 순서 및 의존성

```
브랜드·사이즈·컬러 마스터
   ↓
Admin & Partner (2)
   ↓
상품 & 옵션 (26상품) — Partner 필요
   ↓
고객 (2) — CustomerGrade 필요
   ↓
재고 — Option 또는 Product(단일)에 1:1
   ↓
주문 (배송중·취소·구매확정·반품신청UI·return-assist·반품완료)
   ↓
정산 — 구매확정 OrderItem 기준
   ↓
이벤트 (종료3·진행1·예정1)
   ↓
리뷰 (텍스트, 고객별 최대 3건)
   ↓
FAQ (7건) · QnA (3건, count 0일 때)
```

각 단계는 해당 `Repository.count() == 0` 일 때만 실행된다.

---

## 1. 계정 및 사용자 관계

### Account ↔ Admin / Partner / Customer (1:1)

```
Account (5)
├── Admin ×1 — testAdmin@example.com
├── Partner ×2
│   ├── 스피도 — testPartner1@example.com (메인)
│   └── 아레나 — testPartner2@example.com (보조)
└── Customer ×2
    ├── testCustomer1@example.com (메인, 반품 신청 가능 주문)
    └── testCustomer2@example.com (보조)
```

| 관계 | 카디널리티 | FK |
|------|-----------|-----|
| Account ← Admin | 1:1 | `account_id` |
| Account ← Partner | 1:1 | `account_id` |
| Account ← Customer | 1:1 | `account_id` |
| Admin → Partner (승인) | 1:N | Partner.approvedBy |

---

## 2. 카탈로그 관계

```
Partner
└── Product (1:N, partner_id)
    ├── Option (1:N, product_no) — 옵션 있는 상품
    └── Inventory (1:1) — Option 또는 Product(단일)에 연결
```

- `Partner` → `Product` → `Option`: 옵션 상품
- `Partner` → `Product` → `Inventory`: 옵션 없는 단일 상품 (Option 행 없음)
- 옵션·단일 건수: SEED_DEMO_SPEC §4

---

## 3. 고객·등급

```
CustomerGrade (기본 「일반」)
└── Customer (2) ← Account 1:1
```

---

## 4. 주문·결제·배송·반품

```
Customer
└── Order (1:N)
    ├── Payment (0..1) — 취소 주문은 없음
    └── OrderItem (1:N)
        ├── Product, Option (nullable)
        ├── Delivery (0..1) — 취소 제외
        └── Return (0..1)
            └── ReturnImage (0..N) + UploadedFile — assist/REFUNDED 데모만
```

시나리오별 건수·상태: SEED_DEMO_SPEC §5.

| 시나리오 | Return | 비고 |
|----------|--------|------|
| 배송중 / 취소 / 구매확정 | 없음 | |
| 반품 신청 가능 | 없음 | 고객 UI에서 Live 신청 |
| return-assist 데모 | REQUESTED | 이미지 0~2 |
| 반품 완료 | REFUNDED | 이미지 2 |

---

## 5. 정산·이벤트·리뷰·FAQ·QnA

| 블록 | 의존 |
|------|------|
| 정산 | 구매확정 OrderItem (`completedAt != null`) |
| 이벤트 | Admin |
| 리뷰 | Customer, OrderItem(구매확정) |
| FAQ | Admin |
| QnA | Customer; 파트너 건은 OrderItem → Partner 라우팅 |

---

## 주의사항

1. **순서 의존성**: Partner → Product → Option → Inventory → Order. CustomerGrade는 없으면 자동 생성.
2. **FK 제약**: `@Transactional`로 단계별 시드; 순서를 바꾸면 FK 오류 가능.
3. **중복 방지**: 단계마다 count 0 체크. 기존 DB에 데이터가 있으면 해당 블록은 건너뜀.
