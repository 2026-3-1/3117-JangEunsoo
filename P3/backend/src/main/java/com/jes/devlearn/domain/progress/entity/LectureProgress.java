package com.jes.devlearn.domain.progress.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "lecture_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "lecture_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;

    public LectureProgress(Long enrollmentId, Long lectureId) {
        this.enrollmentId = enrollmentId;
        this.lectureId = lectureId;
        this.isCompleted = true;
    }
}
