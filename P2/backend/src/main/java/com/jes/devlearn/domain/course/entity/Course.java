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

    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String difficulty;

    @Deprecated
    @Column(name = "instructor_name")
    private String instructorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    private PublishStatus publishStatus = PublishStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private LocalDateTime deletedAt;

    public Course(Long categoryId, String title, String description, String difficulty, String instructorName) {
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.instructorName = instructorName;
    }

    public Course(Long instructorId, Long categoryId, String title, String description, String difficulty, String instructorName) {
        this.instructorId = instructorId;
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

    public void publish() {
        this.publishStatus = PublishStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    public void archive() {
        this.publishStatus = PublishStatus.ARCHIVED;
    }

    public boolean isOwnedBy(Long userId) {
        return this.instructorId != null && this.instructorId.equals(userId);
    }
}
