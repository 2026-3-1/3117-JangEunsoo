package com.jes.devlearn.domain.course.service;

import com.jes.devlearn.domain.course.dto.request.CourseCreateRequestDTO;
import com.jes.devlearn.domain.course.dto.request.CourseUpdateRequestDTO;
import com.jes.devlearn.domain.course.dto.response.*;
import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.Lecture;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.entity.Section;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.course.repository.SectionRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LectureRepository lectureRepository;

    @Transactional(readOnly = true)
    public CoursePageResponseDTO getCourses(Long categoryId, String difficulty, String keyword, Pageable pageable) {
        Page<Course> page = courseRepository.findWithFilters(categoryId, difficulty, keyword, pageable);
        return CoursePageResponseDTO.from(page);
    }

    @Transactional(readOnly = true)
    public CourseDetailResponseDTO getCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (course.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }

        List<Section> sections = sectionRepository.findAllByCourseIdOrderByOrderNumAsc(course.getId());
        List<Long> sectionIds = sections.stream().map(Section::getId).toList();

        Map<Long, List<Lecture>> lecturesBySectionId = lectureRepository.findAllBySectionIdIn(sectionIds)
                .stream().collect(Collectors.groupingBy(Lecture::getSectionId));

        List<SectionResponseDTO> sectionDTOs = sections.stream()
                .map(section -> {
                    List<LectureResponseDTO> lectures = lecturesBySectionId.getOrDefault(section.getId(), List.of())
                            .stream()
                            .sorted((a, b) -> {
                                if (a.getOrderNum() == null) return 1;
                                if (b.getOrderNum() == null) return -1;
                                return Integer.compare(a.getOrderNum(), b.getOrderNum());
                            })
                            .map(LectureResponseDTO::from)
                            .toList();
                    return SectionResponseDTO.from(section, lectures);
                })
                .toList();

        return CourseDetailResponseDTO.from(course, sectionDTOs);
    }

    @Transactional
    public CourseResponseDTO createCourse(CourseCreateRequestDTO dto) {
        Course course = new Course(dto.categoryId(), dto.title(), dto.description(), dto.difficulty(), dto.instructorName());
        courseRepository.save(course);
        return CourseResponseDTO.from(course);
    }

    @Transactional
    public CourseResponseDTO updateCourse(Long id, CourseUpdateRequestDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        course.update(dto.categoryId(), dto.title(), dto.description(), dto.difficulty(), dto.instructorName());
        return CourseResponseDTO.from(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        courseRepository.delete(course);
    }
}
