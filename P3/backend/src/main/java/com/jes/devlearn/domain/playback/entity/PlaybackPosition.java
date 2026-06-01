package com.jes.devlearn.domain.playback.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "playback_positions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "lecture_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PlaybackPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "current_time_seconds", nullable = false)
    private Integer currentTimeSeconds;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PlaybackPosition(Long enrollmentId, Long lectureId, Integer currentTimeSeconds) {
        this.enrollmentId = enrollmentId;
        this.lectureId = lectureId;
        this.currentTimeSeconds = currentTimeSeconds == null ? 0 : currentTimeSeconds;
    }

    public void updatePosition(Integer currentTimeSeconds) {
        this.currentTimeSeconds = currentTimeSeconds == null ? 0 : currentTimeSeconds;
    }
}
