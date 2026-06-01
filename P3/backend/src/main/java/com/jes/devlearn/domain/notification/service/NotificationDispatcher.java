package com.jes.devlearn.domain.notification.service;

import com.jes.devlearn.domain.notification.entity.NotificationOutbox;
import com.jes.devlearn.domain.notification.entity.NotificationStatus;
import com.jes.devlearn.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 아웃박스 디스패처(배치/스케줄러). 일정 주기로 발송 대상 row를 읽어
 * NotificationOutboxProcessor에 단건 위임한다. 각 건은 독립 트랜잭션이라
 * 한 건의 실패가 배치 전체를 막지 않으며, 실패 시 지수 백오프로 재시도된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private static final int BATCH_SIZE = 50;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxProcessor processor;

    @Scheduled(fixedDelayString = "${app.notification.dispatch-interval-ms:15000}")
    public void dispatch() {
        List<NotificationOutbox> batch = outboxRepository.findDispatchable(
                NotificationStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) return;

        int sent = 0, failed = 0;
        for (NotificationOutbox row : batch) {
            boolean ok = processor.processOne(row.getId());
            if (ok) sent++; else failed++;
        }
        log.info("[NotificationDispatcher] 배치 처리: 성공 {} · 실패 {}", sent, failed);
    }
}
