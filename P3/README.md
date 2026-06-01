# DevLearn P2

2026-1학기 프로젝트 실습 인강사이트 — P1(JWT 인증·강의·진도·리뷰) 위에
**강사 역할 도입**과 **수강 라이프사이클 확장(장바구니→모의 결제→환불·이어듣기·북마크·리뷰 80% 게이트)** 을 추가했다.

자세한 PRD는 [docs/](./docs/), Claude Code 작업 컨텍스트는 [CLAUDE.md](./CLAUDE.md).

## 구성

```
P2/
├── backend/    Spring Boot 3 + JPA + JWT
├── frontend/   React 19 + Vite + Tailwind
└── docs/       PRD 문서 9종
```

## 주요 기능

- **역할 분리**: STUDENT / INSTRUCTOR. JWT에 role 클레임, URL·메서드 레벨 가드, 소유자 검증으로 IDOR 방어
- **강사 콘솔**: 강의 CRUD, 섹션·렉처 관리, 발행/보관/폐강, 대시보드 집계, 공개 프로필
- **수강 플로우**: 장바구니 → 주문 생성 → 모의 결제(`MockPaymentGateway`) → enrollment 자동 생성 → 환불(부분/전체)
- **학습 경험**: `<video>` timeupdate 기반 재생 위치 저장, 이어보기, 렉처별 북마크, 리뷰 80% 진도 게이트(서버 재계산)

## 실행

```bash
# DB
mysql -u root -p -e "CREATE DATABASE devlearn_p2 CHARACTER SET utf8mb4;"

# 백엔드
cd backend
./gradlew bootRun

# 프론트
cd frontend
npm install
npm run dev
```

환경 변수는 [CLAUDE.md](./CLAUDE.md#commands) 참조.

## 진행 현황

전체 11개 Phase 모두 구현 완료 (2026-05 기준). 상세 체크리스트는 [docs/06-implementation-checklist.md](./docs/06-implementation-checklist.md).
