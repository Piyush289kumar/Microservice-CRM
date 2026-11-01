package com.crm.auth_service.controller;


import java.util.UUID;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.crm.auth_service.dto.ApiResponse;
import com.crm.auth_service.entity.User;
import com.crm.auth_service.exception.ResourceNotFoundException;
import com.crm.auth_service.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ==========================================
    // GET CURRENT AUTHENTICATED USER
    // GET /api/users/me
    // ==========================================

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.getRoles()
                        .stream()
                        .map(role -> role.getName())
                        .toList(),
                user.getCreatedAt()
        );

        ApiResponse<UserProfileResponse> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "User profile retrieved successfully",
                        profile,
                        "/api/users/me"
                );

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // USER PROFILE RESPONSE DTO
    // ==========================================

    public record UserProfileResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String status,
            boolean emailVerified,
            List<String> roles,
            java.time.Instant createdAt
    ) {}
}
