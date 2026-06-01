package com.jes.devlearn.domain.admin.service;

import com.jes.devlearn.domain.admin.dto.response.AdminReportPageResponse;
import com.jes.devlearn.domain.admin.error.AdminErrorCode;
import com.jes.devlearn.domain.report.dto.response.ReportResponse;
import com.jes.devlearn.domain.report.entity.Report;
import com.jes.devlearn.domain.report.entity.ReportStatus;
import com.jes.devlearn.domain.report.entity.ReportTargetType;
import com.jes.devlearn.domain.report.repository.ReportRepository;
import com.jes.devlearn.domain.report.service.ReportTargetRegistry;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final ReportTargetRegistry targetRegistry;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminReportPageResponse list(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        Page<Report> page = reportRepository.findAllForAdmin(status, targetType, pageable);

        Map<Long, String> usernameById = new HashMap<>();
        page.getContent().stream().map(Report::getReporterId).distinct().forEach(uid ->
                userRepository.findById(uid).ifPresent(u -> usernameById.put(uid, u.getUsername())));

        var content = page.getContent().stream()
                .map(r -> ReportResponse.of(r, usernameById.get(r.getReporterId())))
                .toList();

        return new AdminReportPageResponse(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public ReportResponse resolve(Long adminId, Long reportId, boolean deleteTarget, String note) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(AdminErrorCode.REPORT_NOT_FOUND));
        if (!report.isPending()) {
            throw new CustomException(AdminErrorCode.REPORT_ALREADY_RESOLVED);
        }

        if (deleteTarget) {
            targetRegistry.get(report.getTargetType()).deleteTarget(report.getTargetId());
            log.info("[Admin] 신고 처리 — 대상 삭제 {}#{} (reportId={}, adminId={})",
                    report.getTargetType(), report.getTargetId(), reportId, adminId);
        }
        report.resolve(adminId, note);

        String username = userRepository.findById(report.getReporterId()).map(u -> u.getUsername()).orElse(null);
        return ReportResponse.of(report, username);
    }

    @Transactional
    public ReportResponse dismiss(Long adminId, Long reportId, String note) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(AdminErrorCode.REPORT_NOT_FOUND));
        if (!report.isPending()) {
            throw new CustomException(AdminErrorCode.REPORT_ALREADY_RESOLVED);
        }
        report.dismiss(adminId, note);
        log.info("[Admin] 신고 반려 reportId={} (adminId={})", reportId, adminId);

        String username = userRepository.findById(report.getReporterId()).map(u -> u.getUsername()).orElse(null);
        return ReportResponse.of(report, username);
    }
}
