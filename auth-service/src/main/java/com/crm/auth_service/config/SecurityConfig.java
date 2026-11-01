package com.crm.auth_service.config;

import java.io.IOException;
import tools.jackson.databind.ObjectMapper;
import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.crm.auth_service.dto.ApiResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // PASSWORD ENCODER
    // ==========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ==========================================
    // JWT DECODER - HS256
    // ==========================================

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "crm-auth-service"
                )
        );

        return decoder;
    }

    // ==========================================
    // JWT AUTHORITIES CONVERTER
    // ==========================================

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");

        // JWT roles already contain ROLE_ prefix
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    // ==========================================
    // 401 - UNAUTHORIZED HANDLER
    // ==========================================

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {

        return (request, response, exception) -> {

            writeErrorResponse(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        };
    }

    // ==========================================
    // 403 - FORBIDDEN HANDLER
    // ==========================================

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {

        return (request, response, exception) -> {

            writeErrorResponse(
                    response,
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        };
    }

    // ==========================================
    // COMMON JSON ERROR WRITER
    // ==========================================

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {

        response.setStatus(status.value());

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );
        
        ApiResponse<Void> body = ApiResponse.error(
                status.value(),
                message,
                ""
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }

    // ==========================================
    // SECURITY FILTER CHAIN
    // ==========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout"
                ).permitAll()

                // Health checks
                .requestMatchers(
                    "/actuator/health"
                ).permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter
                    )
                )
                .authenticationEntryPoint(
                    authenticationEntryPoint
                )
                .accessDeniedHandler(
                    accessDeniedHandler
                )
            )

            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    authenticationEntryPoint
                )
                .accessDeniedHandler(
                    accessDeniedHandler
                )
            );

        return http.build();
    }
}