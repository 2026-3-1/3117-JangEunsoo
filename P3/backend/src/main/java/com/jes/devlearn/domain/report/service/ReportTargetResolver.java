package com.jes.devlearn.domain.report.service;

import com.jes.devlearn.domain.report.entity.ReportTargetType;

import java.util.Optional;

/**
 * 신고 대상(리뷰/Q&A 등)에 대한 조회·작성자 식별·삭제를 도메인별로 위임하기 위한 SPI.
 * 각 도메인이 구현체를 빈으로 등록하면 ReportService가 targetType으로 디스패치한다.
 */
public interface ReportTargetResolver {

    ReportTargetType supportedType();

    /** 대상이 존재하면 작성자 userId를 반환, 없으면 empty */
    Optional<Long> findAuthorId(Long targetId);

    /** 관리자 조치로 대상을 삭제. 이미 없으면 무시 */
    void deleteTarget(Long targetId);
}
