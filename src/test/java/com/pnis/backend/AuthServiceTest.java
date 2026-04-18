package com.pnis.backend.auth;

import com.pnis.backend.auth.dto.AuthDtos.*;
import com.pnis.backend.auth.model.*;
import com.pnis.backend.auth.repository.*;
import com.pnis.backend.auth.service.AuthService;
import com.pnis.backend.common.config.AppProperties;
import com.pnis.backend.security.JwtService;
import com.pnis.backend.tenant.repository.TenantRepository;
import com.pnis.backend.tenant.repository.UnitRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AppUserRepository  userRepo;
    @Mock AppRoleRepository  roleRepo;
    @Mock TenantRepository   tenantRepo;
    @Mock UnitRepository     unitRepo;
    @Mock PasswordEncoder    passwordEncoder;
    @Mock JwtService         jwtService;
    @Mock AppProperties      appProperties;
    @Mock AuthenticationManager authManager;

    @InjectMocks AuthService authService;

    private AppUser buildUser() {
        AppUser u = new AppUser();
        u.setUsername("testuser");
        u.setEmail("test@pnis.ga");
        u.setPassword("$2a$12$hashed");
        u.setEnabled(true);
        u.setAccountNonLocked(true);
        u.setFailedLoginAttempts(0);
        u.setRoles(Set.of());
        return u;
    }

    @Test
    void login_succeeds_with_valid_credentials() {
        AppUser user = buildUser();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        AppProperties.Security sec = new AppProperties.Security();
        sec.setMfaRequiredRoles(java.util.List.of());

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "$2a$12$hashed")).thenReturn(true);
        when(appProperties.getSecurity()).thenReturn(sec);
        when(appProperties.getJwt()).thenReturn(jwt);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password");

        AuthResponse result = authService.login(req, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.isMfaPending()).isFalse();
    }

    @Test
    void login_fails_with_wrong_password() {
        AppUser user = buildUser();
        AppProperties.Security sec = new AppProperties.Security();
        sec.setMaxLoginAttempts(5);
        sec.setLockoutDurationMinutes(30);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$12$hashed")).thenReturn(false);
        when(appProperties.getSecurity()).thenReturn(sec);

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_locks_account_after_max_attempts() {
        AppUser user = buildUser();
        user.setFailedLoginAttempts(4);
        AppProperties.Security sec = new AppProperties.Security();
        sec.setMaxLoginAttempts(5);
        sec.setLockoutDurationMinutes(30);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(appProperties.getSecurity()).thenReturn(sec);

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("bad");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(user.isAccountNonLocked()).isFalse();
    }
}
