package com.jes.devlearn.domain.enrollment.repository;

import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrollment> findAllByUserId(Long userId);

    Optional<Enrollment> findByIdAndUserId(Long id, Long userId);
}
