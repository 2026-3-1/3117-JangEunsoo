package com.jes.devlearn.domain.progress.service;

import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.error.EnrollmentErrorCode;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.progress.dto.request.CompleteRequestDTO;
import com.jes.devlearn.domain.progress.dto.response.ProgressRateResponseDTO;
import com.jes.devlearn.domain.progress.entity.LectureProgress;
import com.jes.devlearn.domain.progress.error.ProgressErrorCode;
import com.jes.devlearn.domain.progress.repository.LectureProgressRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final LectureProgressRepository lectureProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public void complete(Long userId, CompleteRequestDTO dto) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(dto.enrollmentId(), userId)
                .orElseThrow(() -> new CustomException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!lectureRepository.existsById(dto.lectureId())) {
            throw new CustomException(ProgressErrorCode.LECTURE_NOT_FOUND);
        }

        if (lectureProgressRepository.existsByEnrollmentIdAndLectureId(dto.enrollmentId(), dto.lectureId())) {
            return;
        }

        lectureProgressRepository.save(new LectureProgress(dto.enrollmentId(), dto.lectureId()));
    }

    @Transactional(readOnly = true)
    public ProgressRateResponseDTO getProgressRate(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new CustomException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        long totalLectures = lectureRepository.countByCourseId(enrollment.getCourseId());
        long completedLectures = lectureProgressRepository.countByEnrollmentId(enrollmentId);

        double rate = totalLectures == 0 ? 0.0 : Math.round((double) completedLectures / totalLectures * 1000.0) / 10.0;

        return new ProgressRateResponseDTO(
                enrollmentId,
                enrollment.getCourseId(),
                (int) totalLectures,
                (int) completedLectures,
                rate
        );
    }
}
