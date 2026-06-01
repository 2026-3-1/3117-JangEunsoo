package com.jes.devlearn.domain.report.service;

import com.jes.devlearn.domain.report.entity.ReportTargetType;
import com.jes.devlearn.domain.report.error.ReportErrorCode;
import com.jes.devlearn.global.exception.CustomException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportTargetRegistry {

    private final Map<ReportTargetType, ReportTargetResolver> resolvers = new EnumMap<>(ReportTargetType.class);

    public ReportTargetRegistry(List<ReportTargetResolver> resolverBeans) {
        for (ReportTargetResolver r : resolverBeans) {
            resolvers.put(r.supportedType(), r);
        }
    }

    public ReportTargetResolver get(ReportTargetType type) {
        ReportTargetResolver resolver = resolvers.get(type);
        if (resolver == null) {
            // 아직 지원되지 않는 대상 유형
            throw new CustomException(ReportErrorCode.REPORT_TARGET_NOT_FOUND);
        }
        return resolver;
    }
}
