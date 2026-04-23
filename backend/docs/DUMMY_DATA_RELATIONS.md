# 더미 데이터 연관관계 정리

## 생성 순서 및 의존성

```
1. Admin & Partner (createAdminAndPartners)
   ↓
2. 상품 & 옵션 (createProductsAndOptions) - Partner 필요
   ↓
3. 고객 (createCustomers) - CustomerGrade 필요 (없으면 자동 생성)
   ↓
4. 재고 (createInventories) - Product & Option 필요
   ↓
5. 주문 (createOrders) - Customer, Product, Option 필요
```

---

## 1. 계정 및 사용자 관계

### Account ↔ Admin/Partner/Customer (1:1)
```
Account (1개)
├── Admin (1개)
│   └── email: admin@swim-mall.com
│   └── password: admin123
│   └── role: ADMIN
│
Account (2개)
├── Partner (2개)
│   ├── Partner 1: "수영용품 전문샵"
│   │   └── email: partner1@swim-mall.com
│   │   └── password: partner123
│   │   └── role: PARTNER
│   │   └── approvedBy: Admin
│   │
│   └── Partner 2: "비치웨어 스토어"
│       └── email: partner2@swim-mall.com
│       └── password: partner123
│       └── role: PARTNER
│       └── approvedBy: Admin
│
Account (10개)
└── Customer (10명)
    ├── customer1@test.com ~ customer10@test.com
    ├── password: customer123
    ├── role: CUSTOMER
    └── customerGrade: CustomerGrade (기본 등급 "일반")
```

**관계:**
- `Account` ← `Admin` (1:1, FK: account_id)
- `Account` ← `Partner` (1:1, FK: account_id)
- `Account` ← `Customer` (1:1, FK: account_id)
- `Admin` → `Partner` (승인자 관계, N:1)

---

## 2. 상품 및 옵션 관계

### Partner → Product → Option
```
Partner 1: "수영용품 전문샵"
├── Product 1: "실리콘 수영모자"
│   ├── Option 1-1: 블랙-S
│   ├── Option 1-2: 블랙-M
│   ├── Option 1-3: 블랙-L
│   ├── Option 1-4: 네이비-S
│   ├── Option 1-5: 네이비-M
│   └── Option 1-6: 네이비-L
│
├── Product 2: "여성 원피스 수영복"
│   ├── Option 2-1: 블랙-S
│   ├── Option 2-2: 블랙-M
│   ├── Option 2-3: 블랙-L
│   ├── Option 2-4: 네이비-S
│   ├── Option 2-5: 네이비-M
│   └── Option 2-6: 네이비-L
│
└── Product 4: "수영용 수건" (옵션 없음)

Partner 2: "비치웨어 스토어"
└── Product 3: "프리미엄 수영안경"
    ├── Option 3-1: 투명-일반
    └── Option 3-2: 미러-일반 (+5000원)
```

**관계:**
- `Partner` → `Product` (1:N, FK: partner_id)
- `Product` → `Option` (1:N, FK: product_no)
- `Partner` → `Option` (1:N, FK: partner_id)

**총계:**
- 상품: 4개 (옵션 있는 상품 3개, 옵션 없는 상품 1개)
- 옵션: 14개 (Product 1: 6개, Product 2: 6개, Product 3: 2개, Product 4: 0개)

---

## 3. 고객 및 등급 관계

### CustomerGrade → Customer
```
CustomerGrade (기본 등급)
├── gradeName: "일반"
├── gradeLevel: 1
├── minPurchaseAmount: 0
├── minOrderCount: 0
├── discountRate: 0.0%
└── pointAccumulationRate: 1.0%

Customer (10명)
├── customer1@test.com (김철수) - createAt: 90일 전
├── customer2@test.com (이영희) - createAt: 81일 전
├── customer3@test.com (박민수) - createAt: 72일 전
├── customer4@test.com (최지영) - createAt: 63일 전
├── customer5@test.com (정대현) - createAt: 54일 전
├── customer6@test.com (한소희) - createAt: 45일 전
├── customer7@test.com (윤성호) - createAt: 36일 전
├── customer8@test.com (강미영) - createAt: 27일 전
├── customer9@test.com (임동욱) - createAt: 18일 전
└── customer10@test.com (오수진) - createAt: 9일 전
```

**관계:**
- `CustomerGrade` → `Customer` (1:N, FK: grade_id)
- `Account` → `Customer` (1:1, FK: account_id)

---

## 4. 재고 관계

### Inventory → Option / Product
```
Inventory (옵션이 있는 상품)
├── Option 1-1 (블랙-S) → 재고 10개
├── Option 1-2 (블랙-M) → 재고 10개
├── Option 1-3 (블랙-L) → 재고 10개
├── ... (총 14개 옵션에 각 10개씩)

Inventory (옵션이 없는 상품)
└── Product 4 (수영용 수건) → 재고 10개
```

**관계:**
- `Option` → `Inventory` (1:1, FK: option_no, nullable)
- `Product` → `Inventory` (1:1, FK: product_no, nullable)
- **주의:** 옵션이 있으면 Option에 연결, 없으면 Product에 연결

**총계:**
- 재고: 15개 (옵션 14개 + 상품 1개)

---

## 5. 주문 관계 (복잡)

