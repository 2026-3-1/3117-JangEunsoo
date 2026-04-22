# 07. 강사 API 심화 스펙 — DevLearn P2

본 문서는 [03-api-design.md](./03-api-design.md) 중 **강사(`/api/instructor/**`)** 엔드포인트에 한정하여 **코드 수준의 구현 스펙**을 제공한다. DTO, 검증, 예외, 서비스 시그니처, 소유자 검증 패턴까지 구체화한다.

---

## 1. 패키지 구조 (백엔드 신규/변경)

P1의 도메인별 패키지 구조를 따른다. 강사 전용 코드는 `domain/instructor/` 하위에 모은다.

```
com.jes.devlearn
└─ domain
   ├─ instructor                        ← 🆕 강사 전용
   │  ├─ controller
   │  │  ├─ InstructorCourseController.java
   │  │  ├─ InstructorCurriculumController.java   (sections/lectures)
   │  │  ├─ InstructorProfileController.java
   │  │  └─ InstructorDashboardController.java
   │  ├─ dto
   │  │  ├─ request
   │  │  │  ├─ CourseCreateRequestDTO.java        (instructor 전용, 기존 courses 패키지와 분리)
   │  │  │  ├─ CourseUpdateRequestDTO.java
   │  │  │  ├─ SectionCreateRequestDTO.java
   │  │  │  ├─ SectionUpdateRequestDTO.java
   │  │  │  ├─ LectureCreateRequestDTO.java
   │  │  │  ├─ LectureUpdateRequestDTO.java
   │  │  │  └─ InstructorProfileUpdateRequestDTO.java
   │  │  └─ response
   │  │     ├─ InstructorCourseResponseDTO.java    (DRAFT 포함)
   │  │     ├─ InstructorCoursePageResponseDTO.java
   │  │     ├─ CourseStudentResponseDTO.java
   │  │     ├─ InstructorDashboardResponseDTO.java
   │  │     └─ InstructorProfileResponseDTO.java
   │  ├─ entity
   │  │  └─ InstructorProfile.java                 ← 02-data-model 스펙
   │  ├─ error
   │  │  └─ InstructorErrorCode.java
   │  ├─ repository
   │  │  └─ InstructorProfileRepository.java
   │  └─ service
   │     ├─ InstructorCourseService.java
   │     ├─ InstructorCurriculumService.java
   │     ├─ InstructorProfileService.java
   │     ├─ InstructorDashboardService.java
   │     └─ OwnershipValidator.java                 ← 09-security-design §6
   ├─ course                             (기존, 일부 변경)
   ├─ user                               (role 추가)
   └─ ...
```

**프론트 대응:**

```
frontend/src
├─ api
│  ├─ instructor
│  │  ├─ courses.ts
│  │  ├─ curriculum.ts
│  │  ├─ profile.ts
│  │  └─ dashboard.ts
│  └─ ... (기존 유지)
├─ components
│  ├─ ProtectedRoute.tsx   (기존)
│  └─ RoleGuard.tsx        🆕
├─ pages
│  ├─ instructor
│  │  ├─ InstructorDashboardPage.tsx
│  │  ├─ InstructorCourseListPage.tsx
│  │  ├─ InstructorCourseEditorPage.tsx
│  │  ├─ InstructorCourseStudentsPage.tsx
│  │  └─ InstructorProfileEditPage.tsx
│  └─ InstructorPublicProfilePage.tsx   (공개)
└─ context
   └─ AuthContext.tsx     ← role 저장 추가
```

---

## 2. JWT 및 UserPrincipal 규약

### 2-1. Token 규약 (P1 유지 + role 반영 없음)

- `accessToken.sub = userId`, `role` 클레임 **없음**
- `JwtAuthenticationFilter`가 `tokenProvider.getUserId(token)` → `customUserDetailsService.loadUserById(userId)` 호출
- `loadUserById`는 DB `User.role`을 읽어 `UserPrincipal`에 세팅 → 권위 원천은 **DB**

