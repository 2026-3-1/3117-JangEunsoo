# 09. 보안 설계 — DevLearn P2

P1의 "보안 감사(audit)"와 달리, P2는 역할 기반 접근제어를 **설계 단계**에서 위협 모델링·권한 매트릭스·OWASP 체크리스트로 사전에 못 박는다.

---

## 1. STRIDE 간이 분석

| 위협 | P2에서의 구체 예시 | 대응 |
|------|------------------|------|
| **S** (Spoofing) | 타인의 JWT 탈취 후 가장 | 짧은 Access 만료 + Refresh rotation(P1 구현 유지) + HTTPS 전제 |
| **T** (Tampering) | JWT payload의 `sub`/role 조작 | HS256 서명 검증(P1 구현 유지). 단 **role은 DB에서 재조회하여 권위 있는 값 사용** (아래 §4-2 참조) |
| **R** (Repudiation) | 강사가 "난 그 강의 안 만들었다" 부인 | `courses.instructor_id` FK + `created_at`. 로그 레벨 INFO 이상에서 생성/수정 주체 기록 |
| **I** (Info Disclosure) | 타 강사의 DRAFT 강의 유출 | 학생 API에서 `publish_status = PUBLISHED`만 반환. 강사 API에서도 소유자 검증 |
| **D** (DoS) | 강의 목록에 대량 요청 | P2 범위 밖 (P3 레이트리밋). 단 페이지네이션 기본 size=12 유지 |
| **E** (Elevation) | STUDENT가 강사 API 호출하여 강의 생성 | Spring Security `@PreAuthorize("hasRole('INSTRUCTOR')")` + 필터 레벨 방어 |

---

## 2. OWASP Top 10 — P2 관련 핵심

### A01: Broken Access Control (⭐ 가장 중요)

P2에서 **가장 집중적으로 방어**해야 할 항목.

**위협 시나리오 & 대응:**

| 시나리오 | 방어 |
|---------|------|
| STUDENT가 `POST /api/instructor/courses` 호출 | `@PreAuthorize("hasRole('INSTRUCTOR')")` — Spring Security가 403 |
| 강사 A가 **강사 B의 강의를 수정** (IDOR) | 서비스 레이어에서 `course.isOwnedBy(principal.getUserId())` 소유자 검증 → `FORBIDDEN` |
| 강사가 자기 대시보드 `GET /api/instructor/dashboard`에서 쿼리 파라미터로 `?instructorId=999` 주입하여 타인 통계 조회 | 대시보드는 **쿼리 파라미터로 ID 받지 않음**. 항상 `@AuthenticationPrincipal`의 `userId` 사용 |
| 강사가 자기 강의에 엮이지 않은 `sectionId`로 렉처 추가 | 섹션 → 강의 역참조 후 소유자 검증 (아래 §6 IDOR 패턴) |
| 학생이 직접 `PUT /api/courses/{id}` 호출 (P1 엔드포인트) | **P1 공개 엔드포인트는 P2에서 제거 or 내부 변경**. 모든 쓰기는 `/api/instructor/**`로 이동 |

### A02: Cryptographic Failures

| 항목 | 현재 상태 | P2 조치 |
|------|---------|--------|
| 비밀번호 해싱 | `BCryptPasswordEncoder` (P1) | 유지. 체크: **BCrypt strength 기본 10** 이상 확인 |
| JWT 서명 | HS256, secret은 `${JWT_SECRET_KEY}` env | 유지. secret 길이 256-bit 이상 권고 (JJWT가 내부 검증) |
| Refresh Token 저장 | DB `refresh_tokens.token` 평문 | **⚠ P2 개선 권고**: DB 컬럼에 해시로 저장(토큰 유출 시 재사용 방지). 구현 여부는 checklist Phase 2에서 판단 |
| HTTPS | 개발 환경 HTTP | P2 범위 밖 (P3 배포 시) |

### A03: Injection

| 항목 | 대응 |
|------|------|
| SQL Injection | JPA + PreparedStatement 사용 → 기본 방어. **네이티브 쿼리 도입 시 파라미터 바인딩 필수** |
| XSS | 강의 description·review comment에 사용자 입력. React는 기본 이스케이프. **단 HTML 입력 허용 금지** (마크다운 렌더러 도입 시 DOMPurify 필수) |

