package com.crm.auth_service.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.crm.auth_service.dto.LoginRequest;
import com.crm.auth_service.dto.LoginResponse;
import com.crm.auth_service.dto.RegisterRequest;
import com.crm.auth_service.entity.Role;
import com.crm.auth_service.entity.User;
import com.crm.auth_service.entity.UserStatus;
import com.crm.auth_service.repository.RoleRepository;
import com.crm.auth_service.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    // ==========================================
    // REGISTER USER
    // ==========================================

    @Transactional
    public void register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        String hashedPassword = passwordEncoder.encode(
                request.password()
        );

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Default role ROLE_USER not found"
                ));

        User user = new User(
                request.firstName().trim(),
                request.lastName() == null
                        ? null
                        : request.lastName().trim(),
                email,
                hashedPassword
        );

        user.addRole(defaultRole);

        userRepository.save(user);
    }

    // ==========================================
    // LOGIN USER
    // ==========================================

    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                    "Account is not active"
            );
        }

        // Generate access token
        String accessToken = jwtService.generateAccessToken(user);

        Instant accessTokenExpiresAt =
                jwtService.getExpirationTime();

        // Generate refresh token
        RefreshTokenService.RefreshTokenResult refreshResult =
                refreshTokenService.createRefreshToken(user);

        return new LoginResponse(
                accessToken,
                refreshResult.refreshToken(),
                "Bearer",
                accessTokenExpiresAt,
                refreshResult.expiresAt()
        );
    }

    // ==========================================
    // LOGOUT USER
    // ==========================================

    @Transactional
    public void logout(String refreshToken) {

        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    // ==========================================
    // EMAIL NORMALIZATION
    // ==========================================

    private String normalizeEmail(String email) {

        return email.trim().toLowerCase(Locale.ROOT);
    }
}	