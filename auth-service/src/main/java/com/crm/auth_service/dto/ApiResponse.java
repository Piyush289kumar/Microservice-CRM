package com.crm.auth_service.dto;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        Instant timestamp,
        String path,
        Map<String, String> errors
) {

    public static <T> ApiResponse<T> success(
            int status,
            String message,
            T data,
            String path
    ) {
        return new ApiResponse<>(
                true,
                status,
                message,
                data,
                Instant.now(),
                path,
                null
        );
    }

    public static ApiResponse<Void> error(
            int status,
            String message,
            String path
    ) {
        return new ApiResponse<>(
                false,
                status,
                message,
                null,
                Instant.now(),
                path,
                null
        );
    }

    public static ApiResponse<Void> validationError(
            int status,
            String message,
            String path,
            Map<String, String> errors
    ) {
        return new ApiResponse<>(
                false,
                status,
                message,
                null,
                Instant.now(),
                path,
                errors
        );
    }
}