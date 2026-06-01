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
@Table(name = "qna_answers", indexes = {
        @Index(name = "idx_qna_a_question", columnList = "question_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QnaAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    // 답변 작성 시점 작성자 역할(강사/관리자) 스냅샷 — 표시용
    @Column(name = "author_role", nullable = false, length = 20)
    private String authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public QnaAnswer(Long questionId, Long authorId, String authorRole, String content) {
        this.questionId = questionId;
        this.authorId = authorId;
        this.authorRole = authorRole;
        this.content = content;
    }

    public void update(String content) {
        this.content = content;
    }

    public boolean isAuthoredBy(Long userId) {
        return this.authorId.equals(userId);
    }
}
