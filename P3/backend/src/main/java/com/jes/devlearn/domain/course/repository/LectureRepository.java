package com.jes.devlearn.domain.course.repository;

import com.jes.devlearn.domain.course.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findAllBySectionIdIn(List<Long> sectionIds);

    List<Lecture> findAllBySectionIdOrderByOrderNumAsc(Long sectionId);

    Optional<Lecture> findTopBySectionIdOrderByOrderNumDesc(Long sectionId);

    long countBySectionId(Long sectionId);

    @Query("SELECT COUNT(l) FROM Lecture l WHERE l.sectionId IN " +
           "(SELECT s.id FROM Section s WHERE s.courseId = :courseId)")
    long countByCourseId(@Param("courseId") Long courseId);

    void deleteAllBySectionId(Long sectionId);
}
