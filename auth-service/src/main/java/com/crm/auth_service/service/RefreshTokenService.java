package com.crm.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.crm.auth_service.entity.RefreshToken;
import com.crm.auth_service.entity.User;
import com.crm.auth_service.entity.UserStatus;
import com.crm.auth_service.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    // ==========================================
    // GENERATE REFRESH TOKEN
    // ==========================================

    @Transactional
    public RefreshTokenResult createRefreshToken(User user) {

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                    "Account is not active"
            );
        }

        String rawToken = generateSecureToken();

        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now()
                .plusSeconds(refreshTokenExpiration);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return new RefreshTokenResult(
                rawToken,
                expiresAt
        );
    }

    // ==========================================
    // ROTATE REFRESH TOKEN
    // ==========================================

    @Transactional
    public RotatedTokenResult rotateRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException(
                    "Invalid refresh token"
            );
        }

        String oldTokenHash = hashToken(rawToken);

        RefreshToken existingToken = refreshTokenRepository
                .findByTokenHashForUpdate(oldTokenHash)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid refresh token"
                ));

        // Validate token
        if (!existingToken.isValid()) {
            throw new BadCredentialsException(
                    "Refresh token expired or revoked"
            );
        }

        User user = existingToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                    "Account is not active"
            );
        }

        // Generate replacement token
        String newRawToken = generateSecureToken();

        String newTokenHash = hashToken(newRawToken);

        Instant newExpiresAt = Instant.now()
                .plusSeconds(refreshTokenExpiration);

        // Revoke old token and link replacement
        existingToken.revoke(newTokenHash);

        // Save new refresh token
        RefreshToken newRefreshToken = new RefreshToken(
                user,
                newTokenHash,
                newExpiresAt
        );

        refreshTokenRepository.save(newRefreshToken);

        // Generate new access token
        String newAccessToken =
                jwtService.generateAccessToken(user);

        return new RotatedTokenResult(
                newAccessToken,
                newRawToken,
                newExpiresAt
        );
    }

    // ==========================================
    // REVOKE SINGLE REFRESH TOKEN
    // ==========================================

    @Transactional
    public void revokeRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(token -> {

                    if (!token.isRevoked()) {
                        token.revoke(null);
                    }
                });
    }

    // ==========================================
    // REVOKE ALL USER SESSIONS
    // ==========================================

    @Transactional
    public void revokeAllUserTokens(User user) {

        refreshTokenRepository.revokeAllByUserId(
                user.getId()
        );
    }

    // ==========================================
    // GENERATE SECURE RANDOM TOKEN
    // ==========================================

    private String generateSecureToken() {

        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // ==========================================
    // HASH REFRESH TOKEN USING SHA-256
    // ==========================================

    private String hashToken(String rawToken) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return java.util.HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    ex
            );
        }
    }

    // ==========================================
    // REFRESH TOKEN RESULT
    // ==========================================

    public record RefreshTokenResult(
            String refreshToken,
            Instant expiresAt
    ) {}

    // ==========================================
    // ROTATED TOKEN RESULT
    // ==========================================

    public record RotatedTokenResult(
            String accessToken,
            String refreshToken,
            Instant refreshTokenExpiresAt
    ) {}
}