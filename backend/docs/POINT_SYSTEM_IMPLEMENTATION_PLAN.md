# 포인트 시스템 구현 계획

## 1. 데이터베이스 설계

### 1.1 CustomerEntity 수정
```java
@Column(nullable = false)
@Builder.Default
private Long pointBalance = 0L; // 포인트 잔액
```

### 1.2 PointHistoryEntity 생성
```java
@Entity
@Table(name = "point_history")
public class PointHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType pointType; // ACCUMULATE(적립), USE(사용), MANUAL_ADD(수동 지급), MANUAL_DEDUCT(수동 차감), EXPIRE(만료)
    
    @Column(nullable = false)
    private Long pointAmount; // 포인트 금액 (적립: +, 사용: -)
    
    @Column(nullable = false)
    private Long pointBalanceAfter; // 거래 후 포인트 잔액
    
    @Column(nullable = true)
    private Long orderItemNo; // 관련 주문 상품 (적립 시)
    
    @Column(nullable = true)
    private Long orderNo; // 관련 주문 (사용 시)
    
    @Column(nullable = true, length = 500)
    private String description; // 설명 (예: "주문 완료 적립", "관리자 수동 지급")
    
    @Column(nullable = true)
    private LocalDate expireDate; // 만료일 (적립 시)
    
    @Column(nullable = false)
    private LocalDateTime createdAt; // 생성일시
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = true)
    private AdminEntity admin; // 관리자 (수동 지급/차감 시)
}
```

### 1.3 PointType Enum 생성
```java
public enum PointType {
    ACCUMULATE("적립"),           // 주문 완료 시 자동 적립
    USE("사용"),                  // 주문 결제 시 사용
    MANUAL_ADD("수동 지급"),      // 관리자 수동 지급
    MANUAL_DEDUCT("수동 차감"),   // 관리자 수동 차감
    EXPIRE("만료");               // 포인트 만료
}
```

### 1.4 Flyway Migration
- `V9__add_point_system.sql`
  - `customer` 테이블에 `point_balance` 컬럼 추가
  - `point_history` 테이블 생성

---

## 2. 포인트 적립 로직

### 2.1 적립 시점
- **주문 상품 구매 확정 시** (`OrderItem.completedAt`이 설정될 때)
- 현재는 `CustomerGradeService.updateCustomerGrade()`에서 등급 업데이트만 수행
- 여기에 포인트 적립 로직 추가

### 2.2 적립 계산 방식
```java
// 1. 고객의 현재 등급 조회
CustomerGradeEntity grade = customer.getCustomerGrade();

// 2. 등급별 포인트 적립률 조회
Double pointAccumulationRate = grade.getPointAccumulationRate(); // 예: 1.0% (BEGINNER)

// 3. 주문 상품 금액 기준으로 포인트 계산
Long orderItemAmount = orderItem.getItemTotalPrice(); // 실제 결제 금액
Long pointAmount = (long) (orderItemAmount * pointAccumulationRate / 100.0);

// 4. 포인트 적립 (최소 1원 단위)
if (pointAmount > 0) {
    accumulatePoint(customer, orderItem, pointAmount);
}
```

### 2.3 적립 메서드
```java
@Transactional
public void accumulatePoint(CustomerEntity customer, OrderItemEntity orderItem, Long pointAmount) {
    // 1. 고객 포인트 잔액 증가
    customer.addPointBalance(pointAmount);
    
    // 2. 포인트 내역 기록
    PointHistoryEntity history = PointHistoryEntity.builder()
        .customer(customer)
        .pointType(PointType.ACCUMULATE)
        .pointAmount(pointAmount)
        .pointBalanceAfter(customer.getPointBalance())
        .orderItemNo(orderItem.getOrderItemNo())
        .description(String.format("주문 완료 적립 (등급: %s, 적립률: %.1f%%)", 
            customer.getCustomerGrade().getGradeName().getLabel(),
            customer.getCustomerGrade().getPointAccumulationRate()))
        .expireDate(LocalDate.now().plusYears(1)) // 1년 후 만료
        .createdAt(LocalDateTime.now())
        .build();
    
    pointHistoryRepository.save(history);
    customerRepository.save(customer);
}
```

---

## 3. 포인트 사용 로직

