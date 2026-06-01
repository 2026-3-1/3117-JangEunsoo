package com.jes.devlearn.domain.notification.repository;

import com.jes.devlearn.domain.notification.entity.NotificationOutbox;
import com.jes.devlearn.domain.notification.entity.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    boolean existsByDedupKey(String dedupKey);

    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = :status " +
           "AND (n.nextAttemptAt IS NULL OR n.nextAttemptAt <= :now) ORDER BY n.id ASC")
    List<NotificationOutbox> findDispatchable(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    long countByStatus(NotificationStatus status);
}
