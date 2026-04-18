package com.pnis.backend.admin.controller;

import com.pnis.backend.admin.model.ReferenceData;
import com.pnis.backend.admin.service.AdminService;
import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.tenant.model.Tenant;
import com.pnis.backend.tenant.model.Unit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Administration", description = "Référentiels, tenants, unités, paramétrage §7.14")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ===== RÉFÉRENTIELS =====
    @GetMapping("/references/{category}")
    @Operation(summary = "Liste des valeurs d'une catégorie de référentiel")
    public ResponseEntity<ApiResponse<List<ReferenceData>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getByCategory(category)));
    }

    @PostMapping("/references")
    @Operation(summary = "Créer une valeur de référentiel")
    public ResponseEntity<ApiResponse<ReferenceData>> createRef(@RequestBody ReferenceData ref) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminService.createRef(ref)));
    }

    @PutMapping("/references/{id}")
    @Operation(summary = "Mettre à jour une valeur de référentiel")
    public ResponseEntity<ApiResponse<ReferenceData>> updateRef(
            @PathVariable Long id, @RequestBody ReferenceData patch) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateRef(id, patch)));
    }

    @DeleteMapping("/references/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Supprimer une valeur de référentiel (non-système)")
    public ResponseEntity<ApiResponse<Void>> deleteRef(@PathVariable Long id) {
        adminService.deleteRef(id);
        return ResponseEntity.ok(ApiResponse.noContent("Référentiel supprimé."));
    }

    // ===== TENANTS =====
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Liste des institutions / tenants")
    public ResponseEntity<ApiResponse<List<Tenant>>> listTenants(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("code"));
        return ResponseEntity.ok(PageUtils.toPagedResponse(adminService.listTenants(p)));
    }

    @PostMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Créer un tenant / institution")
    public ResponseEntity<ApiResponse<Tenant>> createTenant(@RequestBody Tenant tenant) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminService.createTenant(tenant)));
    }

    @PutMapping("/tenants/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Mettre à jour un tenant")
    public ResponseEntity<ApiResponse<Tenant>> updateTenant(
            @PathVariable Long id, @RequestBody Tenant patch) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateTenant(id, patch)));
    }

    // ===== UNITÉS =====
    @GetMapping("/tenants/{tenantId}/units")
    @Operation(summary = "Unités d'un tenant")
    public ResponseEntity<ApiResponse<List<Unit>>> listUnits(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUnits(tenantId)));
    }

    @PostMapping("/units")
    @Operation(summary = "Créer une unité")
    public ResponseEntity<ApiResponse<Unit>> createUnit(@RequestBody Unit unit) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminService.createUnit(unit)));
    }
}
