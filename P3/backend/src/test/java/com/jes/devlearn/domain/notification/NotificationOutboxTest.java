package com.jes.devlearn.domain.notification;

import com.jes.devlearn.domain.notification.entity.NotificationChannel;
import com.jes.devlearn.domain.notification.entity.NotificationOutbox;
import com.jes.devlearn.domain.notification.entity.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 아웃박스 재시도/백오프 규칙")
class NotificationOutboxTest {

    private NotificationOutbox newOutbox() {
        return new NotificationOutbox("k1", "TEST", NotificationChannel.LOG, "t", "m");
    }

    @Test
    @DisplayName("생성 직후 PENDING, 즉시 발송 대상")
    void initial_pending() {
        NotificationOutbox o = newOutbox();
        assertThat(o.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(o.getAttemptCount()).isZero();
        assertThat(o.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("markSent → SENT, 에러 클리어")
    void mark_sent() {
        NotificationOutbox o = newOutbox();
        o.markFailed("boom");
        o.markSent();
        assertThat(o.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(o.getSentAt()).isNotNull();
        assertThat(o.getLastError()).isNull();
    }

    @Test
    @DisplayName("실패 누적 — MAX_ATTEMPTS 도달 시 FAILED로 종결, 재시도 예약 없음")
    void max_attempts_terminates() {
        NotificationOutbox o = newOutbox();
        for (int i = 0; i < NotificationOutbox.MAX_ATTEMPTS; i++) {
            o.markFailed("err" + i);
        }
        assertThat(o.getAttemptCount()).isEqualTo(NotificationOutbox.MAX_ATTEMPTS);
        assertThat(o.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(o.getNextAttemptAt()).isNull();
    }

    @Test
    @DisplayName("한도 미만 실패는 PENDING 유지 + 다음 시도 예약")
    void below_max_keeps_pending() {
        NotificationOutbox o = newOutbox();
        o.markFailed("first");
        assertThat(o.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(o.getNextAttemptAt()).isNotNull();
        assertThat(o.getLastError()).isEqualTo("first");
    }
}
