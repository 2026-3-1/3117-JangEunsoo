package com.jes.devlearn.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalApiResponse<T> {
    private boolean success;
    private int status;
    private String message;
    private T data;

    // 성공
    public static <T> GlobalApiResponse<T> success() {
        return GlobalApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("요청 성공")
                .build();
    }

    public static <T> GlobalApiResponse<T> success(String message) {
        return GlobalApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message(message)
                .build();
    }

    public static <T> GlobalApiResponse<T> success(T data) {
        return GlobalApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("요청 성공")
                .data(data)
                .build();
    }

    public static <T> GlobalApiResponse<T> success(T data, String message) {
        return GlobalApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }

    // 실패
    public static <T> GlobalApiResponse<T> fail(HttpStatus status, String message) {
        return GlobalApiResponse.<T>builder()
                .success(false)
                .status(status.value())
                .message(message)
                .build();
    }

    public static <T> GlobalApiResponse<T> fail(int status, String message) {
        return GlobalApiResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .build();
    }
}