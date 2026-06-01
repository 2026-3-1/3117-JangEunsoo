# 05. 보안 설계 — DevLearn P2

## 1. 위협 모델 요약 (STRIDE)

| 위협 | 대표 시나리오 | 방어 |
|------|---------------|------|
| Spoofing | 다른 사용자 행세 | JWT 서명 검증 (`HS256`, ≥256bit 시크릿) |
| Tampering | 결제 금액·진도율 조작 | 클라이언트 값 무시 → 서버 재계산 |
| Repudiation | 결제 부인 | `orders`/`payments`/`refunds` 영구 보존 |
| Info Disclosure | DRAFT 강의 노출, 타인 주문 조회 | `publish_status=PUBLISHED` 필터 + IDOR 시 `404` 반환 |
| Denial of Service | 무한 진도/북마크 폭주 | 학습용 범위 외 (Rate Limit 도입 안 함) |
| Elevation of Privilege | STUDENT가 강사 API 호출 | 3중 인가 |

## 2. 인증 (JWT)

| 항목 | 값 |
|------|-----|
| 알고리즘 | HS256 |
| Secret | `JWT_SECRET_KEY` env (Base64, ≥256bit) |
| Claims | `sub=userId`, `username`, **`role=STUDENT\|INSTRUCTOR`** |
| Access TTL | 60분 (P1 유지) |
| Refresh TTL | 14일 (P1 유지) |
| Refresh 저장 | DB (`refresh_tokens`, user_id UNIQUE) |
| 검증 위치 | `JwtAuthenticationFilter` (OncePerRequestFilter) |

`JwtAuthenticationFilter`는 `Authorization: Bearer <token>`을 파싱해 `UserPrincipal`을 SecurityContext에 주입. Principal의 `getAuthorities()`는 `ROLE_STUDENT` / `ROLE_INSTRUCTOR` 단일 권한 반환.

> Spring Security의 `hasRole('INSTRUCTOR')`는 내부적으로 `ROLE_` 접두사를 붙여 매칭한다. enum 이름을 그대로 사용한다.

## 3. 인가 — 3중 방어선

### Layer 1. SecurityConfig URL 규칙

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
    .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/instructors/*").permitAll()
    .requestMatchers("/api/instructor/**").hasRole("INSTRUCTOR")
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/health", "/error").permitAll()
    .anyRequest().authenticated()
);
```

### Layer 2. 컨트롤러 메서드/클래스 어노테이션

```java
@RestController
@RequestMapping("/api/instructor/courses")
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorCourseController { ... }
```

`@EnableMethodSecurity(prePostEnabled = true)` 설정 필요.

### Layer 3. Service의 OwnershipValidator

```java
@Component
public class OwnershipValidator {
    private final CourseRepository courseRepository;
    private final OrderRepository orderRepository;
    // ...

    public void requireOwnedCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!course.isOwnedBy(userId)) {
            // 존재 은폐: 403이 아닌 404
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
    }

    public void requireOwnedOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
    }
    // ... enrollment, bookmark, playback 동일
}
```

**핵심:** 본인 소유가 아닌 리소스에는 **404**를 던진다. 403을 던지면 "그 ID는 존재하지만 내 것이 아니다"라는 정보가 누출된다.

## 4. 권한 매트릭스

| API | 비로그인 | STUDENT | INSTRUCTOR (소유) | INSTRUCTOR (타인 강의) |
|-----|:--:|:--:|:--:|:--:|
| GET /api/courses (PUBLISHED) | ✅ | ✅ | ✅ | ✅ |
| GET /api/courses/{id} (DRAFT/ARCHIVED) | 404 | 404 | (자기 거면 instructor API로) | 404 |
| POST /api/instructor/courses | 401 | 403 | ✅ | — |
| POST /api/instructor/courses/{id}/publish | 401 | 403 | ✅ | 404 |
| GET /api/instructors/{userId} | ✅ | ✅ | ✅ | ✅ |
| POST /api/enrollments (무료) | 401 | ✅ | ✅ | ✅ |
| POST /api/enrollments (유료) | 401 | 400 COURSE_NOT_FREE | 400 | 400 |
| POST /api/cart/items | 401 | ✅ | ✅ | ✅ |
| POST /api/payments/checkout | 401 | ✅ | ✅ | ✅ |
| GET /api/orders/{id} (타인 주문) | 401 | 404 | 404 | 404 |
| POST /api/reviews (진도 80% 미만) | 401 | 422 | 422 | 422 |
| PUT /api/playback (타인 enrollment) | 401 | 404 | 404 | 404 |

## 5. 비밀번호 저장

`BCryptPasswordEncoder` (Spring Security 기본). 평문 저장 금지.

## 6. CORS

- 빈: `CorsConfigurationSource`
- 허용 출처: `app.cors.allowed-origins` (콤마 구분)
- 허용 메서드: GET, POST, PUT, PATCH, DELETE, OPTIONS
- 허용 헤더: `*` (학습용)
- 자격증명: `allowCredentials = true` (필요 시)

## 7. 입력 검증

- `@Valid` + Jakarta Validation (`@NotBlank`, `@Min`, `@Max`, `@Size`, `@Positive` 등)
- 검증 실패 시 `@RestControllerAdvice`의 `MethodArgumentNotValidException` 핸들러가 `400` 응답 변환

## 8. 정보 은폐 규칙 (요약)

| 상황 | 응답 |
|------|------|
| 존재 안 함 | 404 |
| 존재하나 소유자 아님 | **404** (403 금지) |
| 인증은 됐으나 역할 부족 | 403 `ACCESS_DENIED` |
| 인증 안 됨 | 401 `UNAUTHORIZED` |
| 발행 안 된 강의 공개 조회 | 404 `COURSE_NOT_FOUND` |

## 9. SQL Injection / XSS

- JPA Parameter Binding 사용 → SQL Injection 회피
- 검색 `keyword`는 `LIKE %?%` 바인딩으로 처리
- 출력 측 XSS는 React 자동 이스케이프에 의존 (사용자가 `dangerouslySetInnerHTML` 사용 금지)

## 10. 의존성 보안

학습용이라 자동 스캐닝 미도입. 다만:
- JWT 라이브러리는 `jjwt 0.11.5` 고정
- Spring Boot starter 의존성만 사용 (직접 라이브러리 핀 최소화)
