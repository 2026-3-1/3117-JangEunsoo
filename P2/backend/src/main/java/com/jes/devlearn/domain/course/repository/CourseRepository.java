package com.jes.devlearn.domain.course.repository;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c WHERE " +
           "c.publishStatus = 'PUBLISHED' AND " +
           "(:categoryId IS NULL OR c.categoryId = :categoryId) AND " +
           "(:difficulty IS NULL OR c.difficulty = :difficulty) AND " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> findWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("difficulty") String difficulty,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<Course> findAllByInstructorIdOrderByIdDesc(Long instructorId);

    List<Course> findAllByInstructorIdAndPublishStatusOrderByIdDesc(Long instructorId, PublishStatus publishStatus);

    long countByInstructorId(Long instructorId);

    long countByInstructorIdAndPublishStatus(Long instructorId, PublishStatus publishStatus);

    List<Course> findAllByInstructorIdAndPublishStatus(Long instructorId, PublishStatus publishStatus);
}
