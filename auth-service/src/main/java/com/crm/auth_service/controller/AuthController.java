package com.crm.auth_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.crm.auth_service.dto.ApiResponse;
import com.crm.auth_service.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {

        // Call your AuthService here to register the user.

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
}