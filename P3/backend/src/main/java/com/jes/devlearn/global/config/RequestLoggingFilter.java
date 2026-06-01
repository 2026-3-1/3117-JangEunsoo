package com.jes.devlearn.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 구조화 요청 로그 + 요청 ID(MDC). 모든 요청에 requestId를 부여하고
 * 응답 시 메서드·경로·상태·소요시간을 한 줄로 남긴다(운영 추적용).
 * 응답 헤더 X-Request-Id로 클라이언트와 로그를 상호 추적 가능.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(HEADER, requestId);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long took = System.currentTimeMillis() - start;
            // actuator/health 등 노이즈는 디버그로
            String uri = request.getRequestURI();
            if (uri.startsWith("/actuator")) {
                log.debug("{} {} -> {} ({}ms)", request.getMethod(), uri, response.getStatus(), took);
            } else {
                log.info("{} {} -> {} ({}ms)", request.getMethod(), uri, response.getStatus(), took);
            }
            MDC.remove(REQUEST_ID);
        }
    }
}
