package com.jes.devlearn.domain.admin.service;

import com.jes.devlearn.domain.admin.dto.response.AdminCoursePageResponse;
import com.jes.devlearn.domain.admin.dto.response.AdminCourseResponse;
import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminCoursePageResponse list(PublishStatus status, String keyword, Pageable pageable) {
        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Course> page = courseRepository.findAllForAdmin(status, normalized, pageable);

        Map<Long, String> usernameById = new HashMap<>();
        page.getContent().stream().map(Course::getInstructorId).distinct().forEach(iid -> {
            if (iid != null) {
                userRepository.findById(iid).ifPresent(u -> usernameById.put(iid, u.getUsername()));
            }
        });

        List<AdminCourseResponse> content = page.getContent().stream()
                .map(c -> AdminCourseResponse.of(
                        c,
                        usernameById.get(c.getInstructorId()),
                        enrollmentRepository.countByCourseId(c.getId())))
                .toList();

        return new AdminCoursePageResponse(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public AdminCourseResponse block(Long adminId, Long courseId, String reason) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        course.blockByAdmin(reason);
        log.info("[Admin] courseId={} 차단 사유='{}' (by adminId={})", courseId, reason, adminId);
        return toResponse(course);
    }

    @Transactional
    public AdminCourseResponse unblock(Long adminId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        course.unblockByAdmin();
        log.info("[Admin] courseId={} 차단 해제 (by adminId={})", courseId, adminId);
        return toResponse(course);
    }

    private AdminCourseResponse toResponse(Course course) {
        String username = course.getInstructorId() == null ? null
                : userRepository.findById(course.getInstructorId()).map(u -> u.getUsername()).orElse(null);
        return AdminCourseResponse.of(course, username, enrollmentRepository.countByCourseId(course.getId()));
    }
}
