# DevLearn P2 — 재현용 PRD 인덱스

이 문서 세트는 **AI 에이전트가 코드 없이 zero에서 DevLearn P2 프로젝트를 동일하게 재현할 수 있도록** 작성된 사양서입니다.
한 문서를 읽고 다음 문서를 읽으면, 별도의 추측 없이 동일한 동작/구조의 결과물이 나오도록 모든 식별자(클래스명·필드명·경로)와 값을 명시합니다.

## 읽는 순서

1. [01-project-overview.md](./01-project-overview.md) — 무엇을 만드는지, 왜, 핵심 기능 7가지
2. [02-tech-stack.md](./02-tech-stack.md) — 정확한 라이브러리 버전·디렉토리 구조
3. [03-data-model.md](./03-data-model.md) — ERD·DDL·제약·인덱스
4. [04-api-design.md](./04-api-design.md) — 전체 엔드포인트 표 + 요청/응답 스키마
5. [05-security-design.md](./05-security-design.md) — JWT·역할 인가·소유자 검증·CORS
6. [06-business-rules.md](./06-business-rules.md) — 상태머신·게이트·환불 로직 등 핵심 규칙
7. [07-frontend-spec.md](./07-frontend-spec.md) — 라우팅·페이지·컴포넌트·API 클라이언트
8. [08-sample-data.md](./08-sample-data.md) — seed 계정·카테고리·강의
9. [09-build-deploy.md](./09-build-deploy.md) — Docker·환경변수·로컬 실행 절차
10. [10-implementation-checklist.md](./10-implementation-checklist.md) — Phase별 DoD

## 핵심 원칙 (모든 문서에 공통 적용)

| 원칙 | 내용 |
|------|------|
| **단일 진실원** | 결제 금액·진도율은 **항상 서버 계산**. 클라이언트가 보낸 값은 무시 |
| **존재 은폐** | 권한 없는 리소스 접근 시 **403이 아닌 404** 반환 (IDOR 방지) |
| **3중 인가** | SecurityConfig URL 규칙 + `@PreAuthorize` + Service의 `OwnershipValidator` |
| **하위호환** | P1에서 사용된 경로·필드는 **삭제/변경 금지**. 추가만 |
| **언어** | 커밋 메시지·에러 메시지·UI 카피는 **한국어** |
| **응답 포맷** | 모든 API는 `{success, status, data?, error?}` 형식 (P1 유지) |
| **소프트 삭제** | `courses`는 `deleted_at` 기반 soft delete (Hibernate `@SQLDelete`/`@SQLRestriction`) |

## 프로젝트의 한 줄 요약

> JWT 인증과 강의 시청 기반의 P1 인강 플랫폼에, **강사 역할 + 모의 결제(장바구니→주문→환불) + 이어듣기 + 북마크 + 80% 진도 리뷰 게이트**를 추가한 학생/강사 공존형 LMS의 학습용 풀스택 구현체.
