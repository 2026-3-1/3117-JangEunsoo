package com.jes.devlearn.domain.instructor.repository;

import com.jes.devlearn.domain.instructor.entity.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {

    @Query("SELECT p FROM InstructorProfile p WHERE p.user.id = :userId")
    Optional<InstructorProfile> findByUserId(@Param("userId") Long userId);
}
