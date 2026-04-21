package com.jes.devlearn.domain.enrollment.service;

import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.enrollment.dto.request.EnrollmentCreateRequestDTO;
import com.jes.devlearn.domain.enrollment.dto.response.EnrollmentResponseDTO;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.error.EnrollmentErrorCode;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public EnrollmentResponseDTO enroll(Long userId, EnrollmentCreateRequestDTO dto) {
        if (!courseRepository.existsById(dto.courseId())) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, dto.courseId())) {
            throw new CustomException(EnrollmentErrorCode.ALREADY_ENROLLED);
        }
        Enrollment enrollment = enrollmentRepository.save(new Enrollment(userId, dto.courseId()));
        return EnrollmentResponseDTO.from(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getMyEnrollments(Long userId) {
        return enrollmentRepository.findAllByUserId(userId).stream()
                .map(EnrollmentResponseDTO::from)
                .toList();
    }

    @Transactional
    public void cancel(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new CustomException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));
        enrollmentRepository.delete(enrollment);
    }
}
