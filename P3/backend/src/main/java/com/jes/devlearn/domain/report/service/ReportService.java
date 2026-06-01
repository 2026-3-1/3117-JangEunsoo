package com.jes.devlearn.domain.report.service;

import com.jes.devlearn.domain.report.dto.request.ReportCreateRequest;
import com.jes.devlearn.domain.report.dto.response.ReportResponse;
import com.jes.devlearn.domain.report.entity.Report;
import com.jes.devlearn.domain.report.error.ReportErrorCode;
import com.jes.devlearn.domain.report.repository.ReportRepository;
import com.jes.devlearn.domain.notification.service.NotificationService;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportTargetRegistry targetRegistry;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReportResponse create(Long reporterId, ReportCreateRequest req) {
        // 대상 존재 확인 + 작성자 식별 (지원되지 않는 유형이면 REPORT_TARGET_NOT_FOUND)
        Optional<Long> authorId = targetRegistry.get(req.targetType()).findAuthorId(req.targetId());
        if (authorId.isEmpty()) {
            throw new CustomException(ReportErrorCode.REPORT_TARGET_NOT_FOUND);
        }
        if (authorId.get().equals(reporterId)) {
            throw new CustomException(ReportErrorCode.CANNOT_REPORT_OWN);
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, req.targetType(), req.targetId())) {
            throw new CustomException(ReportErrorCode.DUPLICATE_REPORT);
        }

        Report report = reportRepository.save(
                new Report(reporterId, req.targetType(), req.targetId(), req.reason()));
        log.info("[Report] reporterId={} {}#{} 신고 접수", reporterId, req.targetType(), req.targetId());

        // 관리자 채널 알림
        notificationService.enqueue(
                "report:" + report.getId(), "REPORT_CREATED", "신고 접수(관리자)",
                String.format("새 신고 접수: %s #%d — 사유: %s",
                        req.targetType(), req.targetId(), req.reason()));

        String username = userRepository.findById(reporterId).map(u -> u.getUsername()).orElse(null);
        return ReportResponse.of(report, username);
    }
}