### 2-2. `UserPrincipal.getAuthorities()` 구현

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
}
```

Spring Security 관례상 `hasRole("INSTRUCTOR")` 표현을 쓰려면 Authority prefix `ROLE_`이 필요.

---

## 3. 컨트롤러 스펙

### 3-1. 공통 — 클래스 레벨 어노테이션

모든 강사 컨트롤러는 동일한 보안 어노테이션 세트를 가진다:

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instructor")
@PreAuthorize("hasRole('INSTRUCTOR')")       // ← 클래스 레벨
@Tag(name = "Instructor")                     // Swagger
@SecurityRequirement(name = "bearerAuth")
public class InstructorCourseController { ... }
```

### 3-2. `InstructorCourseController`

| 메서드 | 경로 | DTO | 응답 |
|--------|------|-----|------|
| `GET /courses` | 내 강의 목록 | Query: page, size, status? | `InstructorCoursePageResponseDTO` |
| `POST /courses` | 생성 | `CourseCreateRequestDTO` | `InstructorCourseResponseDTO` (201) |
| `GET /courses/{id}` | 상세 (소유자) | — | `InstructorCourseResponseDTO` |
| `PUT /courses/{id}` | 수정 | `CourseUpdateRequestDTO` | `InstructorCourseResponseDTO` |
| `DELETE /courses/{id}` | soft delete | — | 200 void |
| `POST /courses/{id}/publish` | 발행 | — | `InstructorCourseResponseDTO` |
| `POST /courses/{id}/archive` | 보관 | — | `InstructorCourseResponseDTO` |
| `POST /courses/{id}/cancel` | 🆕 폐강 (일괄 환불 + ARCHIVED) | — | `CourseCancelResultDTO` |
| `GET /courses/{id}/students` | 수강생 목록 | Query: page, size | `Page<CourseStudentResponseDTO>` |
| `GET /courses/{id}/reviews` | 리뷰 | Query: page, size | `Page<ReviewResponseDTO>` (기존 DTO 재사용) |

**코드 예:**

```java
@PostMapping("/courses")
public ResponseEntity<GlobalApiResponse<InstructorCourseResponseDTO>> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CourseCreateRequestDTO dto
) {
    InstructorCourseResponseDTO created =
            instructorCourseService.create(principal.getUserId(), dto);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(GlobalApiResponse.success(created));
}

@PutMapping("/courses/{id}")
public ResponseEntity<GlobalApiResponse<InstructorCourseResponseDTO>> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @Valid @RequestBody CourseUpdateRequestDTO dto
) {
    return ResponseEntity.ok(GlobalApiResponse.success(
            instructorCourseService.update(principal.getUserId(), id, dto)));
}

@PostMapping("/courses/{id}/publish")
public ResponseEntity<GlobalApiResponse<InstructorCourseResponseDTO>> publish(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
) {
    return ResponseEntity.ok(GlobalApiResponse.success(
            instructorCourseService.publish(principal.getUserId(), id),
            "강의가 발행되었습니다."));
}
```

### 3-3. `InstructorCurriculumController`

섹션·렉처 조작. 요청 URL에는 자식 리소스 ID가 나타나지만, **반드시 부모 강의의 소유자 검증**을 거친다.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instructor")
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorCurriculumController {

    private final InstructorCurriculumService curriculumService;

    @PostMapping("/courses/{courseId}/sections")
    public ResponseEntity<GlobalApiResponse<SectionResponseDTO>> createSection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody SectionCreateRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            GlobalApiResponse.success(
                curriculumService.addSection(principal.getUserId(), courseId, dto)));
    }

    @PutMapping("/sections/{sectionId}")
    public ResponseEntity<GlobalApiResponse<SectionResponseDTO>> updateSection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
            curriculumService.updateSection(principal.getUserId(), sectionId, dto)));
    }

    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteSection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sectionId
    ) {
        curriculumService.deleteSection(principal.getUserId(), sectionId);
        return ResponseEntity.ok(GlobalApiResponse.success("섹션이 삭제되었습니다."));
    }

    @PostMapping("/sections/{sectionId}/lectures")
    public ResponseEntity<GlobalApiResponse<LectureResponseDTO>> createLecture(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sectionId,
            @Valid @RequestBody LectureCreateRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            GlobalApiResponse.success(
                curriculumService.addLecture(principal.getUserId(), sectionId, dto)));
    }

    @PutMapping("/lectures/{lectureId}")
    public ResponseEntity<GlobalApiResponse<LectureResponseDTO>> updateLecture(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
            curriculumService.updateLecture(principal.getUserId(), lectureId, dto)));
    }

    @DeleteMapping("/lectures/{lectureId}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteLecture(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long lectureId
    ) {
        curriculumService.deleteLecture(principal.getUserId(), lectureId);
        return ResponseEntity.ok(GlobalApiResponse.success("렉처가 삭제되었습니다."));
    }
}
```

### 3-4. `InstructorProfileController`

```java
@GetMapping("/profile")
public ResponseEntity<GlobalApiResponse<InstructorProfileResponseDTO>> me(
        @AuthenticationPrincipal UserPrincipal principal
) {
    return ResponseEntity.ok(GlobalApiResponse.success(
        profileService.getMyProfile(principal.getUserId())));
}

