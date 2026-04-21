package com.jes.devlearn.domain.auth.controller;

import com.jes.devlearn.domain.auth.dto.request.LoginRequestDTO;
import com.jes.devlearn.domain.auth.dto.request.RefreshRequestDTO;
import com.jes.devlearn.domain.auth.dto.request.SignupRequestDTO;
import com.jes.devlearn.domain.auth.dto.response.AuthResponseDTO;
import com.jes.devlearn.domain.auth.service.AuthService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("signup")
    public ResponseEntity<GlobalApiResponse<Void>> signup(
            @Valid @RequestBody SignupRequestDTO dto
    ) {
        authService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalApiResponse.success("Sign-up completed successfully."));
    }

    @PostMapping("login")
    public ResponseEntity<GlobalApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO dto
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(authService.login(dto)));
    }

    @PostMapping("refresh")
    public ResponseEntity<GlobalApiResponse<AuthResponseDTO>> refresh(
            @RequestBody RefreshRequestDTO dto
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(authService.refresh(dto)));
    }

    @PostMapping("logout")
    public ResponseEntity<GlobalApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        authService.logout(userPrincipal);
        return ResponseEntity.ok(GlobalApiResponse.success(" completed successfully."));
    }
}
