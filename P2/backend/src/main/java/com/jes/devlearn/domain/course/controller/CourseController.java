package com.jes.devlearn.domain.course.controller;

import com.jes.devlearn.domain.course.dto.request.CourseCreateRequestDTO;
import com.jes.devlearn.domain.course.dto.request.CourseUpdateRequestDTO;
import com.jes.devlearn.domain.course.dto.response.CourseDetailResponseDTO;
import com.jes.devlearn.domain.course.dto.response.CoursePageResponseDTO;
import com.jes.devlearn.domain.course.dto.response.CourseResponseDTO;
import com.jes.devlearn.domain.course.service.CourseService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<CoursePageResponseDTO>> getCourses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 12, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(courseService.getCourses(categoryId, difficulty, keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<CourseDetailResponseDTO>> getCourse(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(courseService.getCourse(id)));
    }

    @PostMapping
    public ResponseEntity<GlobalApiResponse<CourseResponseDTO>> createCourse(
            @Valid @RequestBody CourseCreateRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.success(courseService.createCourse(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<CourseResponseDTO>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(courseService.updateCourse(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteCourse(
            @PathVariable Long id
    ) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(GlobalApiResponse.success("강의가 삭제되었습니다."));
    }
}
