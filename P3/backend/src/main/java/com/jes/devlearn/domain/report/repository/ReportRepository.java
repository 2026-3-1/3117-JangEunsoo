package com.jes.devlearn.domain.report.repository;

import com.jes.devlearn.domain.report.entity.Report;
import com.jes.devlearn.domain.report.entity.ReportStatus;
import com.jes.devlearn.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(
            Long reporterId, ReportTargetType targetType, Long targetId);

    @Query("SELECT r FROM Report r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:targetType IS NULL OR r.targetType = :targetType)")
    Page<Report> findAllForAdmin(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            Pageable pageable
    );

    long countByStatus(ReportStatus status);
}
