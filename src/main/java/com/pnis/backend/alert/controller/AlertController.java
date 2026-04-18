package com.pnis.backend.alert.controller;

import com.pnis.backend.alert.model.Alert;
import com.pnis.backend.alert.service.AlertService;
import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alerts")
@Tag(name = "Alertes", description = "Gestion des alertes opérationnelles §7.8 + §9.3")
public class AlertController {

    private final AlertService alertService;

    public AlertController(
            AlertService alertService) {
        this.alertService = alertService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Créer une alerte")
    public ResponseEntity<ApiResponse<Alert>> create(@RequestBody Alert alert) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(alertService.create(alert)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une alerte")
    public ResponseEntity<ApiResponse<Alert>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Recherche dans les alertes")
    public ResponseEntity<ApiResponse<List<Alert>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                alertService.search(TenantContext.getTenantId(), status, severity, type, from, to, p)));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','COORDINATOR','PUBLICATION_EDITOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Diffuser une alerte (DRAFT → ACTIVE)")
    public ResponseEntity<ApiResponse<Alert>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.publish(id)));
    }

    @PatchMapping("/{id}/acknowledge")
    @Operation(summary = "Accuser réception d'une alerte")
    public ResponseEntity<ApiResponse<Alert>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.acknowledge(id)));
    }

    @PatchMapping("/{id}/lift")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Lever une alerte")
    public ResponseEntity<ApiResponse<Alert>> lift(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.lift(id, body.get("reason"))));
    }
}
