package com.crm.auth_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.crm.auth_service.dto.ApiResponse;
import com.crm.auth_service.dto.LoginRequest;
import com.crm.auth_service.dto.LoginResponse;
import com.crm.auth_service.dto.LogoutRequest;
import com.crm.auth_service.dto.RegisterRequest;
import com.crm.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // Constructor injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==========================================
    // REGISTER USER
    // POST /api/auth/register
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        authService.register(request);

        ApiResponse<Void> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "User registered successfully",
                null,
                httpRequest.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==========================================
    // LOGIN USER
    // POST /api/auth/login
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        LoginResponse loginResponse = authService.login(request);

        ApiResponse<LoginResponse> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Login successful",
                loginResponse,
                httpRequest.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
    
    
 // ==========================================
    // LOGOUT USER
    // POST /api/auth/logout
    // ==========================================
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletRequest httpRequest) {

        authService.logout(request.refreshToken());

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Logout successful",
                        null,
                        httpRequest.getRequestURI()
                )
        );
    }
}