package com.jes.devlearn.domain.review.repository;

import com.jes.devlearn.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByCourseId(Long courseId);
    Optional<Review> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    long countByCourseIdIn(Collection<Long> courseIds);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.courseId IN :courseIds")
    Double avgRatingByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.courseId = :courseId")
    Double avgRatingByCourseId(@Param("courseId") Long courseId);

    long countByCourseId(Long courseId);
}
