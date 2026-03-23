package com.jes.devlearn.domain.course.repository;

import com.jes.devlearn.domain.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findAll(Pageable pageable);
    Page<Course> findAllByCategoryId(Long categoryId, Pageable pageable);
}