### A07: Identification and Authentication Failures

| 항목 | 대응 |
|------|------|
| 무차별 로그인 시도 | P2 범위 밖 (P3 레이트리밋/캡차) |
| Refresh Token 재사용 | **Rotation 이미 구현** (P1 `AuthService.refresh` → `saveOrUpdate`로 새 토큰 저장). 단 탈취된 구 토큰으로도 한 번은 갱신 가능한 약점은 남아 있음 → 검증·차단 로직은 P3 과제로 명시 |
| 토큰 만료 | Access 짧게(P1 `TokenConstants` 값 준수), Refresh 길게 |

### 기타 (P3로 미룸)

- A04 Insecure Design — P3 스레싱 모델
- A05 Security Misconfiguration — 배포 단계
- A06 Vulnerable Components — `gradle dependencies --scan` P3
- A08 Software & Data Integrity
- A09 Security Logging & Monitoring
- A10 SSRF — 외부 API 연동(P3)

---

## 3. 권한 매트릭스

각 리소스 × 역할의 허용 동작. **익명(Anonymous)** 은 토큰 없음, **STUDENT/INSTRUCTOR**는 토큰 있음.

| 리소스 / 액션 | Anon | STUDENT | INSTRUCTOR (소유자) | INSTRUCTOR (타인) |
|--------------|:----:|:-------:|:-------------------:|:-----------------:|
| `GET /api/courses` (발행된 강의만) | ✅ | ✅ | ✅ | ✅ |
| `GET /api/courses/{id}` (PUBLISHED) | ✅ | ✅ | ✅ | ✅ |
| `GET /api/courses/{id}` (DRAFT/ARCHIVED) | ❌ | ❌ | ✅ | ❌ |
| `POST /api/instructor/courses` | ❌ | ❌ | ✅ | ✅ (자기 것 생성) |
| `PUT /api/instructor/courses/{id}` | ❌ | ❌ | ✅ | ❌ |
| `DELETE /api/instructor/courses/{id}` | ❌ | ❌ | ✅ | ❌ |
| `POST /api/instructor/courses/{id}/publish` | ❌ | ❌ | ✅ | ❌ |
| `POST /api/instructor/courses/{cid}/sections` | ❌ | ❌ | ✅ | ❌ |
| `POST /api/instructor/sections/{sid}/lectures` | ❌ | ❌ | ✅ | ❌ |
| `GET /api/instructor/courses/{id}/students` | ❌ | ❌ | ✅ | ❌ |
| `GET /api/instructor/dashboard` | ❌ | ❌ | ✅ (자기 것) | ✅ (자기 것) |
| `GET /api/instructors/{userId}` (공개 프로필) | ✅ | ✅ | ✅ | ✅ |
| `PUT /api/instructor/profile` | ❌ | ❌ | ✅ (자기 것) | ✅ (자기 것) |
| `POST /api/enrollments` | ❌ | ✅ | ✅ (자기 수강) | ✅ (자기 수강) |
| `GET /api/enrollments/me` | ❌ | ✅ | ✅ | ✅ |
| `POST /api/reviews` | ❌ | ✅ | ✅ | ✅ |
| `DELETE /api/reviews/{id}` | ❌ | ✅ (작성자) | ✅ (작성자) | ❌ |
| `POST /api/auth/signup` | ✅ | — | — | — |
| `POST /api/auth/login` | ✅ | — | — | — |
| `POST /api/auth/logout` | ❌ | ✅ | ✅ | ✅ |

**규칙:**
- 로그인은 role과 무관하게 요구 (`/api/enrollments`, `/api/reviews` 등). 강사도 학생 체험을 위해 수강 가능.
- "타인의 DRAFT 강의"는 **존재 자체를 은폐** (404 반환, 403 아님 — info leak 방지).

---

## 4. 인가(Authorization) 구현 전략

### 4-1. 2-레이어 방어

