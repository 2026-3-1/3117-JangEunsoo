package com.jes.devlearn.domain.bookmark.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "time_seconds", nullable = false)
    private Integer timeSeconds;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Bookmark(Long userId, Long lectureId, Integer timeSeconds, String memo) {
        this.userId = userId;
        this.lectureId = lectureId;
        this.timeSeconds = timeSeconds == null ? 0 : timeSeconds;
        this.memo = memo;
    }

    public void update(Integer timeSeconds, String memo) {
        if (timeSeconds != null) this.timeSeconds = timeSeconds;
        this.memo = memo;
    }
}
