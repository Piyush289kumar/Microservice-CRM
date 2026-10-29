package com.crm.auth_service.dto;

import java.time.Instant;

public record LoginResponse(

    String accessToken,

    String tokenType,

    Instant expiresAt

) {}