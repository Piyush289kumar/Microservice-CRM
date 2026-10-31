package com.crm.auth_service.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant expiresAt,
        Instant refreshTokenExpiresAt
) {
}	