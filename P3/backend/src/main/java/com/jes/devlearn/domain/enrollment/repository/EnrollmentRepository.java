package com.jes.devlearn.domain.enrollment.repository;

import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrollment> findAllByUserId(Long userId);

    Optional<Enrollment> findByIdAndUserId(Long id, Long userId);

    long countByCourseId(Long courseId);

    long countByCourseIdIn(Collection<Long> courseIds);

    List<Enrollment> findAllByCourseId(Long courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.courseId IN :courseIds ORDER BY e.createdAt DESC")
    List<Enrollment> findRecentByCourseIds(@Param("courseIds") Collection<Long> courseIds, Pageable pageable);

    void deleteAllByCourseId(Long courseId);
}
