package com.jes.devlearn.domain.notification.service;

import com.jes.devlearn.domain.notification.entity.NotificationChannel;
import com.jes.devlearn.domain.notification.entity.NotificationOutbox;
import com.jes.devlearn.domain.notification.entity.NotificationStatus;
import com.jes.devlearn.domain.notification.repository.NotificationOutboxRepository;
import com.jes.devlearn.domain.notification.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 아웃박스 단건 발송 처리기. 디스패처(스케줄러)와 분리된 빈으로 두어
 * REQUIRES_NEW 트랜잭션이 프록시를 통해 정상 적용되도록 한다(자기호출 회피).
 */
@Slf4j
@Component
public class NotificationOutboxProcessor {

    private final NotificationOutboxRepository outboxRepository;
    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);
    private final NotificationSender fallback;

    public NotificationOutboxProcessor(NotificationOutboxRepository outboxRepository,
                                       List<NotificationSender> senderBeans) {
        this.outboxRepository = outboxRepository;
        for (NotificationSender s : senderBeans) {
            senders.put(s.channel(), s);
        }
        this.fallback = senders.getOrDefault(NotificationChannel.LOG, senderBeans.get(0));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processOne(Long outboxId) {
        NotificationOutbox row = outboxRepository.findById(outboxId).orElse(null);
        if (row == null || row.getStatus() != NotificationStatus.PENDING) return false;

        NotificationSender sender = senders.getOrDefault(row.getChannel(), fallback);
        try {
            sender.send(row.getTitle(), row.getMessage());
            row.markSent();
            return true;
        } catch (Exception e) {
            row.markFailed(e.getMessage());
            log.warn("[Notification] 발송 실패 id={} attempt={}: {}",
                    row.getId(), row.getAttemptCount(), e.getMessage());
            return false;
        }
    }
}
