package com.jes.devlearn.domain.review.service;

import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.progress.repository.LectureProgressRepository;
import com.jes.devlearn.domain.review.dto.request.ReviewCreateRequestDTO;
import com.jes.devlearn.domain.review.dto.response.ReviewResponseDTO;
import com.jes.devlearn.domain.review.entity.Review;
import com.jes.devlearn.domain.review.error.ReviewErrorCode;
import com.jes.devlearn.domain.review.error.ReviewProgressGateException;
import com.jes.devlearn.domain.review.repository.ReviewRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    public static final int REQUIRED_PROGRESS_RATE = 80;

    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;

    @Transactional
    public ReviewResponseDTO createReview(Long userId, ReviewCreateRequestDTO dto) {
        if (!courseRepository.existsById(dto.courseId())) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        Enrollment enrollment = enrollmentRepository.findAllByUserId(userId).stream()
                .filter(e -> e.getCourseId().equals(dto.courseId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ReviewErrorCode.NOT_ENROLLED));

        long total = lectureRepository.countByCourseId(dto.courseId());
        long completed = lectureProgressRepository.countByEnrollmentId(enrollment.getId());
        int rate = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
        if (rate < REQUIRED_PROGRESS_RATE) {
            throw new ReviewProgressGateException(rate, REQUIRED_PROGRESS_RATE);
        }

        Review review = reviewRepository.save(new Review(userId, dto.courseId(), dto.rating(), dto.comment()));
        return ReviewResponseDTO.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByCourse(Long courseId) {
        return reviewRepository.findAllByCourseId(courseId).stream()
                .map(ReviewResponseDTO::from)
                .toList();
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new CustomException(ReviewErrorCode.REVIEW_NOT_FOUND));
        reviewRepository.delete(review);
    }
}
