package com.jes.devlearn.domain.auth.service;

import com.jes.devlearn.domain.auth.entity.RefreshToken;
import com.jes.devlearn.domain.auth.error.AuthErrorCode;
import com.jes.devlearn.domain.auth.repository.RefreshTokenRepository;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.global.exception.CustomException;
import com.jes.devlearn.global.security.jwt.TokenConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void deleteByUserId(Long id) {
        refreshTokenRepository.deleteByUserId(id);
    }

    @Transactional
    public void saveOrUpdate(User user, String token) {
        LocalDateTime expiry = LocalDateTime.now().plus(TokenConstants.REFRESH_TOKEN_VALIDITY, ChronoUnit.MILLIS);

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        rt -> rt.update(token, expiry),
                        () -> refreshTokenRepository.save(
                                new RefreshToken(
                                        user,
                                        token,
                                        expiry
                                )
                        )
                );
    }

    @Transactional
    public RefreshToken getValidRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException(AuthErrorCode.TOKEN_NOT_FOUND));

        if (refreshToken.isExpired()) {
            deleteByUserId(refreshToken.getUser().getId());
            throw new CustomException(AuthErrorCode.TOKEN_EXPIRED);
        }

        return refreshToken;
    }
}
