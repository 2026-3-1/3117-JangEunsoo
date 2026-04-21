package com.jes.devlearn.domain.progress.repository;

import com.jes.devlearn.domain.progress.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {
    boolean existsByEnrollmentIdAndLectureId(Long enrollmentId, Long lectureId);
    long countByEnrollmentId(Long enrollmentId);
}
