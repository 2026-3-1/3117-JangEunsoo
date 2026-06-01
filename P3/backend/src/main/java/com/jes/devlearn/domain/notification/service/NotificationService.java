package com.jes.devlearn.domain.notification.service;

import com.jes.devlearn.domain.notification.entity.NotificationChannel;
import com.jes.devlearn.domain.notification.entity.NotificationOutbox;
import com.jes.devlearn.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 적재 진입점. 비즈니스 로직은 enqueue만 호출하고 실제 발송은 스케줄러가 담당한다.
 * dedupKey 기반 멱등 적재 — 같은 이벤트가 두 번 들어와도 한 번만 큐잉된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationOutboxRepository outboxRepository;

    @Value("${app.notification.default-channel:DISCORD}")
    private NotificationChannel defaultChannel;

    /**
     * 멱등 적재. 호출하는 비즈니스 트랜잭션과 분리(REQUIRES_NEW)하여,
     * 알림 적재 실패가 본 비즈니스 트랜잭션을 롤백시키지 않게 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String dedupKey, String eventType, String title, String message) {
        try {
            if (outboxRepository.existsByDedupKey(dedupKey)) {
                return; // 멱등: 이미 적재됨
            }
            outboxRepository.save(new NotificationOutbox(
                    dedupKey, eventType, defaultChannel, title, message));
        } catch (DataIntegrityViolationException e) {
            // 동시 적재로 UNIQUE 충돌 — 멱등 처리상 무시
            log.debug("[Notification] 중복 dedupKey 적재 무시: {}", dedupKey);
        } catch (Exception e) {
            // 알림 적재 실패는 비즈니스 흐름을 막지 않는다
            log.warn("[Notification] enqueue 실패 dedupKey={}: {}", dedupKey, e.getMessage());
        }
    }
}