```
┌─────────────────────────────────────────────┐
│  Layer 1: URL/메서드 기반                   │
│    Spring Security `@PreAuthorize`          │
│    → "이 엔드포인트는 INSTRUCTOR 전용"      │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│  Layer 2: 리소스 소유자 검증                │
│    Service layer `course.isOwnedBy(userId)` │
│    → "이 강의가 '너'의 강의인가"            │
└─────────────────────────────────────────────┘
```

**두 층 모두 통과해야 요청 승인.** 한 층만으로는 부족:
- Layer 1만 → 강사 B가 강사 A의 강의 수정 가능 (IDOR)
- Layer 2만 → STUDENT도 이론상 API 호출 가능(반환은 403이지만 공격 면적 증가)

### 4-2. JWT의 role은 "힌트", 권위는 DB

JWT payload에 `role` 클레임을 넣더라도, **Spring Security 인증 시점에 `CustomUserDetailsService`가 DB에서 role을 다시 읽어 `UserPrincipal`에 세팅**한다. 이는 탈취·조작된 토큰에서 role만 바꾼 공격을 무력화한다.

```java
// CustomUserDetailsService.java (수정 포인트)
public UserPrincipal loadUserById(String userId) {
    User user = userRepository.findById(Long.parseLong(userId))
        .orElseThrow(...);
    return new UserPrincipal(user);   // user.role이 DB 값
}
```

```java
// UserPrincipal.java (수정 포인트)
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
}
```

**JWT에 role 클레임을 넣을지 여부**: 성능(매 요청 DB 조회 회피)을 위해 넣을 수 있으나 **P2에선 넣지 않는다**. 매 요청 `loadUserById` 쿼리 1회 허용. 단순성 우선.

### 4-3. `@PreAuthorize` 적용 규칙

- 강사 전용 컨트롤러 → **클래스 레벨**에 `@PreAuthorize("hasRole('INSTRUCTOR')")` 부착 (전체 메서드 커버)
- 공개 조회 엔드포인트 → SecurityConfig의 `permitAll` 그대로
- 학생/강사 공통(수강, 리뷰) → 메서드 내부 `isAuthenticated()` 조건만 (SecurityConfig `anyRequest().authenticated()`로 이미 커버)

---

## 5. 주요 수정 지점 (보안 관점)

| 파일 | 변경 내용 |
|------|---------|
| `global/security/UserPrincipal.java` | `getAuthorities()`가 `ROLE_STUDENT`/`ROLE_INSTRUCTOR` 반환 |
| `global/security/SecurityConfig.java` | `/api/instructor/**` 경로는 `authenticated()` + `@PreAuthorize` 이중 방어. 강의 쓰기 엔드포인트(`POST/PUT/DELETE /api/courses/**`) **제거 또는 내부 엔드포인트로 이동** |
| `domain/user/entity/User.java` | `role` enum 필드 |
| `domain/auth/dto/request/SignupRequestDTO.java` | `role` 선택 필드 (`STUDENT` default) |
| `domain/auth/service/AuthService.java` | signup 시 role 저장, 강사면 `InstructorProfile` 함께 생성 |
| `domain/course/service/CourseService.java` | 모든 수정/삭제 메서드에서 `course.isOwnedBy(userId)` 체크 |
| 신규: `domain/instructor/**` | 강사 전용 컨트롤러·서비스·DTO |

---

## 6. IDOR 방지 패턴 (표준 코드)

모든 "자식 리소스"(섹션, 렉처, 수강생 목록 등)는 **부모 강의의 소유자 확인**을 거쳐야 한다. 중복을 줄이기 위해 서비스 레이어에 표준 헬퍼를 둔다.

```java
// domain/instructor/service/OwnershipValidator.java
@Component
@RequiredArgsConstructor
public class OwnershipValidator {
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;

    public Course requireOwnedCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!course.isOwnedBy(userId)) {
            // 404로 은폐 (타 강사의 DRAFT 존재 노출 방지)
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    public Section requireOwnedSection(Long sectionId, Long userId) {
        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new CustomException(CourseErrorCode.SECTION_NOT_FOUND));
        requireOwnedCourse(section.getCourseId(), userId);  // 역참조
        return section;
    }
}
```