### 3.1 사용 시점
- **주문 생성 시** (`OrderService.createOrder()`)
- 고객이 포인트 사용을 선택하고 사용할 포인트 금액을 입력
- 결제 금액에서 포인트 금액 차감

### 3.2 사용 검증
```java
// 1. 포인트 잔액 확인
if (customer.getPointBalance() < usePointAmount) {
    throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
}

// 2. 최소 사용 금액 확인 (예: 1,000원 이상)
if (usePointAmount < 1000) {
    throw new BusinessException(ErrorCode.INVALID_REQUEST, "포인트는 최소 1,000원 이상 사용 가능합니다.");
}

// 3. 최대 사용 금액 확인 (주문 금액의 50% 이하)
Long maxUsePoint = orderTotalPrice / 2;
if (usePointAmount > maxUsePoint) {
    throw new BusinessException(ErrorCode.INVALID_REQUEST, "포인트는 주문 금액의 50% 이하만 사용 가능합니다.");
}
```

### 3.3 사용 메서드
```java
@Transactional
public void usePoint(CustomerEntity customer, OrderEntity order, Long usePointAmount) {
    // 1. 고객 포인트 잔액 차감
    customer.deductPointBalance(usePointAmount);
    
    // 2. 포인트 내역 기록
    PointHistoryEntity history = PointHistoryEntity.builder()
        .customer(customer)
        .pointType(PointType.USE)
        .pointAmount(-usePointAmount) // 음수로 기록
        .pointBalanceAfter(customer.getPointBalance())
        .orderNo(order.getOrderNo())
        .description(String.format("주문 결제 사용 (주문번호: %d)", order.getOrderNo()))
        .createdAt(LocalDateTime.now())
        .build();
    
    pointHistoryRepository.save(history);
    customerRepository.save(customer);
}
```

### 3.4 OrderCreateRequestDto 수정
```java
private Long usePointAmount = 0L; // 사용할 포인트 금액 (기본값: 0)
```

---

## 4. 포인트 내역 조회

### 4.1 고객용 API
```
GET /api/customer/points/history
- 페이징 지원
- 필터: pointType, startDate, endDate
```

### 4.2 관리자용 API
```
GET /api/admin/customers/{customerId}/points/history
- 고객별 포인트 내역 조회
- 페이징 지원
```

---

## 5. 관리자 포인트 수동 지급/차감

### 5.1 API 엔드포인트
```
POST /api/admin/customers/{customerId}/points/add
Body: {
    "pointAmount": 10000,
    "description": "이벤트 보상"
}

POST /api/admin/customers/{customerId}/points/deduct
Body: {
    "pointAmount": 5000,
    "description": "부정 사용 차감"
}
```

### 5.2 구현
```java
@Transactional
public void addPointManually(Long customerId, Long pointAmount, String description, HttpSession session) {
    authService.requireRole(session, AccountRole.ADMIN);
    AdminEntity admin = getAdminFromSession(session);
    
    CustomerEntity customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    
    customer.addPointBalance(pointAmount);
    
    PointHistoryEntity history = PointHistoryEntity.builder()
        .customer(customer)
        .pointType(PointType.MANUAL_ADD)
        .pointAmount(pointAmount)
        .pointBalanceAfter(customer.getPointBalance())
        .description(description)
        .admin(admin)
        .createdAt(LocalDateTime.now())
        .build();
    
    pointHistoryRepository.save(history);
    customerRepository.save(customer);
}
```

---

## 6. 포인트 만료 관리 (선택사항)

### 6.1 만료 정책
- 포인트 적립 후 1년 후 만료
- 만료일이 지난 포인트는 자동 차감

