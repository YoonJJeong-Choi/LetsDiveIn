# PartnerEntity 불필요한 컬럼 검토 결과

## 검토 일자
2024년

## 검토 대상
`PartnerEntity`의 모든 컬럼

## 검토 결과

### 1. `password` 필드 (불필요)

위치: `PartnerEntity.java:43`

현재 상태:
```java
@Column(nullable = true)
private String password; // 암호화된 비밀번호 (신청 시 입력, 승인 시 Account 생성, 부트스트랩 시는 null)
```

불필요한 이유:
1. 파트너 신청 시에는 password가 필요 없습니다. (`createApplication` 메서드에서 null로 설정)
2. 파트너 승인 시에는 `AccountEntity`가 생성되며, 비밀번호는 `AccountEntity.password`에 저장됩니다.
3. 실제 코드에서 `PartnerEntity.password`를 읽거나 쓰는 곳이 없습니다.
4. 비밀번호는 `AccountEntity`에만 저장하는 것이 올바른 설계입니다.

권장 사항:
- `password` 필드를 제거하거나, 향후 사용 계획이 없다면 제거 권장
- 제거 시 `create` 및 `createApplication` 메서드에서도 해당 파라미터 제거 필요

---

### 2. `deactivationApprovedAt` 필드 (불필요)

위치: `PartnerEntity.java:66`

현재 상태:
```java
@Column(nullable = true)
private LocalDateTime deactivationApprovedAt; // 휴업 신청 승인일시
```

불필요한 이유:
1. 휴업 신청 승인 시 `PartnerHistoryEntity`에 `DEACTIVATION_APPROVED` 액션 타입으로 이력이 기록됩니다.
2. 이력 테이블에서 승인일시를 조회할 수 있으므로 중복입니다.
3. 실제 코드에서 이 필드를 읽거나 쓰는 곳이 없습니다.
4. 상태 변경 정보는 `PartnerHistory`에 기록하는 것이 일관성 있는 설계입니다.

권장 사항:
- `deactivationApprovedAt` 필드 제거 권장
- 승인일시가 필요하면 `PartnerHistory`에서 조회

---

### 3. `reactivationApprovedAt` 필드 (불필요)

위치: `PartnerEntity.java:81`

현재 상태:
```java
@Column(nullable = true)
private LocalDateTime reactivationApprovedAt; // 재활성화 신청 승인일시
```

불필요한 이유:
1. 재활성화 신청 승인 시 `PartnerHistoryEntity`에 `REACTIVATION_APPROVED` 액션 타입으로 이력이 기록됩니다.
2. 이력 테이블에서 승인일시를 조회할 수 있으므로 중복입니다.
3. 실제 코드에서 이 필드를 읽거나 쓰는 곳이 없습니다.
4. 상태 변경 정보는 `PartnerHistory`에 기록하는 것이 일관성 있는 설계입니다.

권장 사항:
- `reactivationApprovedAt` 필드 제거 권장
- 승인일시가 필요하면 `PartnerHistory`에서 조회

---

## 요약

| 필드명 | 불필요 여부 | 제거 권장 | 이유 |
|--------|------------|----------|------|
| `password` | ✅ 불필요 | ✅ 권장 | AccountEntity에만 저장하는 것이 올바른 설계 |
| `deactivationApprovedAt` | ✅ 불필요 | ✅ 권장 | PartnerHistory에 중복 기록됨 |
| `reactivationApprovedAt` | ✅ 불필요 | ✅ 권장 | PartnerHistory에 중복 기록됨 |

## 제거 시 주의사항

1. 데이터베이스 마이그레이션
   - 기존 데이터가 있다면 마이그레이션 스크립트 작성 필요
   - `ALTER TABLE partner DROP COLUMN password;`
   - `ALTER TABLE partner DROP COLUMN deactivation_approved_at;`
   - `ALTER TABLE partner DROP COLUMN reactivation_approved_at;`

2. 코드 수정
   - `PartnerEntity.create()` 메서드에서 해당 파라미터 제거
   - `PartnerEntity.createApplication()` 메서드에서 해당 파라미터 제거
   - 생성자에서도 해당 필드 제거

3. 테스트
   - 제거 후 모든 테스트 케이스 통과 확인
   - 데이터베이스 스키마 변경 확인

## 결론

위 3개 필드는 모두 제거해도 기능에 영향이 없으며, 코드와 데이터베이스 구조를 더 깔끔하게 만들 수 있습니다.
