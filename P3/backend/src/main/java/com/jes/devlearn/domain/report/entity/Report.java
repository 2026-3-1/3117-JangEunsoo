package com.jes.devlearn.domain.report.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_report_reporter_target",
                columnNames = {"reporter_id", "target_type", "target_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "resolver_id")
    private Long resolverId;

    @Column(name = "resolver_note", length = 500)
    private String resolverNote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Report(Long reporterId, ReportTargetType targetType, Long targetId, String reason) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
    }

    public void resolve(Long resolverId, String note) {
        this.status = ReportStatus.RESOLVED;
        this.resolverId = resolverId;
        this.resolverNote = note;
    }

    public void dismiss(Long resolverId, String note) {
        this.status = ReportStatus.DISMISSED;
        this.resolverId = resolverId;
        this.resolverNote = note;
    }

    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }
}