### 6.2 스케줄러
```java
@Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시 실행
@Transactional
public void expirePoints() {
    LocalDate today = LocalDate.now();
    
    // 만료일이 오늘인 포인트 내역 조회
    List<PointHistoryEntity> expiredHistories = pointHistoryRepository
        .findByPointTypeAndExpireDate(PointType.ACCUMULATE, today);
    
    for (PointHistoryEntity history : expiredHistories) {
        CustomerEntity customer = history.getCustomer();
        Long expireAmount = history.getPointAmount();
        
        // 만료할 포인트가 잔액보다 크면 잔액만큼만 만료
        Long actualExpireAmount = Math.min(expireAmount, customer.getPointBalance());
        
        if (actualExpireAmount > 0) {
            customer.deductPointBalance(actualExpireAmount);
            
            PointHistoryEntity expireHistory = PointHistoryEntity.builder()
                .customer(customer)
                .pointType(PointType.EXPIRE)
                .pointAmount(-actualExpireAmount)
                .pointBalanceAfter(customer.getPointBalance())
                .description(String.format("포인트 만료 (적립일: %s)", history.getCreatedAt().toLocalDate()))
                .createdAt(LocalDateTime.now())
                .build();
            
            pointHistoryRepository.save(expireHistory);
            customerRepository.save(customer);
        }
    }
}
```

---

## 7. 구현 순서

### Phase 1: 기본 구조 (1일)
1. ✅ PointType Enum 생성
2. ✅ PointHistoryEntity 생성
3. ✅ CustomerEntity에 pointBalance 필드 추가
4. ✅ Flyway Migration 생성
5. ✅ Repository 생성

### Phase 2: 포인트 적립 (1일)
1. ✅ PointService 생성
2. ✅ 포인트 적립 로직 구현
3. ✅ OrderService에 포인트 적립 호출 추가
4. ✅ 테스트

### Phase 3: 포인트 사용 (1일)
1. ✅ OrderCreateRequestDto 수정
2. ✅ 포인트 사용 검증 로직
3. ✅ 포인트 사용 처리
4. ✅ 결제 금액 계산 수정
5. ✅ 테스트

### Phase 4: 포인트 내역 조회 (0.5일)
1. ✅ 고객용 포인트 내역 API
2. ✅ 관리자용 포인트 내역 API
3. ✅ DTO 생성

### Phase 5: 관리자 수동 지급/차감 (0.5일)
1. ✅ 관리자 포인트 지급 API
2. ✅ 관리자 포인트 차감 API
3. ✅ 프론트엔드 연동

### Phase 6: 포인트 만료 (선택, 0.5일)
1. ✅ 스케줄러 구현
2. ✅ 만료 로직 테스트

### Phase 7: 프론트엔드 (1일)
1. ✅ 고객 포인트 내역 페이지
2. ✅ 관리자 포인트 관리 페이지
3. ✅ 주문 시 포인트 사용 UI

---

## 8. 주요 고려사항

### 8.1 포인트 적립률
- 등급별로 다른 적립률 적용
- BEGINNER: 1.0%, SWIMMER: 1.5%, PRO: 2.0%, MASTER: 2.5%, LEGEND: 3.0%

### 8.2 포인트 사용 제한
- 최소 사용 금액: 1,000원
- 최대 사용 비율: 주문 금액의 50%
- 포인트 잔액 확인 필수

### 8.3 포인트 만료
- 적립 후 1년 후 만료
- 선입선출(FIFO) 방식 권장 (먼저 적립된 포인트부터 만료)

### 8.4 트랜잭션 관리
- 포인트 적립/사용은 모두 `@Transactional` 필수
- 동시성 문제 방지를 위한 낙관적/비관적 락 고려

### 8.5 에러 처리
- 포인트 부족 시 명확한 에러 메시지
- 포인트 사용 실패 시 롤백 처리

---

## 9. API 명세 요약

### 고객용
- `GET /api/customer/points/balance` - 포인트 잔액 조회
- `GET /api/customer/points/history` - 포인트 내역 조회

### 관리자용
- `GET /api/admin/customers/{customerId}/points/balance` - 고객 포인트 잔액 조회
- `GET /api/admin/customers/{customerId}/points/history` - 고객 포인트 내역 조회
- `POST /api/admin/customers/{customerId}/points/add` - 포인트 수동 지급
- `POST /api/admin/customers/{customerId}/points/deduct` - 포인트 수동 차감

---

## 10. 예상 작업 시간

- **총 예상 시간: 5-6일**
  - 백엔드: 4일
  - 프론트엔드: 1-2일

---

## 11. 다음 단계

포인트 시스템 구현 후:
1. 등급별 할인 적용 (주문 시 등급별 할인율 적용)
2. 쿠폰 시스템 구현
3. 통합 혜택 시스템 (포인트 + 쿠폰 + 등급 할인)
