package com.pnis.backend.auth.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.auth.dto.AuthDtos.*;
import com.pnis.backend.auth.model.*;
import com.pnis.backend.auth.repository.*;
import com.pnis.backend.common.config.AppProperties;
import com.pnis.backend.common.enums.ClassificationLevel;
import com.pnis.backend.common.exception.*;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.tenant.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final AppUserRepository  userRepository;
    private final AppRoleRepository  roleRepository;
    private final TenantRepository   tenantRepository;
    private final UnitRepository     unitRepository;
    private final PasswordEncoder    passwordEncoder;
    private final AuditService       auditService;

    public UserService(
            AppUserRepository userRepository,
            AppRoleRepository roleRepository,
            TenantRepository tenantRepository,
            UnitRepository unitRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.unitRepository = unitRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }


    // ===================================================
    // CRÉATION
    // ===================================================
    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new ConflictException("Le nom d'utilisateur '" + req.getUsername() + "' est déjà utilisé.");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("L'adresse email '" + req.getEmail() + "' est déjà utilisée.");

        AppUser user = new AppUser();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setPosition(req.getPosition());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setMustChangePassword(true);
        user.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());

        if (req.getTenantId() != null) {
            user.setTenant(tenantRepository.findById(req.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", req.getTenantId())));
        }
        if (req.getUnitId() != null) {
            user.setUnit(unitRepository.findById(req.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", req.getUnitId())));
        }

        if (req.getRoles() != null && !req.getRoles().isEmpty()) {
            Set<AppRole> roles = req.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(RoleName.valueOf(roleName))
                            .orElseThrow(() -> new ResourceNotFoundException("Role", roleName)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        if (req.getMaxClassification() != null) {
            user.setMaxClassification(ClassificationLevel.valueOf(req.getMaxClassification()));
        }

        AppUser saved = userRepository.save(user);
        auditService.log("USER_CREATED", "AppUser", saved.getId(), null, null);
        return mapToResponse(saved);
    }

    // ===================================================
    // MISE À JOUR
    // ===================================================
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (req.getFullName() != null)  user.setFullName(req.getFullName());
        if (req.getPosition() != null)  user.setPosition(req.getPosition());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getEnabled() != null)   user.setEnabled(req.getEnabled());

        if (req.getUnitId() != null) {
            user.setUnit(unitRepository.findById(req.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", req.getUnitId())));
        }
        if (req.getRoles() != null) {
            Set<AppRole> roles = req.getRoles().stream()
                    .map(r -> roleRepository.findByName(RoleName.valueOf(r))
                            .orElseThrow(() -> new ResourceNotFoundException("Role", r)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        if (req.getMaxClassification() != null) {
            user.setMaxClassification(ClassificationLevel.valueOf(req.getMaxClassification()));
        }

        AppUser saved = userRepository.save(user);
        auditService.log("USER_UPDATED", "AppUser", saved.getId(), null, null);
        return mapToResponse(saved);
    }

    // ===================================================
    // LECTURE
    // ===================================================
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return mapToResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)));
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return mapToResponse(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username)));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listByTenant(Long tenantId, String q, Pageable pageable) {
        Page<AppUser> page = (q != null && !q.isBlank())
                ? userRepository.searchByTenant(tenantId, q, pageable)
                : userRepository.findByTenantId(tenantId, pageable);
        return page.map(this::mapToResponse);
    }

    // ===================================================
    // DÉSACTIVATION (soft delete)
    // ===================================================
    @Transactional
    public void disableUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setEnabled(false);
        userRepository.save(user);
        auditService.log("USER_DISABLED", "AppUser", id, null, null);
    }

    // ===================================================
    // DÉVERROUILLAGE
    // ===================================================
    @Transactional
    public void unlockUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);
        auditService.log("USER_UNLOCKED", "AppUser", id, null, null);
    }

    // ===================================================
    // MAPPER
    // ===================================================
    public UserResponse mapToResponse(AppUser u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .position(u.getPosition())
                .phoneNumber(u.getPhoneNumber())
                .tenantId(u.getTenant() != null ? u.getTenant().getId() : null)
                .tenantCode(u.getTenant() != null ? u.getTenant().getCode() : null)
                .unitId(u.getUnit() != null ? u.getUnit().getId() : null)
                .unitName(u.getUnit() != null ? u.getUnit().getName() : null)
                .roles(u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .maxClassification(u.getMaxClassification() != null ? u.getMaxClassification().name() : null)
                .enabled(u.isEnabled())
                .mfaEnabled(u.isMfaEnabled())
                .mustChangePassword(u.isMustChangePassword())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
