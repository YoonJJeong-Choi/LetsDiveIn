# PostgreSQL CHECK 제약 수동 수정 (이력)

원문은 `docs/runbooks/DB_FIX_GUIDE.md`, `docs/runbooks/FIX_RETURN_STATUS_CONSTRAINT.md`였다. 두 내용을 합쳐 아카이브로 옮겼다. 스키마·상태 정본은 현재 코드와 `docs/architecture/`, `docs/domains/`를 우선한다.

---

## A. 주문 `orders` — `orders_order_status_check`

### 문제

`orders_order_status_check` 제약이 모든 `OrderStatus` enum 값을 허용하지 않아 발생하는 오류.

현재 코드 기준 `OrderStatus` (`common/enums/OrderStatus.java`) 값:

- `PENDING_PAYMENT` (결제 대기중)
- `PAID` (결제 완료)
- `PAYMENT_FAILED` (결제 실패)
- `ACTIVE` (주문 진행중)
- `CANCELLED` (주문 취소)

구매 확정은 주문 전체 상태 `COMPLETED`가 아니라 `OrderItem.completedAt` 등으로 관리한다. DB CHECK에 `COMPLETED`를 넣지 않는다.

### 해결

#### A-1. create 모드로 테이블 재생성 (로컬·테스트 전용, 데이터 삭제)

1. `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=create
```

2. 백엔드 재시작.

3. 팀 기본값으로 복구. 이 프로젝트는 보통 `validate`:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

#### A-2. PostgreSQL에서 직접 수정 (데이터 보존)

```sql
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_status_check;

ALTER TABLE orders ADD CONSTRAINT orders_order_status_check 
CHECK (order_status IN (
    'PENDING_PAYMENT', 
    'PAID', 
    'PAYMENT_FAILED', 
    'ACTIVE', 
    'CANCELLED'
));
```

#### A-3. 현재 제약 확인

```sql
SELECT c.conname AS constraint_name, pg_get_constraintdef(c.oid) AS check_clause
FROM pg_constraint c
JOIN pg_class t ON c.conrelid = t.oid
WHERE t.relname = 'orders'
  AND c.contype = 'c'
  AND c.conname LIKE '%order_status%';
```

---

## B. 반품 `return` — `return_return_status_check`

### 문제

`PICKUP_COMPLETED`에서 `REJECTED` 등으로 바꿀 때 DB CHECK 위반이 날 수 있다.

### 원인

`return_return_status_check`가 예전 상태 전이에 맞춰져 있어 새 전이를 막는 경우.

### 해결

#### B-1. PostgreSQL에서 직접 수정 (권장)

```sql
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'return'::regclass
  AND conname = 'return_return_status_check';

ALTER TABLE "return" DROP CONSTRAINT IF EXISTS return_return_status_check;

ALTER TABLE "return" ADD CONSTRAINT return_return_status_check 
    CHECK (return_status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'PICKUP_COMPLETED', 'REFUNDED'));
```

#### B-2. SQL 파일 실행 (선택)

과거 런북에는 `backend/src/main/resources/db/migration/fix_return_status_check_constraint.sql` 실행을 안내했다. 레포에 해당 파일이 없으면 B-1 SQL을 사용한다.

#### B-3. Hibernate DDL

`spring.jpa.hibernate.ddl-auto=update`이면 재시작 시 자동 반영될 수 있으나, 수동으로 만든 CHECK는 안 바뀔 수 있어 B-1을 권장한다. (로컬 기본은 `validate`인 경우가 많다.)

### 확인

```sql
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'return'::regclass
  AND conname = 'return_return_status_check';
```

### 참고

- DB CHECK와 애플리케이션 전이 검증(예: `ReturnService.validateStatusTransition`)은 별개로, 둘 다 통과해야 할 수 있다.