@PutMapping("/profile")
public ResponseEntity<GlobalApiResponse<InstructorProfileResponseDTO>> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody InstructorProfileUpdateRequestDTO dto
) {
    return ResponseEntity.ok(GlobalApiResponse.success(
        profileService.updateMyProfile(principal.getUserId(), dto)));
}
```

### 3-5. `InstructorDashboardController`

```java
@GetMapping("/dashboard")
public ResponseEntity<GlobalApiResponse<InstructorDashboardResponseDTO>> dashboard(
        @AuthenticationPrincipal UserPrincipal principal
) {
    return ResponseEntity.ok(GlobalApiResponse.success(
        dashboardService.getDashboard(principal.getUserId())));
}
```

**주의:** `userId`를 쿼리 파라미터로 받지 않는다. 반드시 `UserPrincipal`에서만 추출 → 타인 통계 조회 원천 차단.

---

## 4. DTO 상세 스펙

### 4-1. `CourseCreateRequestDTO` (강사용, Java record)

```java
public record CourseCreateRequestDTO(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotBlank @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
                           message = "난이도는 BEGINNER/INTERMEDIATE/ADVANCED 중 하나여야 합니다.")
        String difficulty,
        @NotNull Long categoryId,
        @NotNull @Min(value = 0, message = "가격은 0원 이상이어야 합니다.") Long price   // 🆕
) {}
```

### 4-2. `CourseUpdateRequestDTO`

```java
public record CourseUpdateRequestDTO(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotBlank @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED") String difficulty,
        @NotNull Long categoryId,
        @NotNull @Min(0) Long price   // 🆕
) {}
```

### 4-3. `SectionCreateRequestDTO`

```java
public record SectionCreateRequestDTO(
        @NotBlank @Size(max = 100) String title,
        @NotNull @Min(1) Integer orderNum
) {}
```

### 4-4. `LectureCreateRequestDTO`

```java
public record LectureCreateRequestDTO(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Pattern(regexp = "^https?://.+", message = "동영상 URL은 http/https로 시작해야 합니다.")
        String videoUrl,
        @NotNull @Min(1) Integer orderNum,
        @NotNull @Min(0) Integer durationSeconds   // 🆕 재생 위치 계산용
) {}
```

### 4-5. `InstructorProfileUpdateRequestDTO`

```java
public record InstructorProfileUpdateRequestDTO(
        @NotBlank @Size(max = 50) String displayName,
        @Size(max = 2000) String bio,
        @Min(0) @Max(80) Integer careerYears,
        @Size(max = 500) @Pattern(regexp = "^(https?://.+)?$") String profileImageUrl
) {}
```

### 4-6. `InstructorCourseResponseDTO`

```java
public record InstructorCourseResponseDTO(
        Long id,
        String title,
        String description,
        String difficulty,
        Long categoryId,
        String categoryName,
        Long price,                          // 🆕
        String publishStatus,
        LocalDateTime publishedAt,
        Integer sectionCount,
        Integer lectureCount,
        Integer enrolledStudentCount,
        Long totalRevenue                    // 🆕 (결제완료-환불) 집계
) {
    public static InstructorCourseResponseDTO from(Course course, /* ... */) { /* ... */ }
}
```

### 4-7. `CourseStudentResponseDTO`

```java
public record CourseStudentResponseDTO(
        Long userId,
        String username,
        LocalDateTime enrolledAt,
        Double progressRate,           // 0.0 ~ 100.0
        Integer completedLectureCount,
        Integer totalLectureCount
) {}
```

### 4-8. `InstructorDashboardResponseDTO`

```java
public record InstructorDashboardResponseDTO(
        CourseCount courseCount,
        Integer totalStudents,
        Integer totalReviews,
        Double averageRating,
        Revenue revenue,                          // 🆕
        List<RecentEnrollment> recentEnrollments
) {
    public record CourseCount(Integer draft, Integer published, Integer archived) {}

    /** 🆕 매출 집계 — 강사 본인 강의의 모든 order_items 기준 */
    public record Revenue(
            Long grossAmount,     // 전체 결제된 합계 (price_snapshot 합)
            Long refundedAmount,  // 환불된 합계
            Long netAmount        // gross - refunded
    ) {}

    public record RecentEnrollment(
            Long userId, String username,
            Long courseId, String courseTitle,
            LocalDateTime enrolledAt) {}
}
```

---

## 5. 서비스 시그니처

```java
public interface InstructorCourseService {
    InstructorCoursePageResponseDTO getMyCourses(Long instructorId, Pageable pageable, String statusFilter);
    InstructorCourseResponseDTO getMyCourse(Long instructorId, Long courseId);
    InstructorCourseResponseDTO create(Long instructorId, CourseCreateRequestDTO dto);
    InstructorCourseResponseDTO update(Long instructorId, Long courseId, CourseUpdateRequestDTO dto);
    void delete(Long instructorId, Long courseId);
    InstructorCourseResponseDTO publish(Long instructorId, Long courseId);
    InstructorCourseResponseDTO archive(Long instructorId, Long courseId);
    Page<CourseStudentResponseDTO> getStudents(Long instructorId, Long courseId, Pageable pageable);

