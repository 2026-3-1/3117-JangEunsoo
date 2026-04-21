package com.jes.devlearn.domain.review.repository;

import com.jes.devlearn.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByCourseId(Long courseId);
    Optional<Review> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
