package com.crm.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 320, message = "Email cannot exceed 320 characters")
    String email,

    @NotBlank(message = "Password is required")
    String password

) {}