    /** 🆕 강의 취소: ARCHIVED 전환 + 해당 강의의 모든 PAID order_items 일괄 환불 */
    CourseCancelResultDTO cancelCourse(Long instructorId, Long courseId);
}

/** 강의 취소 결과 */
public record CourseCancelResultDTO(
        Long courseId,
        LocalDateTime archivedAt,
        Integer refundedOrderCount,
        Long refundedAmount
) {}

public interface InstructorCurriculumService {
    SectionResponseDTO addSection(Long instructorId, Long courseId, SectionCreateRequestDTO dto);
    SectionResponseDTO updateSection(Long instructorId, Long sectionId, SectionUpdateRequestDTO dto);
    void deleteSection(Long instructorId, Long sectionId);
    LectureResponseDTO addLecture(Long instructorId, Long sectionId, LectureCreateRequestDTO dto);
    LectureResponseDTO updateLecture(Long instructorId, Long lectureId, LectureUpdateRequestDTO dto);
    void deleteLecture(Long instructorId, Long lectureId);
}

public interface InstructorProfileService {
    InstructorProfileResponseDTO getMyProfile(Long instructorId);
    InstructorProfileResponseDTO updateMyProfile(Long instructorId, InstructorProfileUpdateRequestDTO dto);
    InstructorPublicProfileResponseDTO getPublicProfile(Long userId);   // 공개용
}

public interface InstructorDashboardService {
    InstructorDashboardResponseDTO getDashboard(Long instructorId);
}
```

---

## 6. 소유자 검증 표준 패턴

`OwnershipValidator`는 강사 API의 모든 쓰기 경로에서 **첫 번째 호출**.

```java
@Component
@RequiredArgsConstructor
public class OwnershipValidator {
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LectureRepository lectureRepository;

    /** courseId가 존재하며 userId 소유이면 반환, 아니면 COURSE_NOT_FOUND (은폐) */
    public Course requireOwnedCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!course.isOwnedBy(userId)) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    /** sectionId의 부모 강의 소유자 검증 후 section 반환 */
    public Section requireOwnedSection(Long sectionId, Long userId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.SECTION_NOT_FOUND));
        requireOwnedCourse(section.getCourseId(), userId);
        return section;
    }

    /** lectureId의 부모 섹션→강의 소유자 검증 후 lecture 반환 */
    public Lecture requireOwnedLecture(Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.LECTURE_NOT_FOUND));
        requireOwnedSection(lecture.getSectionId(), userId);
        return lecture;
    }
}
```

**서비스 사용 예:**

```java
@Service
@RequiredArgsConstructor
public class InstructorCurriculumServiceImpl implements InstructorCurriculumService {
    private final OwnershipValidator ownership;
    private final SectionRepository sectionRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public SectionResponseDTO addSection(Long instructorId, Long courseId, SectionCreateRequestDTO dto) {
        Course course = ownership.requireOwnedCourse(courseId, instructorId);   // ← 첫 줄
        Section section = new Section(course.getId(), dto.title(), dto.orderNum());
        return SectionResponseDTO.from(sectionRepository.save(section));
    }

