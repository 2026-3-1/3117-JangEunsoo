package com.jes.devlearn.domain.review.service;

import com.jes.devlearn.domain.report.entity.ReportTargetType;
import com.jes.devlearn.domain.report.service.ReportTargetResolver;
import com.jes.devlearn.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReviewReportTargetResolver implements ReportTargetResolver {

    private final ReviewRepository reviewRepository;

    @Override
    public ReportTargetType supportedType() {
        return ReportTargetType.REVIEW;
    }

    @Override
    public Optional<Long> findAuthorId(Long targetId) {
        return reviewRepository.findById(targetId).map(r -> r.getUserId());
    }

    @Override
    public void deleteTarget(Long targetId) {
        reviewRepository.findById(targetId).ifPresent(reviewRepository::delete);
    }
}
