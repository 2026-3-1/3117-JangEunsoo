package com.jes.devlearn.domain.notification.sender;

import com.jes.devlearn.domain.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기본 채널. 외부 연동 없이 구조화 로그로만 남긴다.
 * Discord webhook이 미설정인 환경의 fallback 역할도 겸한다.
 */
@Slf4j
@Component
public class LogNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.LOG;
    }

    @Override
    public void send(String title, String message) {
        log.info("[Notification] {} | {}", title, message);
    }
}