    @Transactional
    public void deleteLecture(Long instructorId, Long lectureId) {
        Lecture lecture = ownership.requireOwnedLecture(lectureId, instructorId); // ← 첫 줄
        lectureRepository.delete(lecture);
    }
}
```

---

## 7. 에러 코드

### 7-1. `InstructorErrorCode` (신규)

```java
@Getter
@AllArgsConstructor
public enum InstructorErrorCode implements ErrorCode {
    NOT_INSTRUCTOR("강사 권한이 필요합니다.", HttpStatus.FORBIDDEN),
    PROFILE_NOT_FOUND("강사 프로필이 없습니다.", HttpStatus.NOT_FOUND),
    COURSE_NOT_READY("발행 조건을 만족하지 않습니다. 섹션과 렉처를 최소 1개 이상 추가하세요.", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION("허용되지 않은 상태 전환입니다.", HttpStatus.BAD_REQUEST);

    private final String defaultMessage;
    private final HttpStatus status;
}
```

### 7-2. 기존 `CourseErrorCode` 확장

```java
public enum CourseErrorCode implements ErrorCode {
    COURSE_NOT_FOUND("강의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),   // 기존
    SECTION_NOT_FOUND("섹션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),   // 🆕
    LECTURE_NOT_FOUND("렉처를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);   // 🆕
    // ...
}
```

---

## 8. 테스트 시나리오 (강사 API 한정)

### 8-1. 정상 플로우

| # | 시나리오 | 기대 |
|---|---------|------|
| 1 | INSTRUCTOR가 강의 생성 | 201, `publishStatus = DRAFT` |
| 2 | 섹션 추가 → 렉처 추가 → publish | 3단계 모두 200/201, 최종 `publishStatus = PUBLISHED` |
| 3 | 섹션/렉처 없이 publish 시도 | 400 `COURSE_NOT_READY` |
| 4 | 내 강의 목록 조회 | 내가 만든 DRAFT + PUBLISHED 전부 반환 |
| 5 | 대시보드 조회 | courseCount / totalStudents / averageRating 정상 집계 |

### 8-2. 권한 우회 시도 (보안 테스트 — §09 체크리스트와 연동)

| # | 시나리오 | 기대 |
|---|---------|------|
| 1 | STUDENT 토큰으로 `POST /api/instructor/courses` | 403 |
| 2 | 토큰 없이 `GET /api/instructor/dashboard` | 401 |
| 3 | INSTRUCTOR A 토큰으로 `PUT /api/instructor/courses/{B의 강의 ID}` | **404** (은폐) |
| 4 | INSTRUCTOR A 토큰으로 `POST /api/instructor/sections/{B 강의의 섹션ID}/lectures` | **404** |
| 5 | 만료된 JWT로 호출 | 401 |
| 6 | JWT payload 변조(sub → 1) 후 호출 | 401 (서명 실패) |

### 8-3. 엣지 케이스

| # | 시나리오 | 기대 |
|---|---------|------|
| 1 | ARCHIVED 상태에서 publish 재시도 | 400 `INVALID_STATUS_TRANSITION` |
| 2 | signup role=INSTRUCTOR인데 displayName 누락 | 400 `VALIDATION_ERROR` |
| 3 | displayName 51자 이상 | 400 |
| 4 | videoUrl이 `ftp://...` | 400 |
| 5 | 다른 강사의 렉처를 videoUrl만 복붙해 create | 허용 (URL 중복 제약 없음 — 학습 프로젝트) |

---

## 9. 응답 시간 SLA (참고)

| 엔드포인트 | 목표 (p95) |
|-----------|-----------|
| `GET /api/instructor/courses` | < 200ms |
| `GET /api/instructor/dashboard` | < 500ms |
| `GET /api/instructor/courses/{id}/students` | < 300ms (20건 페이지) |
| 쓰기 엔드포인트 | < 300ms |

대시보드의 `averageRating` 같은 집계는 단순 `AVG()` 쿼리로 시작. 느려지면 P3에서 캐시/집계 테이블 도입.
