package com.jes.devlearn.domain.course.service;

import com.jes.devlearn.domain.course.dto.request.CourseCreateRequestDTO;
import com.jes.devlearn.domain.course.dto.request.CourseUpdateRequestDTO;
import com.jes.devlearn.domain.course.dto.response.CoursePageResponseDTO;
import com.jes.devlearn.domain.course.dto.response.CourseResponseDTO;
import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public CoursePageResponseDTO getCourses(Long categoryId, Pageable pageable) {
        Page<Course> page = (categoryId != null)
                ? courseRepository.findAllByCategoryId(categoryId, pageable)
                : courseRepository.findAll(pageable);

        return CoursePageResponseDTO.from(page);
    }

    @Transactional(readOnly = true)
    public CourseResponseDTO getCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        return CourseResponseDTO.from(course);
    }

    @Transactional
    public CourseResponseDTO createCourse(CourseCreateRequestDTO dto) {
        Course course = new Course(dto.categoryId(), dto.title());
        courseRepository.save(course);

        return CourseResponseDTO.from(course);
    }

    @Transactional
    public CourseResponseDTO updateCourse(Long id, CourseUpdateRequestDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        course.update(dto.categoryId(), dto.title());

        return CourseResponseDTO.from(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        courseRepository.delete(course);
    }
}