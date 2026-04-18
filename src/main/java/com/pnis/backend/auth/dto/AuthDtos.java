package com.pnis.backend.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public final class AuthDtos {

    private AuthDtos() {}

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        private String username;
        @NotBlank(message = "Le mot de passe est obligatoire.")
        private String password;
    }

    @Getter @Setter
    public static class MfaVerifyRequest {
        @NotBlank(message = "Le token MFA temporaire est obligatoire.")
        private String mfaToken;
        @NotBlank(message = "Le code TOTP est obligatoire.")
        @Size(min = 6, max = 8)
        private String totpCode;
    }

    @Getter @Setter
    public static class RefreshTokenRequest {
        @NotBlank(message = "Le refresh token est obligatoire.")
        private String refreshToken;
    }

    @Getter @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private String username;
        private String fullName;
        private Long tenantId;
        private String tenantCode;
        private String roles;
        private boolean mfaPending;
        private String mfaToken;
        private boolean mustChangePassword;
    }

    @Getter @Setter
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank
        @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères.")
        private String newPassword;
        @NotBlank
        private String confirmPassword;
    }

    @Getter @Setter
    public static class CreateUserRequest {
        @NotBlank @Size(min = 3, max = 100)
        private String username;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 12)
        private String password;
        @NotBlank @Size(max = 200)
        private String fullName;
        private String position;
        private String phoneNumber;
        private Long tenantId;
        private Long unitId;
        private java.util.Set<String> roles;
        private String maxClassification;
    }

    @Getter @Builder
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String position;
        private String phoneNumber;
        private Long tenantId;
        private String tenantCode;
        private Long unitId;
        private String unitName;
        private java.util.Set<String> roles;
        private String maxClassification;
        private boolean enabled;
        private boolean mfaEnabled;
        private boolean mustChangePassword;
        private java.time.Instant lastLoginAt;
        private java.time.Instant createdAt;
    }

    @Getter @Setter
    public static class UpdateUserRequest {
        private String fullName;
        private String position;
        private String phoneNumber;
        private Long unitId;
        private java.util.Set<String> roles;
        private String maxClassification;
        private Boolean enabled;
    }
}
