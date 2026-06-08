# Backend 문서 인덱스

이 파일은 `docs/` 아래 Markdown의 목차이다. 동작 규칙·API·Postman 절차·체크리스트 등은 각 링크 문서를 본다.

`docs/` 루트에는 이 `README.md`만 두고, 정본은 `domains/`·`architecture/`·`testing/`, 데이터 참고는 `reference/`, 과거 자료는 `archive/`에 둔다.

## 전체 목록 (경로별)

### `docs/` 루트

| 파일 | 비고 |
|------|------|
| [README.md](README.md) | 이 인덱스(문서 목차) |

### `architecture/`

| 파일 | 비고 |
|------|------|
| [architecture/status/status-model.md](architecture/status/status-model.md) | 주문·배송·반품·결제 연동·enum 기준 문서 |

### `domains/`

| 파일 | 비고 |
|------|------|
| [domains/delivery/delivery.md](domains/delivery/delivery.md) | 배송 |
| [domains/payment/payment.md](domains/payment/payment.md) | 결제 |
| [domains/points/points.md](domains/points/points.md) | 포인트 |
| [domains/qna/qna.md](domains/qna/qna.md) | 1:1 문의 (QnA) — 동작 정본 |
| [domains/qna/qna-development.md](domains/qna/qna-development.md) | QnA 개발 스토리 (바이브 코딩 대표 사례) |
| [domains/review/review-analysis.md](domains/review/review-analysis.md) | 파트너 리뷰 AI 분석 — 동작 정본 |
| [domains/settlement/settlement.md](domains/settlement/settlement.md) | 정산 |

### `testing/`

| 파일 | 비고 |
|------|------|
| [testing/POSTMAN_TEST_GUIDE.md](testing/POSTMAN_TEST_GUIDE.md) | Postman 공통·최소 시나리오·도메인 링크 |
| [testing/POSTMAN_CURRENT_REFERENCE.md](testing/POSTMAN_CURRENT_REFERENCE.md) | 현재 |
| [testing/POSTMAN_LEGACY_REFERENCE.md](testing/POSTMAN_LEGACY_REFERENCE.md) | 기록용 |

### `security/`

| 파일 | 비고 |
|------|------|
| [security/RATE_LIMIT_OPERATION_GUIDE.md](security/RATE_LIMIT_OPERATION_GUIDE.md) | Rate limit·로그인 잠금·가입 제한 운영 정본 |

### `reference/`

| 파일 | 비고 |
|------|------|
| [reference/DUMMY_DATA_RELATIONS.md](reference/DUMMY_DATA_RELATIONS.md) | 더미 데이터·엔티티 관계(로컬/시드 이해용) |
| [reference/SEED_DEMO_SPEC.md](reference/SEED_DEMO_SPEC.md) | 시드·포폴 데모 목표 스펙(이벤트·이미지·체크리스트) |

### `archive/`

| 파일 | 비고 |
|------|------|
| [archive/README.md](archive/README.md) | 아카이브 보관 기준·주의 |
| [archive/POSTGRES_CHECK_CONSTRAINT_FIXES.md](archive/POSTGRES_CHECK_CONSTRAINT_FIXES.md) | 주문·반품 PostgreSQL CHECK 수동 수정(옛 런북 통합) |
| [archive/DELIVERY_IMPLEMENTATION_PLAN.md](archive/DELIVERY_IMPLEMENTATION_PLAN.md) | 배송 구현 계획(이력) |
| [archive/DAY1-엔티티-관계-점검.md](archive/DAY1-엔티티-관계-점검.md) | 초기 엔티티 점검 메모 |
| [archive/PartnerEntity-불필요한-컬럼-검토.md](archive/PartnerEntity-불필요한-컬럼-검토.md) | Partner 엔티티 컬럼 일회성 검토 |
