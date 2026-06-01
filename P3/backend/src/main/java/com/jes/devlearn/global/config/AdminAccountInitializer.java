package com.jes.devlearn.global.config;

import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부트스트랩: 환경변수로 지정한 관리자 계정이 없으면 생성한다.
 * 회원가입 경로(ADMIN_SIGNUP_FORBIDDEN)로는 ADMIN을 만들 수 없으므로,
 * 운영 환경에서는 이 초기화로만 관리자 계정을 부여한다.
 *
 * 환경변수:
 *   ADMIN_USERNAME (기본 admin01)
 *   ADMIN_PASSWORD (미설정 시 생성 생략 — 운영에서 임의 비밀번호 노출 방지)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin01}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.info("[AdminInit] ADMIN_PASSWORD 미설정 — 관리자 계정 자동 생성을 생략합니다.");
            return;
        }
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("[AdminInit] 관리자 계정 '{}' 이미 존재 — 생성 생략.", adminUsername);
            return;
        }
        User admin = new User(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN);
        userRepository.save(admin);
        log.info("[AdminInit] 관리자 계정 '{}' 생성 완료.", adminUsername);
    }
}
