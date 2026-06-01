package com.jes.devlearn.domain.notification.sender;

import com.jes.devlearn.domain.notification.entity.NotificationChannel;

public interface NotificationSender {

    NotificationChannel channel();

    /** 발송. 실패 시 예외를 던지면 outbox가 재시도 처리한다. */
    void send(String title, String message);
}
