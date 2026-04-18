package com.pnis.backend.collection.controller;

import com.pnis.backend.collection.model.CollectedData;
import com.pnis.backend.collection.model.ImportJob;
import com.pnis.backend.collection.service.CollectionService;
import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collection")
@Tag(name = "Collecte & Acquisition", description = "Saisie terrain, imports, dédoublonnage §7.3")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(
            CollectionService collectionService) {
        this.collectionService = collectionService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('COLLECTOR','ANALYST','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Soumettre une donnée collectée")
    public ResponseEntity<ApiResponse<CollectedData>> submit(@Valid @RequestBody CollectedData data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(collectionService.submit(data)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une donnée par ID")
    public ResponseEntity<ApiResponse<CollectedData>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Recherche dans les données collectées")
    public ResponseEntity<ApiResponse<List<CollectedData>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort s = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                collectionService.search(TenantContext.getTenantId(), status, dataType, from, to, pageable)));
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Valider une donnée collectée")
    public ResponseEntity<ApiResponse<CollectedData>> validate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.ok(collectionService.validate(id, notes)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Rejeter une donnée collectée")
    public ResponseEntity<ApiResponse<CollectedData>> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                collectionService.reject(id, body.get("reason"))));
    }

    // ===== IMPORT JOBS =====

    @GetMapping("/jobs")
    @Operation(summary = "Liste des jobs d'import")
    public ResponseEntity<ApiResponse<List<ImportJob>>> listJobs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                collectionService.listJobs(TenantContext.getTenantId(), pageable)));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Détail d'un job d'import")
    public ResponseEntity<ApiResponse<ImportJob>> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.getJobById(id)));
    }
}