### 주문 완료(배송중) - 3개
```
Order (3개)
├── Order 1
│   ├── Customer (랜덤)
│   ├── Payment (결제 완료)
│   ├── OrderItem (1개)
│   │   ├── Product (랜덤)
│   │   ├── Option (랜덤, nullable)
│   │   ├── confirmedAt: 설정됨
│   │   └── Delivery (배송중)
│   │       └── deliveryStatus: SHIPPED
│   └── orderStatus: ACTIVE
│
├── Order 2 (동일 구조)
└── Order 3 (동일 구조)
```

### 주문 취소 - 2개
```
Order (2개)
├── Order 1
│   ├── Customer (랜덤)
│   ├── Payment: 없음
│   ├── OrderItem (1개)
│   │   ├── Product (랜덤)
│   │   ├── Option (랜덤, nullable)
│   │   └── isCancelled: true
│   └── orderStatus: CANCELLED
│
└── Order 2 (동일 구조)
```

### 구매확정 - 4개
```
Order (4개)
├── Order 1
│   ├── Customer (랜덤)
│   ├── Payment (결제 완료)
│   ├── OrderItem (1개)
│   │   ├── Product (랜덤)
│   │   ├── Option (랜덤, nullable)
│   │   ├── confirmedAt: 설정됨
│   │   ├── completedAt: 설정됨 (구매 확정)
│   │   └── Delivery (배송 완료)
│   │       └── deliveryStatus: DELIVERED
│   └── orderStatus: ACTIVE
│
└── ... (Order 2, 3, 4 동일 구조)
```

### 반품완료 - 2개
```
Order (2개)
├── Order 1
│   ├── Customer (랜덤)
│   ├── Payment (결제 완료)
│   ├── OrderItem (1개)
│   │   ├── Product (랜덤)
│   │   ├── Option (랜덤, nullable)
│   │   ├── confirmedAt: 설정됨
│   │   ├── Delivery (배송 완료)
│   │   │   └── deliveryStatus: DELIVERED
│   │   └── Return (반품 완료)
│   │       └── returnStatus: REFUNDED
│   └── orderStatus: ACTIVE
│
└── Order 2 (동일 구조)
```

**관계:**
- `Customer` → `Order` (1:N, FK: customer_id)
- `Order` → `Payment` (1:1, FK: payment_no)
- `Order` → `OrderItem` (1:N, FK: order_no)
- `OrderItem` → `Product` (N:1, FK: product_no)
- `OrderItem` → `Option` (N:1, FK: option_no, nullable)
- `OrderItem` → `Delivery` (1:1, FK: order_item_no)
- `OrderItem` → `Return` (1:1, FK: order_item_no, nullable)

**총계:**
- 주문: 11개 (배송중 3개, 취소 2개, 구매확정 4개, 반품완료 2개)
- 주문 상품: 11개 (주문당 1개씩)
- 결제: 9개 (취소 주문 제외)
- 배송: 9개 (취소 주문 제외)
- 반품: 2개

---

## 전체 관계도

```
Account (13개)
├── Admin (1) ← Account
│   └── Partner (2) ← Account, approvedBy: Admin
│       └── Product (4) ← Partner
│           └── Option (14) ← Product, Partner
│               └── Inventory (14) ← Option
│
├── Customer (10) ← Account
│   └── CustomerGrade (1) ← Customer
│
└── Order (11) ← Customer
    ├── Payment (9) ← Order
    └── OrderItem (11) ← Order
        ├── Product ← OrderItem
        ├── Option ← OrderItem (nullable)
        ├── Delivery (9) ← OrderItem
        └── Return (2) ← OrderItem

Product (옵션 없음)
└── Inventory (1) ← Product
```

---

## 생성 순서 요약

1. **Account 생성** (13개)
   - Admin Account (1)
   - Partner Account (2)
   - Customer Account (10)

2. **Admin & Partner 생성**
   - Admin (1) ← Account
   - Partner (2) ← Account, approvedBy: Admin

3. **CustomerGrade 생성** (없으면 자동 생성)
   - 기본 등급 "일반"

4. **Customer 생성** (10명)
   - Customer (10) ← Account, CustomerGrade

5. **Product & Option 생성**
   - Product (4) ← Partner
   - Option (14) ← Product, Partner

6. **Inventory 생성** (15개)
   - Inventory (14) ← Option
   - Inventory (1) ← Product (옵션 없는 상품)

7. **Order 생성** (11개)
   - Order (11) ← Customer
   - Payment (9) ← Order
   - OrderItem (11) ← Order, Product, Option
   - Delivery (9) ← OrderItem
   - Return (2) ← OrderItem

---

## 주의사항

1. **순서 의존성:**
   - Partner가 있어야 Product 생성 가능
   - Product가 있어야 Option 생성 가능
   - CustomerGrade가 있어야 Customer 생성 가능 (없으면 자동 생성)
   - Product/Option이 있어야 Inventory 생성 가능
   - Customer, Product, Option이 있어야 Order 생성 가능

2. **외래키 제약:**
   - 모든 관계는 FK로 연결되어 있어 순서를 지켜야 함
   - `@Transactional`로 전체가 하나의 트랜잭션으로 처리됨

3. **중복 방지:**
   - 각 단계마다 `count() == 0` 체크로 중복 생성 방지
   - 이미 데이터가 있으면 해당 단계 건너뜀
