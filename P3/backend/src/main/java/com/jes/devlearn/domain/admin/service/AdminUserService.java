package com.jes.devlearn.domain.admin.service;

import com.jes.devlearn.domain.admin.dto.response.AdminUserPageResponse;
import com.jes.devlearn.domain.admin.dto.response.AdminUserResponse;
import com.jes.devlearn.domain.admin.error.AdminErrorCode;
import com.jes.devlearn.domain.instructor.entity.InstructorProfile;
import com.jes.devlearn.domain.instructor.repository.InstructorProfileRepository;
import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.error.UserErrorCode;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final InstructorProfileRepository instructorProfileRepository;

    @Transactional(readOnly = true)
    public AdminUserPageResponse list(Role role, String keyword, Pageable pageable) {
        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<User> page = userRepository.searchForAdmin(role, normalized, pageable);
        return AdminUserPageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse changeRole(Long adminId, Long userId, Role newRole) {
        if (adminId.equals(userId)) {
            throw new CustomException(AdminErrorCode.CANNOT_MODIFY_SELF);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 다른 관리자 계정의 강등/변경은 금지 (권한 회수는 DB 직접 작업으로만)
        if (user.isAdmin() || newRole == Role.ADMIN) {
            throw new CustomException(AdminErrorCode.CANNOT_DEMOTE_ADMIN);
        }

        user.changeRole(newRole);

        // STUDENT → INSTRUCTOR 승급 시 강사 프로필이 없으면 생성
        if (newRole == Role.INSTRUCTOR && instructorProfileRepository.findByUserId(userId).isEmpty()) {
            InstructorProfile profile = new InstructorProfile(user, user.getUsername(), null, null, null);
            instructorProfileRepository.save(profile);
        }

        log.info("[Admin] userId={} 역할 변경 → {} (by adminId={})", userId, newRole, adminId);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse setActive(Long adminId, Long userId, boolean active) {
        if (adminId.equals(userId)) {
            throw new CustomException(AdminErrorCode.CANNOT_MODIFY_SELF);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (user.isAdmin()) {
            throw new CustomException(AdminErrorCode.CANNOT_DEMOTE_ADMIN);
        }

        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        log.info("[Admin] userId={} 활성상태 변경 → active={} (by adminId={})", userId, active, adminId);
        return AdminUserResponse.from(user);
    }
}
