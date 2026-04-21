package com.jes.devlearn.domain.course.controller;

import com.jes.devlearn.domain.course.dto.response.CourseDetailResponseDTO;
import com.jes.devlearn.domain.course.dto.response.CoursePageResponseDTO;
import com.jes.devlearn.domain.course.service.CourseService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
