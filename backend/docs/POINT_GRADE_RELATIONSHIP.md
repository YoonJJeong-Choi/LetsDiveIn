# 포인트 시스템과 등급 시스템의 연관성

## 1. 핵심 연관성

### 1.1 등급별 포인트 적립률
포인트 적립률은 **등급별로 다르게 설정**되어 있습니다.

| 등급 | 포인트 적립률 | 예시 (10만원 구매 시) |
|------|--------------|---------------------|
| BEGINNER | 1.0% | 1,000원 적립 |
| SWIMMER | 1.5% | 1,500원 적립 |
| PRO | 2.0% | 2,000원 적립 |
| MASTER | 2.5% | 2,500원 적립 |
| LEGEND | 3.0% | 3,000원 적립 |

**등급이 높을수록 더 많은 포인트를 적립**할 수 있어, 등급 상승의 동기 부여가 됩니다.

---

## 2. 데이터 구조 연관성

### 2.1 CustomerGradeEntity
```java
@Column(nullable = false)
@Builder.Default
private Double pointAccumulationRate = 0.0; // 포인트 적립률 (%)

@Column(nullable = false)
@Builder.Default
private Double discountRate = 0.0; // 등급별 할인율 (%)
```

등급 엔티티에 **포인트 적립률이 필드로 정의**되어 있습니다.

### 2.2 CustomerEntity
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "grade_id", nullable = true)
private CustomerGradeEntity customerGrade; // 고객 등급

@Column(nullable = false)
@Builder.Default
private Long pointBalance = 0L; // 포인트 잔액
```

고객은 **등급을 가지고 있고**, 그 등급에 따라 포인트 적립률이 결정됩니다.

---

## 3. 비즈니스 로직 연관성

### 3.1 포인트 적립 시 등급 확인
```java
// 포인트 적립 시 반드시 고객의 현재 등급을 확인
CustomerGradeEntity grade = customer.getCustomerGrade();

if (grade == null) {
    // 등급이 없으면 포인트 적립 불가 (또는 기본 적립률 적용)
    throw new BusinessException(ErrorCode.GRADE_NOT_FOUND);
}

// 등급별 포인트 적립률 사용
Double pointAccumulationRate = grade.getPointAccumulationRate();
Long pointAmount = (long) (orderItemAmount * pointAccumulationRate / 100.0);
```

### 3.2 등급 업데이트와 포인트 적립의 시점
**같은 시점에 발생**: 주문 상품 구매 확정 시 (`OrderItem.completedAt`)

```java
// OrderService에서 주문 상품 완료 처리 시
@Transactional
public void completeOrderItem(Long orderItemNo) {
    OrderItemEntity orderItem = orderItemRepository.findById(orderItemNo)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    
    // 1. 구매 확정 처리
    orderItem.setCompletedAt(LocalDateTime.now());
    orderItemRepository.save(orderItem);
    
    // 2. 등급 업데이트 (누적 구매액/주문 건수 재계산)
    customerGradeService.updateCustomerGrade(customerId);
    
    // 3. 포인트 적립 (등급별 적립률 적용)
    pointService.accumulatePoint(customer, orderItem);
}
```

**주의**: 등급 업데이트가 먼저 발생해야 합니다!
- 등급이 변경되면 → 새로운 등급의 적립률로 포인트 적립
- 등급 업데이트 전에 포인트 적립하면 → 이전 등급의 적립률로 적립됨

---

## 4. 등급 상승의 이점

### 4.1 포인트 적립률 증가
```
BEGINNER (1.0%) → SWIMMER (1.5%) → PRO (2.0%) → MASTER (2.5%) → LEGEND (3.0%)
```

등급이 올라갈수록 **같은 금액을 구매해도 더 많은 포인트를 적립**할 수 있습니다.

### 4.2 예시 시나리오
```
고객 A: BEGINNER 등급
- 100만원 구매 → 10,000원 포인트 적립

고객 B: LEGEND 등급  
- 100만원 구매 → 30,000원 포인트 적립

→ 등급 차이로 3배의 포인트 적립!
```

---

## 5. 구현 시 주의사항

### 5.1 등급 업데이트 순서
```java
// ✅ 올바른 순서
1. 등급 업데이트 (누적 구매액/주문 건수 재계산)
2. 포인트 적립 (새로운 등급의 적립률 적용)

// ❌ 잘못된 순서
1. 포인트 적립 (이전 등급의 적립률 적용)
2. 등급 업데이트
```

### 5.2 등급이 없는 경우
```java
// 등급이 없으면 포인트 적립 불가 또는 기본 적립률 적용
if (customer.getCustomerGrade() == null) {
    // 옵션 1: 포인트 적립 안 함
    log.warn("등급이 없어 포인트 적립을 건너뜁니다. 고객 ID: {}", customerId);
    return;
    
    // 옵션 2: 기본 적립률(1.0%) 적용
    pointAccumulationRate = 1.0;
}
```

### 5.3 등급 변경 시 기존 포인트
- **기존 포인트는 유지**됩니다.
- 등급이 변경되어도 이미 적립된 포인트는 그대로 유지됩니다.
- **앞으로 적립되는 포인트만** 새로운 등급의 적립률이 적용됩니다.

---

## 6. 통합 흐름도

```
주문 상품 구매 확정
    ↓
[1] 등급 업데이트
    - 누적 구매액 재계산
    - 주문 건수 재계산
    - 조건에 맞는 등급 할당
    ↓
[2] 포인트 적립
    - 현재 등급 조회
    - 등급별 포인트 적립률 확인
    - 주문 금액 × 적립률 = 적립 포인트
    - 포인트 잔액 증가
    - 포인트 내역 기록
    ↓
완료
```

---

## 7. 등급별 혜택 통합

등급 시스템은 **두 가지 혜택**을 제공합니다:

1. **할인율** (`discountRate`)
   - 주문 시 등급별 할인 적용 (아직 미구현)
   - 예: LEGEND 등급 → 10% 할인

2. **포인트 적립률** (`pointAccumulationRate`)
   - 구매 확정 시 포인트 적립 (구현 예정)
   - 예: LEGEND 등급 → 3.0% 적립

---

## 8. 결론

포인트 시스템과 등급 시스템은 **불가분의 관계**입니다:

✅ **등급별 포인트 적립률이 다름**
✅ **등급이 높을수록 더 많은 포인트 적립**
✅ **등급 업데이트와 포인트 적립이 같은 시점에 발생**
✅ **등급별 혜택의 핵심 요소**

따라서 포인트 시스템 구현 시:
1. 등급 엔티티의 `pointAccumulationRate` 필드를 활용
2. 등급 업데이트 후 포인트 적립 처리
3. 등급별로 다른 적립률 적용

이렇게 구현하면 등급 시스템과 완벽하게 연동됩니다.
