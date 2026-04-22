package com.jes.devlearn.domain.course.repository;

import com.jes.devlearn.domain.course.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findAllByCourseIdOrderByOrderNumAsc(Long courseId);

    long countByCourseId(Long courseId);

    Optional<Section> findTopByCourseIdOrderByOrderNumDesc(Long courseId);
}
