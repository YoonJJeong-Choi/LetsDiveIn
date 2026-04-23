# 정산 기능 분석 및 권장사항

## 1. 취소 기능 확인 결과

### 현재 문제점 발견 ⚠️

**취소된 정산의 주문 아이템이 정산 대상으로 복구되지 않습니다.**

현재 정산 대상 조회 쿼리:
```sql
AND NOT EXISTS (SELECT 1 FROM SettlementOrderItemEntity soi 
                WHERE soi.orderItem.orderItemNo = oi.orderItemNo)
```

이 쿼리는 정산 상태와 관계없이 `SettlementOrderItemEntity`에 연결된 모든 주문 아이템을 제외합니다.
- ✅ PENDING 정산의 주문 아이템 제외 (정상)
- ✅ COMPLETED 정산의 주문 아이템 제외 (정상)
- ❌ **CANCELLED 정산의 주문 아이템도 제외 (문제!)**

### 해결 방법

정산 대상 조회 쿼리를 수정하여 CANCELLED 상태인 정산의 주문 아이템은 다시 정산 대상에 포함되도록 해야 합니다.

```sql
AND NOT EXISTS (
    SELECT 1 FROM SettlementOrderItemEntity soi 
    JOIN SettlementEntity s ON soi.settlement.settlementId = s.settlementId
    WHERE soi.orderItem.orderItemNo = oi.orderItemNo 
    AND s.settlementStatus != 'CANCELLED'
)
```

또는 CANCELLED 정산의 `SettlementOrderItemEntity`를 삭제하는 방법도 있습니다.

---

## 2. 알림 기능 - 한번에 만들지 말지?

### 권장: **나중에 한번에 만들기** ✅

**이유:**
1. **알림 시스템은 독립적인 기능**
   - 정산 알림뿐만 아니라 주문, 배송, 반품 등 다양한 알림이 필요
   - 알림 시스템을 먼저 구축한 후 각 기능에 통합하는 것이 효율적

2. **구현 복잡도**
   - 알림 시스템: 이메일, SMS, 푸시 알림, 인앱 알림 등
   - 알림 템플릿 관리
   - 알림 설정 (수신 여부, 알림 타입별 설정)
   - 알림 이력 관리

3. **현재 우선순위**
   - 정산 기능 자체가 완료되었으므로, 다른 핵심 기능을 먼저 구현하는 것이 좋음
   - 알림은 "있으면 좋은" 기능이지 "필수" 기능은 아님

### 알림 기능 구현 시 포함할 항목
- 정산 완료 알림 (파트너에게)
- 정산 취소 알림 (파트너에게)
- 주문 알림 (고객, 파트너)
- 배송 알림 (고객)
- 반품 알림 (고객, 파트너)
- 입점 신청 승인/반려 알림 (파트너)
- 상품 승인 알림 (파트너)

**결론: 알림 기능은 별도 프로젝트로 나중에 한번에 구현하는 것을 권장합니다.**

---

## 3. 정산 이력 관리란?

### 정의
정산의 상태 변경, 금액 수정, 지급일 변경 등 모든 변경 사항을 기록하는 기능입니다.

### 현재 상태
- ❌ **이력 관리 없음**
- ✅ 상태 변경만 가능 (PENDING → COMPLETED → CANCELLED)
- ❌ 누가 변경했는지 기록 안됨
- ❌ 언제 변경했는지 상세 기록 안됨
- ❌ 무엇을 변경했는지 기록 안됨

### 필요한 정보 예시
```
정산 ID: 123
변경 이력:
- 2024-01-15 10:00 | 관리자 A | 생성 | 상태: PENDING
- 2024-01-20 14:30 | 관리자 B | 상태 변경 | PENDING → COMPLETED, 지급일: 2024-01-20
- 2024-01-21 09:15 | 관리자 B | 지급일 수정 | 2024-01-20 → 2024-01-21
- 2024-01-22 11:00 | 관리자 A | 상태 변경 | COMPLETED → CANCELLED
```

### 구현 방법

#### 옵션 1: 별도 이력 테이블 생성 (권장)
```java
@Entity
@Table(name = "settlement_history")
public class SettlementHistoryEntity {
    private Long historyId;
    private Long settlementId;
    private Long adminId;  // 변경한 관리자
    private LocalDateTime changedAt;
    private String actionType;  // "CREATE", "STATUS_CHANGE", "PAID_DATE_UPDATE"
    private String oldValue;  // JSON 형태로 저장
    private String newValue;  // JSON 형태로 저장
    private String reason;  // 변경 사유
}
```

#### 옵션 2: 간단한 변경 로그 (현재 상태에서도 가능)
- `SettlementEntity`에 `lastModifiedBy`, `lastModifiedAt` 필드 추가
- 하지만 상세 이력은 기록되지 않음

### 필요성 평가

**필수도: 중간**
- 작은 규모의 쇼핑몰: 선택사항
- 큰 규모의 쇼핑몰: 권장 (감사, 분쟁 해결, 추적)

**구현 시기:**
- 현재는 선택사항
- 나중에 필요하다고 판단되면 추가 가능

---

## 최종 권장사항

### 지금 해야 할 것 ✅
1. **취소 기능 수정** (필수)
   - CANCELLED 정산의 주문 아이템이 정산 대상으로 복구되도록 수정

### 나중에 해야 할 것 📅
2. **알림 기능** (선택)
   - 다른 알림 기능들과 함께 한번에 구현
   - 알림 시스템 구축 후 각 기능에 통합

3. **정산 이력 관리** (선택)
   - 필요하다고 판단되면 추가
   - 감사, 분쟁 해결이 중요한 경우 구현 권장

### 결론
- 취소 기능 수정은 **지금 해야 함** (버그 수정)
- 알림 기능과 이력 관리는 **나중에 해도 됨** (선택사항)