사용 예:

```java
// InstructorCourseService.java
public void publishCourse(Long courseId, Long userId) {
    Course course = ownershipValidator.requireOwnedCourse(courseId, userId);
    course.publish();
}

public SectionResponseDTO addSection(Long courseId, Long userId, SectionCreateDTO dto) {
    Course course = ownershipValidator.requireOwnedCourse(courseId, userId);
    Section section = new Section(course.getId(), dto.title(), dto.orderNum());
    return SectionResponseDTO.from(sectionRepository.save(section));
}
```

**핵심 원칙:** 강사 API의 모든 쓰기 경로는 소유자 검증 **없이는 단 한 줄도 쓰지 않는다.** 서비스 메서드 첫 줄이 `requireOwnedCourse`/`requireOwnedSection`이다.

---

## 7. 에러 코드 정책 (보안 관련)

| 코드 | HTTP | 사용 상황 | 비고 |
|------|------|----------|------|
| `UNAUTHORIZED` | 401 | 토큰 없음/무효 | Spring Security 기본 |
| `FORBIDDEN` | 403 | role 부족 (STUDENT가 강사 API 호출) | `@PreAuthorize` 실패 |
| `COURSE_NOT_FOUND` | 404 | 소유자 검증 실패 포함 | **타인 DRAFT 존재 은폐 목적** |
| `INVALID_TOKEN` | 401 | JWT 서명/만료 | P1 `AuthErrorCode` 유지 |
| `DUPLICATE_USERNAME` | 409 | 가입 시 중복 | P1 유지 |

**중요:** 소유자 검증 실패는 403이 아니라 **404**로 반환. 이유: "그 ID의 강의가 존재하는지" 자체가 민감 정보가 될 수 있음 (타 강사의 DRAFT 염탐 방지).

---

## 8. 보안 체크리스트 (P2 완료 전 확인)

`- [ ]` 형식으로 Phase별 DoD의 일부로 활용.

### 설계 단계
- [ ] 모든 강사 API가 권한 매트릭스(§3)와 1:1 매칭되는가
- [ ] 소유자 검증이 필요한 모든 쓰기 경로가 `OwnershipValidator` 경유하는가
- [ ] 민감 엔드포인트에서 ID를 URL 경로로만 받고 쿼리/body의 ID를 신뢰하지 않는가

### 구현 단계
- [ ] `UserPrincipal.getAuthorities()`가 role 기반 authority 반환하는가
- [ ] 강사 전용 컨트롤러 **클래스에 `@PreAuthorize("hasRole('INSTRUCTOR')")`** 부착되었는가
- [ ] `SecurityConfig`의 기존 `POST/PUT/DELETE /api/courses/**` 공개 엔드포인트가 **제거되거나 `/api/instructor/**`로 이동**했는가
- [ ] 학생용 강의 목록에 `publish_status = PUBLISHED` 필터가 들어갔는가
- [ ] 강의 상세 조회에서 DRAFT/ARCHIVED는 소유자 아니면 404 반환하는가

### 테스트 단계 (Phase 8)
- [ ] `STUDENT가 POST /api/instructor/courses 호출 → 403` 테스트 통과
- [ ] `INSTRUCTOR A가 PUT /api/instructor/courses/{B의 강의} → 404` 테스트 통과
- [ ] 유효하지 않은 JWT로 `/api/instructor/dashboard` → 401 테스트 통과
- [ ] Signup 시 role=`ADMIN` 같은 허용 외 값 주입 → 400 / default로 강제 전환 테스트 통과
- [ ] JWT payload 수동 변조 후 호출 → 서명 실패로 401 테스트 통과

---

## 9. P3로 미루는 보안 항목 (명시적)

아래는 P2 범위 밖. 완료 조건에 포함하지 않는다.

- Refresh Token DB 해시 저장
- Refresh Token 재사용 탐지/차단
- 로그인 Rate Limit, Captcha
- 감사 로그(Audit Log) 테이블
- HTTPS 강제, HSTS
- 의존성 취약점 스캔(CI)
- OWASP ZAP 자동화 스캔
- 내용 보안 정책(CSP) 헤더
