# 반품 상태 체크 제약 조건 수정 가이드

## 문제 상황
`PICKUP_COMPLETED` 상태에서 `REJECTED`로 변경할 때 데이터베이스 체크 제약 조건 위반 오류가 발생합니다.

## 원인
데이터베이스에 정의된 체크 제약 조건(`return_return_status_check`)이 이전 상태 전이 규칙에 맞춰져 있어서, 새로운 상태 전이를 허용하지 않습니다.

## 해결 방법

### 방법 1: PostgreSQL에 직접 연결하여 수정 (권장)

```sql
-- 1. 현재 제약 조건 확인
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'return'::regclass
  AND conname = 'return_return_status_check';

-- 2. 기존 제약 조건 삭제
ALTER TABLE "return" DROP CONSTRAINT IF EXISTS return_return_status_check;

-- 3. 새로운 제약 조건 추가 (모든 반품 상태 값 허용)
ALTER TABLE "return" ADD CONSTRAINT return_return_status_check 
    CHECK (return_status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'PICKUP_COMPLETED', 'REFUNDED'));
```

### 방법 2: SQL 스크립트 실행

`backend/src/main/resources/db/migration/fix_return_status_check_constraint.sql` 파일을 실행하세요.

```bash
# PostgreSQL에 연결
psql -U postgres -d swim_mall

# SQL 스크립트 실행
\i backend/src/main/resources/db/migration/fix_return_status_check_constraint.sql
```

### 방법 3: 애플리케이션 재시작 (Hibernate DDL Auto 사용 시)

`spring.jpa.hibernate.ddl-auto=update`로 설정되어 있으면, 애플리케이션을 재시작하면 Hibernate가 제약 조건을 자동으로 업데이트할 수 있습니다. 하지만 기존 제약 조건이 수동으로 생성된 경우 자동 업데이트가 안 될 수 있으므로, 위의 방법 1 또는 2를 권장합니다.

## 확인

제약 조건이 올바르게 수정되었는지 확인:

```sql
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'return'::regclass
  AND conname = 'return_return_status_check';
```

예상 결과:
```
constraint_name              | constraint_definition
----------------------------|--------------------------------------------------
return_return_status_check  | CHECK (return_status = ANY (ARRAY['REQUESTED'::text, 'APPROVED'::text, 'REJECTED'::text, 'PICKUP_COMPLETED'::text, 'REFUNDED'::text]))
```

## 참고

- 제약 조건은 데이터베이스 레벨에서 상태 값의 유효성을 검증합니다.
- 애플리케이션 레벨의 상태 전이 검증(`ReturnService.validateStatusTransition`)과는 별개로 동작합니다.
- 두 가지 검증 모두 통과해야 상태 변경이 성공합니다.
