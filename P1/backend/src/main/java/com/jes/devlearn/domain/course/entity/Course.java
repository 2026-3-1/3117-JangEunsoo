package com.jes.devlearn.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE courses SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String difficulty;

    @Column(name = "instructor_name")
    private String instructorName;

    private LocalDateTime deletedAt;

    public Course(Long categoryId, String title, String description, String difficulty, String instructorName) {
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.instructorName = instructorName;
    }

    public void update(Long categoryId, String title, String description, String difficulty, String instructorName) {
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.instructorName = instructorName;
    }
}
