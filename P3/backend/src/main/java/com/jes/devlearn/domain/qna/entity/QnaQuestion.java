package com.jes.devlearn.domain.qna.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "qna_questions", indexes = {
        @Index(name = "idx_qna_q_course", columnList = "course_id"),
        @Index(name = "idx_qna_q_author", columnList = "author_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QnaQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 강사/관리자가 비공개 처리 가능 (작성자·강사·관리자만 열람)
    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "answer_count", nullable = false)
    private int answerCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public QnaQuestion(Long courseId, Long authorId, String title, String content, boolean isPrivate) {
        this.courseId = courseId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.isPrivate = isPrivate;
    }

    public void update(String title, String content, boolean isPrivate) {
        this.title = title;
        this.content = content;
        this.isPrivate = isPrivate;
    }

    public void increaseAnswerCount() {
        this.answerCount++;
    }

    public void decreaseAnswerCount() {
        if (this.answerCount > 0) this.answerCount--;
    }

    public boolean isAnswered() {
        return this.answerCount > 0;
    }

    public boolean isAuthoredBy(Long userId) {
        return this.authorId.equals(userId);
    }
}
