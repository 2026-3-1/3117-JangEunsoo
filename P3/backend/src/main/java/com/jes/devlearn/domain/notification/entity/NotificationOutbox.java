package com.jes.devlearn.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 알림 아웃박스. 비즈니스 트랜잭션이 알림을 직접 보내지 않고 outbox row만 남긴 뒤,
 * 스케줄러가 비동기로 발송한다. dedupKey UNIQUE로 멱등성을 보장(중복 enqueue 무시).
 */
@Entity
@Table(name = "notification_outbox", indexes = {
        @Index(name = "idx_outbox_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationOutbox {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dedup_key", unique = true, nullable = false, length = 100)
    private String dedupKey;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public NotificationOutbox(String dedupKey, String eventType, NotificationChannel channel,
                              String title, String message) {
        this.dedupKey = dedupKey;
        this.eventType = eventType;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.status = NotificationStatus.PENDING;
        this.nextAttemptAt = LocalDateTime.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastError = null;
    }

    /** 발송 실패 시 호출. 최대 시도 횟수를 넘기면 FAILED로 종결, 아니면 지수 백오프로 재시도 예약 */
    public void markFailed(String error) {
        this.attemptCount++;
        this.lastError = error == null ? null : (error.length() > 500 ? error.substring(0, 500) : error);
        if (this.attemptCount >= MAX_ATTEMPTS) {
            this.status = NotificationStatus.FAILED;
            this.nextAttemptAt = null;
        } else {
            // 지수 백오프: 1, 2, 4, 8분
            long backoffMinutes = 1L << (this.attemptCount - 1);
            this.nextAttemptAt = LocalDateTime.now().plusMinutes(backoffMinutes);
        }
    }
}
