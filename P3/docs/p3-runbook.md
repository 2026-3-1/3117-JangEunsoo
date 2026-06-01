# P3 운영 Runbook (DevLearn)

배포·롤백·장애 대응·모니터링 절차 요약. (배포 인프라는 P2의 `docker-compose.yml` + EC2 기준)

## 1. 환경변수

`.env.example`를 복사해 `.env`로 채운다. P3 신규 항목:

```
# 관리자 부트스트랩 (미설정 시 관리자 계정 자동 생성 안 함)
ADMIN_USERNAME=admin01
ADMIN_PASSWORD=<강력한 비밀번호>

# 알림 (미설정 시 로그 fallback)
NOTIFICATION_CHANNEL=DISCORD
DISCORD_WEBHOOK_URL=<디스코드 채널 webhook URL>
NOTIFICATION_DISPATCH_INTERVAL_MS=15000

# 운영 튜닝
JPA_SHOW_SQL=false
```

## 2. 배포

```bash
# 1) 코드 가져오기
git pull

# 2) 환경변수 확인 (.env)
#    ADMIN_PASSWORD, JWT_SECRET_KEY, DB_PASSWORD, CORS_ALLOWED_ORIGINS 필수

# 3) 빌드 + 기동
docker compose up -d --build

# 4) 헬스 확인
curl -s http://localhost:8080/actuator/health   # {"status":"UP"}
```

신규 테이블(`reports`, `qna_questions`, `qna_answers`, `notification_outbox`)은
`ddl-auto=update`가 자동 생성. 별도 마이그레이션 불필요.

## 3. 헬스체크 / 모니터링

| 항목 | 경로 | 비고 |
|------|------|------|
| 헬스 | `GET /actuator/health` | 공개. liveness/readiness probe |
| 정보 | `GET /actuator/info` | 공개 |
| 메트릭 | `GET /actuator/metrics`, `/actuator/prometheus` | **ADMIN 토큰 필요** |
| 요청 추적 | 응답 헤더 `X-Request-Id` | 로그의 `reqId=`와 매칭 |

로그는 구조화(`%5p [reqId=...]`). 장애 조사 시 `X-Request-Id`로 해당 요청 로그를 추적.

## 4. 롤백

```bash
git checkout <직전 안정 태그/커밋>
docker compose up -d --build
```

DB 스키마는 컬럼/테이블 추가만 했으므로(파괴적 변경 없음) 이전 버전 코드와 호환된다.

## 5. 장애 시나리오

### 5-1. Discord webhook 다운 / URL 오류
- 증상: `NotificationDispatcher` 로그에 발송 실패 누적.
- 동작: 해당 outbox row는 PENDING 유지 + 지수 백오프(1·2·4·8분) 재시도, 5회 초과 시 `FAILED` 종결.
- 비즈니스 영향 **없음**(알림은 본 트랜잭션과 분리). URL 정정 후 PENDING row 자동 재발송.
- 임시 비활성화: `NOTIFICATION_CHANNEL=LOG`로 전환 후 재기동(로그로만 기록).

### 5-2. DB 다운
- 증상: `/actuator/health` `DOWN`, API 5xx.
- 조치: MySQL 컨테이너/인스턴스 복구 → `docker compose restart backend`.

### 5-3. 관리자 계정 분실
- `ADMIN_PASSWORD` 설정 후 재기동하면 동일 username이 없을 때만 새로 생성.
- 기존 계정 비밀번호 재설정은 DB에서 직접(BCrypt 해시) 또는 신규 username으로 생성.

### 5-4. 부적절 콘텐츠/계정 대응
- 강의: 관리자 콘솔 → 강의 탭 → 차단(사유 입력). 즉시 비공개.
- 사용자: 사용자 탭 → 정지. 다음 요청부터 인증 차단.
- 리뷰/Q&A: 신고 탭 → "대상 삭제 후 처리".

## 6. 사고 보고서 템플릿

```
## 사고 요약
- 발생 시각 / 탐지 시각 / 복구 시각:
- 영향 범위(사용자/기능):
- 심각도:

## 타임라인
- HH:MM  ...

## 원인
## 조치 / 복구
## 재발 방지
```
