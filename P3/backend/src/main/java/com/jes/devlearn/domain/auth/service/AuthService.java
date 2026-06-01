package com.jes.devlearn.domain.auth.service;

import com.jes.devlearn.domain.auth.dto.request.LoginRequestDTO;
import com.jes.devlearn.domain.auth.dto.request.RefreshRequestDTO;
import com.jes.devlearn.domain.auth.dto.request.SignupRequestDTO;
import com.jes.devlearn.domain.auth.dto.response.AuthResponseDTO;
import com.jes.devlearn.domain.auth.entity.RefreshToken;
import com.jes.devlearn.domain.auth.error.AuthErrorCode;
import com.jes.devlearn.domain.instructor.entity.InstructorProfile;
import com.jes.devlearn.domain.instructor.repository.InstructorProfileRepository;
import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.error.UserErrorCode;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import com.jes.devlearn.global.security.UserPrincipal;
import com.jes.devlearn.global.security.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenService refreshTokenService;

    private final UserRepository userRepository;
    private final InstructorProfileRepository instructorProfileRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    @Transactional
    public void signup(SignupRequestDTO dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new CustomException(UserErrorCode.DUPLICATE_USERNAME);
        }

        Role role = dto.role() == null ? Role.STUDENT : dto.role();
        // 권한 상승 방지: ADMIN 계정은 회원가입으로 생성 불가 (seed/DB 직접 부여만)
        if (role == Role.ADMIN) {
            throw new CustomException(AuthErrorCode.ADMIN_SIGNUP_FORBIDDEN);
        }
        User user = new User(dto.username(), passwordEncoder.encode(dto.password()), role);
        userRepository.save(user);

        if (role == Role.INSTRUCTOR) {
            String displayName = (dto.displayName() == null || dto.displayName().isBlank())
                    ? dto.username()
                    : dto.displayName();
            InstructorProfile profile = new InstructorProfile(
                    user, displayName, dto.bio(), dto.careerYears(), null
            );
            instructorProfileRepository.save(profile);
        }
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
        );

        UserPrincipal userPrincipal = Objects.requireNonNull((UserPrincipal) authentication.getPrincipal());

        User user = userRepository.findById(userPrincipal.getUserId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String accessToken = tokenProvider.createToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());

        refreshTokenService.saveOrUpdate(user, refreshToken);

        return new AuthResponseDTO(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponseDTO refresh(RefreshRequestDTO dto) {
        if (!tokenProvider.validateToken(dto.refreshToken())) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        RefreshToken rf = refreshTokenService.getValidRefreshToken(dto.refreshToken());

        User user = rf.getUser();
        String accessToken = tokenProvider.createToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());

        refreshTokenService.saveOrUpdate(user, refreshToken);

        return new AuthResponseDTO(accessToken, refreshToken);
    }

    @Transactional
    public void logout(UserPrincipal userPrincipal) {
        refreshTokenService.deleteByUserId(userPrincipal.getUserId());
    }
}
