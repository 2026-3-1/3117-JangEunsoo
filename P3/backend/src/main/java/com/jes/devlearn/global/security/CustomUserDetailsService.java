package com.jes.devlearn.global.security;

import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.error.UserErrorCode;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserPrincipal loadUserByUsername(String username) throws CustomException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return new UserPrincipal(user);
    }

    public UserPrincipal loadUserById(String userId) throws CustomException {
        return loadUserById(userId, null);
    }

    public UserPrincipal loadUserById(String userId, Role tokenRole) throws CustomException {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return new UserPrincipal(user, tokenRole);
    }
}
