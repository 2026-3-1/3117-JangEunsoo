package com.jes.devlearn.domain.notification.entity;

public enum NotificationStatus {
    PENDING,   // 발송 대기
    SENT,      // 발송 성공
    FAILED,    // 재시도 한도 초과로 실패 처리
    DEAD       // 영구 실패 (재시도 안 함)
}
