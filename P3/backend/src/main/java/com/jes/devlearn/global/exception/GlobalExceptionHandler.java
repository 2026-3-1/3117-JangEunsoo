package com.jes.devlearn.global.exception;

import com.jes.devlearn.domain.review.error.ReviewProgressGateException;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // validation exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex){
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[{}] {}", ex.getClass().getSimpleName(), msg);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalApiResponse.fail(HttpStatus.BAD_REQUEST, msg));
    }

    // login authentication error (BadCredentials, InternalAuthenticationServiceException 등 모두 포함)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleAuthentication(AuthenticationException ex){
        log.warn("[{}] {}", ex.getClass().getSimpleName(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(GlobalApiResponse.fail(HttpStatus.UNAUTHORIZED, "username or password is invalid"));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleAccessDenied(AuthorizationDeniedException ex) {
        log.warn("[{}] {}", ex.getClass().getSimpleName(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(GlobalApiResponse.fail(HttpStatus.FORBIDDEN, "not enough right."));
    }

    // endpoint not found
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("[{}] {}", ex.getClass().getSimpleName(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(GlobalApiResponse.fail(HttpStatus.NOT_FOUND, "not found"));
    }

    @ExceptionHandler(ReviewProgressGateException.class)
    public ResponseEntity<GlobalApiResponse<Map<String, Integer>>> handleReviewProgressGate(ReviewProgressGateException ex) {
        log.warn("[{}] {}", ex.getClass().getSimpleName(), ex.getMessage());
        Map<String, Integer> data = Map.of(
                "currentProgressRate", ex.getCurrentProgressRate(),
                "requiredRate", ex.getRequiredRate()
        );
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(GlobalApiResponse.fail(ex.getErrorCode().getStatus(), ex.getMessage(), data));
    }

    // CustomException
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleCustomException(CustomException ex){
        log.warn("[{}] {}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(GlobalApiResponse.fail(ex.getErrorCode().getStatus(), ex.getMessage()));
    }

    // the other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleException(Exception ex){
        log.error("[{}] {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error."));
    }
}
