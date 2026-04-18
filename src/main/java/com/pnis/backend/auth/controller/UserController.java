package com.pnis.backend.auth.controller;

import com.pnis.backend.auth.dto.AuthDtos.*;
import com.pnis.backend.auth.service.UserService;
import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agents")
@Tag(name = "Agents / Utilisateurs", description = "Gestion des agents et utilisateurs de la plateforme")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService) {
        this.userService = userService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Créer un agent")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(userService.createUser(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Mettre à jour un agent")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateUser(id, req)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un agent par ID")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'agent connecté")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getByUsername(user.getUsername())));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Lister les agents d'un tenant")
    public ResponseEntity<ApiResponse<java.util.List<UserResponse>>> listByTenant(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(PageUtils.toPagedResponse(userService.listByTenant(tenantId, q, pageable)));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Désactiver un agent")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.noContent("Agent désactivé."));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Déverrouiller un compte")
    public ResponseEntity<ApiResponse<Void>> unlock(@PathVariable Long id) {
        userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.noContent("Compte déverrouillé."));
    }
}
