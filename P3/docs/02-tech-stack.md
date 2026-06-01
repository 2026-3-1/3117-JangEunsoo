# 02. 기술 스택 & 디렉토리 구조 — DevLearn P2

## 1. 정확한 버전

### 1-1. Backend (`backend/build.gradle`)

| 항목 | 버전 / 값 |
|------|----------|
| Spring Boot | **4.0.2** |
| Java | **21** (toolchain) |
| Gradle Wrapper | **8.14** |
| Group / Artifact | `com.jes` / `dev-learn` |
| JWT 라이브러리 | `io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson` **0.11.5** |
| Swagger / OpenAPI | `springdoc-openapi-starter-webmvc-ui` **2.8.0** |
| MySQL Connector | `com.mysql:mysql-connector-j` (Spring 관리, runtime) |
| 빌드/테스트 도구 | `spring-boot-starter-test`, `spring-security-test`, `junit-platform-launcher` |
| Lombok | annotation processor + compileOnly |

**의존성 묶음:**

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0'

    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'

    runtimeOnly 'com.mysql:mysql-connector-j'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

### 1-2. Frontend (`frontend/package.json`)

| 항목 | 버전 |
|------|------|
| React | **19.2.4** |
| React DOM | 19.2.4 |
| React Router DOM | **7.13.1** |
| TypeScript | **~5.9.3** |
| Vite | **8.0.1** |
| `@vitejs/plugin-react` | 6.0.1 |
| Tailwind CSS | **4.2.2** |
| ESLint | 9.39.4 |
| axios | **1.13.6** |

**npm 스크립트:**

```json
"scripts": {
  "dev": "vite",
  "build": "tsc -b && vite build",
  "preview": "vite preview",
  "type-check": "tsc --noEmit",
  "lint": "eslint ."
}
```

### 1-3. DBMS / 인프라

- MySQL **8.0** (`mysql:8.0` 이미지)
- 문자셋 `utf8mb4`
- ORM Hibernate (`ddl-auto=update` 자동 스키마 반영)
- 마이그레이션 도구는 도입하지 않음 (학습용). 수동 마이그레이션 SQL 1개 + seed.sql 1개

## 2. 모노레포 디렉토리 구조

```
P2/
├── CLAUDE.md
├── README.md
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle/wrapper/
│   ├── gradlew, gradlew.bat
│   └── src/
│       ├── main/
│       │   ├── java/com/jes/devlearn/
│       │   │   ├── DevLearnApplication.java
│       │   │   ├── domain/
│       │   │   │   ├── auth/         (AuthController, AuthService, RefreshToken*)
│       │   │   │   ├── bookmark/
│       │   │   │   ├── cart/
│       │   │   │   ├── category/
│       │   │   │   ├── course/       (Course, Section, Lecture)
│       │   │   │   ├── enrollment/
│       │   │   │   ├── health/
│       │   │   │   ├── instructor/   (InstructorProfile + 강사 전용 컨트롤러/서비스)
│       │   │   │   ├── order/        (Order, OrderItem)
│       │   │   │   ├── payment/      (Payment, Refund, MockPaymentGateway)
│       │   │   │   ├── playback/     (PlaybackPosition)
│       │   │   │   ├── progress/     (LectureProgress)
│       │   │   │   ├── review/
│       │   │   │   └── user/         (User, Role)
│       │   │   └── global/
│       │   │       ├── security/     (SecurityConfig, UserPrincipal, OwnershipValidator)
│       │   │       │   └── jwt/      (JwtAuthenticationFilter, TokenProvider, TokenConstants)
│       │   │       ├── error/        (ErrorCode, GlobalExceptionHandler, CustomException)
│       │   │       └── response/     (GlobalApiResponse)
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/
│       │           ├── schema-p2-migration.sql
│       │           └── seed.sql
│       └── test/java/com/jes/devlearn/
│           ├── DevLearnApplicationTests.java
│           ├── domain/
│           │   ├── enrollment/service/EnrollmentPaidGuardTest.java
│           │   ├── payment/service/CheckoutTest.java
│           │   └── review/service/ReviewProgressGateTest.java
│           └── global/security/
│               ├── AuthorizationGateTest.java
│               └── OwnershipValidatorTest.java
├── frontend/
│   ├── Dockerfile
│   ├── package.json, tsconfig*.json, vite.config.ts
│   ├── tailwind.config.js, postcss.config.js
│   ├── index.html
│   └── src/
│       ├── App.tsx
│       ├── main.tsx
│       ├── index.css
│       ├── api/
│       │   ├── auth.ts, courses.ts, categories.ts
│       │   ├── enrollments.ts, progress.ts, reviews.ts
│       │   ├── cart.ts, order.ts, payment.ts
│       │   ├── playback.ts, bookmark.ts
│       │   └── instructor.ts
│       ├── components/
│       │   ├── ProtectedRoute.tsx, RoleGuard.tsx
│       │   ├── NavBar.tsx, InstructorCard.tsx
│       ├── context/
│       │   └── AuthContext.tsx
│       └── pages/
│           ├── LoginPage.tsx, SignupPage.tsx
│           ├── CoursesPage.tsx, CourseDetailPage.tsx
│           ├── LearningPage.tsx, MyCoursesPage.tsx
│           ├── MyBookmarksPage.tsx, InstructorPublicProfilePage.tsx
│           ├── CartPage.tsx, CheckoutPage.tsx
│           ├── OrdersPage.tsx, OrderDetailPage.tsx
│           └── instructor/
│               ├── InstructorDashboardPage.tsx
│               ├── InstructorCourseListPage.tsx
│               ├── InstructorCourseEditorPage.tsx
│               ├── InstructorCourseStudentsPage.tsx
│               └── InstructorProfileEditPage.tsx
└── docs/   (이 폴더는 PRD 문서)
```

## 3. 패키지 네이밍 규칙

- Base package: `com.jes.devlearn`
- 도메인 단위 패키지: `com.jes.devlearn.domain.<도메인명>`
- 각 도메인 내부 구조: `controller/`, `service/`, `repository/`, `entity/`, `dto/`(필요 시 `request/`+`response/` 또는 record), `exception/`
- 전역: `com.jes.devlearn.global.<security|error|response|config>`
- DTO 명명: P1에서 온 것은 `*RequestDTO`/`*ResponseDTO`, P2 신규는 `*Request`/`*Response` (Record). **혼용을 유지** (P1 호환)

## 4. 표준 응답 포맷

P1·P2 공통. 모든 컨트롤러 응답은 `GlobalApiResponse<T>`로 감싸 반환:

```json
// 성공
{
  "success": true,
  "status": 200,
  "data": { ... }
}

// 실패
{
  "success": false,
  "status": 403,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "해당 리소스에 접근할 권한이 없습니다."
  }
}
```

## 5. 코드 스타일

- 자바: 4-space indent, Lombok `@Getter @Builder @NoArgsConstructor(AccessLevel.PROTECTED) @AllArgsConstructor` 패턴 권장
- TypeScript: 2-space indent, `import type` 사용, `tsc --noEmit` 통과 필수
- 커밋 메시지·에러 메시지·UI 카피: **한국어**

## 6. 외부 의존성 없음

CDN/외부 API 호출은 없다. 모든 종속은 위 표의 라이브러리·MySQL뿐.
