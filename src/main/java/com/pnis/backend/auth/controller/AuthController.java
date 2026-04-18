package com.pnis.backend.auth.controller;

import com.pnis.backend.auth.dto.AuthDtos.*;
import com.pnis.backend.auth.service.AuthService;
import com.pnis.backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentification", description = "Login, MFA, refresh token, logout, changement de mot de passe")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentifie un utilisateur et retourne les tokens JWT.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        AuthResponse response = authService.login(req, ip);
        return ResponseEntity.ok(ApiResponse.ok(response, "Connexion réussie."));
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Vérification MFA", description = "Vérifie le code TOTP et retourne les tokens finaux.")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyMfa(req), "MFA validé."));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(req)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion – révocation du refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails user) {
        authService.logout(user.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent("Déconnexion effectuée."));
    }

    @PutMapping("/password/change")
    @Operation(summary = "Changement de mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(user.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.noContent("Mot de passe modifié avec succès."));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
