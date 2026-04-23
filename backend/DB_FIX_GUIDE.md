# PostgreSQL CHECK 제약조건 수정 가이드

## 문제
`orders_order_status_check` 제약조건이 모든 `OrderStatus` enum 값을 허용하지 않아 발생하는 오류입니다.

현재 `OrderStatus` enum 값:
- `PENDING_PAYMENT` (결제 대기중)
- `PAID` (결제 완료)
- `PAYMENT_FAILED` (결제 실패)
- `ACTIVE` (주문 진행중)
- `COMPLETED` (구매 확정)
- `CANCELLED` (주문 취소)

## 해결 방법

### 방법 1: create 모드로 테이블 재생성 (권장 - 테스트 환경)

1. `application.properties`에서 다음으로 변경:
```properties
spring.jpa.hibernate.ddl-auto=create
```

2. 백엔드 서버 재시작 (모든 데이터 삭제됨)

3. 다시 `update` 모드로 변경:
```properties
spring.jpa.hibernate.ddl-auto=update
```

### 방법 2: PostgreSQL에서 직접 제약조건 수정 (데이터 보존)

PostgreSQL에 접속하여 다음 SQL 실행:

```sql
-- 기존 제약조건 삭제
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_status_check;

-- 새로운 제약조건 추가 (모든 OrderStatus 값 포함)
ALTER TABLE orders ADD CONSTRAINT orders_order_status_check 
CHECK (order_status IN (
    'PENDING_PAYMENT', 
    'PAID', 
    'PAYMENT_FAILED', 
    'ACTIVE', 
    'COMPLETED', 
    'CANCELLED'
));
```

### 방법 3: 제약조건 확인 및 수정

```sql
-- 현재 제약조건 확인
SELECT constraint_name, check_clause 
FROM information_schema.check_constraints 
WHERE constraint_name = 'orders_order_status_check';

-- 제약조건 삭제
ALTER TABLE orders DROP CONSTRAINT orders_order_status_check;

-- 새로운 제약조건 추가 (모든 OrderStatus 값 포함)
ALTER TABLE orders ADD CONSTRAINT orders_order_status_check 
CHECK (order_status IN (
    'PENDING_PAYMENT', 
    'PAID', 
    'PAYMENT_FAILED', 
    'ACTIVE', 
    'COMPLETED', 
    'CANCELLED'
));
```

## 확인

제약조건이 올바르게 설정되었는지 확인:

```sql
SELECT constraint_name, check_clause 
FROM information_schema.check_constraints 
WHERE table_name = 'orders' AND constraint_name LIKE '%order_status%';
```
