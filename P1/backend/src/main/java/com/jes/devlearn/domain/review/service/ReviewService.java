package com.jes.devlearn.domain.review.service;

import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.review.dto.request.ReviewCreateRequestDTO;
import com.jes.devlearn.domain.review.dto.response.ReviewResponseDTO;
import com.jes.devlearn.domain.review.entity.Review;
import com.jes.devlearn.domain.review.error.ReviewErrorCode;
import com.jes.devlearn.domain.review.repository.ReviewRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public ReviewResponseDTO createReview(Long userId, ReviewCreateRequestDTO dto) {
        if (!courseRepository.existsById(dto.courseId())) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, dto.courseId())) {
            throw new CustomException(ReviewErrorCode.NOT_ENROLLED);
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
