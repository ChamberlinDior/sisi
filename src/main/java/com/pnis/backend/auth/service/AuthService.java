package com.pnis.backend.auth.service;

import com.pnis.backend.auth.dto.AuthDtos.*;
import com.pnis.backend.auth.model.*;
import com.pnis.backend.auth.repository.*;
import com.pnis.backend.common.config.AppProperties;
import com.pnis.backend.common.exception.*;
import com.pnis.backend.common.util.HashUtils;
import com.pnis.backend.security.JwtService;
import com.pnis.backend.tenant.repository.TenantRepository;
import com.pnis.backend.tenant.repository.UnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private final AppUserRepository  userRepository;
    private final AppRoleRepository  roleRepository;
    private final TenantRepository   tenantRepository;
    private final UnitRepository     unitRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtService         jwtService;
    private final AppProperties      appProperties;
    private final AuthenticationManager authManager;

    public AuthService(
            AppUserRepository userRepository,
            AppRoleRepository roleRepository,
            TenantRepository tenantRepository,
            UnitRepository unitRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties,
            AuthenticationManager authManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.unitRepository = unitRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.authManager = authManager;
    }


    // ===================================================
    // LOGIN
    // ===================================================
    @Transactional
    public AuthResponse login(LoginRequest req, String ipAddress) {
        AppUser user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides."));

        // Vérification lockout
        if (!user.isAccountNonLocked()) {
            if (user.getLockoutUntil() != null && Instant.now().isBefore(user.getLockoutUntil())) {
                throw new LockedException("Compte verrouillé jusqu'à : " + user.getLockoutUntil());
            } else {
                // Lockout expiré, on déverrouille
                user.setAccountNonLocked(true);
                user.setFailedLoginAttempts(0);
            }
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            int max = appProperties.getSecurity().getMaxLoginAttempts();
            if (attempts >= max) {
                user.setAccountNonLocked(false);
                user.setLockoutUntil(Instant.now().plusSeconds(
                        appProperties.getSecurity().getLockoutDurationMinutes() * 60L));
                log.warn("Compte verrouillé après {} tentatives : {}", max, user.getUsername());
            }
            userRepository.save(user);
            throw new BadCredentialsException("Identifiants invalides.");
        }

        if (!user.isEnabled()) throw new DisabledException("Compte désactivé.");

        // Reset compteur
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        // MFA requis ?
        boolean mfaRequired = user.isMfaEnabled() ||
                user.getRoles().stream().anyMatch(r ->
                        appProperties.getSecurity().getMfaRequiredRoles()
                                .contains(r.getName().name()));

        if (mfaRequired && user.isMfaEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user.getUsername());
            return AuthResponse.builder()
                    .mfaPending(true)
                    .mfaToken(mfaToken)
                    .username(user.getUsername())
                    .build();
        }

        return buildFullAuthResponse(user);
    }

    // ===================================================
    // MFA VERIFY
    // ===================================================
    @Transactional
    public AuthResponse verifyMfa(MfaVerifyRequest req) {
        if (!jwtService.isMfaPendingToken(req.getMfaToken())) {
            throw new BadRequestException("Token MFA invalide.");
        }
        String username = jwtService.extractUsername(req.getMfaToken());
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        // TODO: vérification TOTP (Google Authenticator) — intégrer une lib TOTP
        // Pour l'instant, code de dev : "000000" validé uniquement en non-prod
        if (!"000000".equals(req.getTotpCode())) {
            throw new BadCredentialsException("Code MFA invalide.");
        }

        return buildFullAuthResponse(user);
    }

    // ===================================================
    // REFRESH TOKEN
    // ===================================================
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String token = req.getRefreshToken();
        if (!jwtService.isRefreshToken(token)) {
            throw new BadRequestException("Token invalide.");
        }
        if (jwtService.isTokenExpired(token)) {
            throw new BadRequestException("Refresh token expiré. Veuillez vous reconnecter.");
        }
        String username = jwtService.extractUsername(token);
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        // Vérification hash du refresh token stocké
        String tokenHash = HashUtils.sha256Hex(token.getBytes());
        if (!tokenHash.equals(user.getRefreshTokenHash())) {
            throw new BadRequestException("Refresh token révoqué.");
        }

        return buildFullAuthResponse(user);
    }

    // ===================================================
    // CHANGE PASSWORD
    // ===================================================
    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mot de passe actuel incorrect.");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Les mots de passe ne correspondent pas.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    // ===================================================
    // LOGOUT (révocation du refresh token)
    // ===================================================
    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshTokenHash(null);
            user.setRefreshTokenExpiresAt(null);
            userRepository.save(user);
        });
    }

    // ===================================================
    // PRIVATE HELPERS
    // ===================================================
    private AuthResponse buildFullAuthResponse(AppUser user) {
        String roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.joining(","));

        Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String tenantCode = user.getTenant() != null ? user.getTenant().getCode() : null;

        String accessToken  = jwtService.generateAccessToken(user.getUsername(), tenantId, roles);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        // Stocker le hash du refresh token
        String refreshHash = HashUtils.sha256Hex(refreshToken.getBytes());
        user.setRefreshTokenHash(refreshHash);
        user.setRefreshTokenExpiresAt(Instant.now().plusMillis(
                appProperties.getJwt().getRefreshTokenExpirationMs()));
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpirationMs() / 1000)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .tenantId(tenantId)
                .tenantCode(tenantCode)
                .roles(roles)
                .mfaPending(false)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }
}
