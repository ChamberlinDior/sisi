package com.pnis.backend.intelligence.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.intelligence.model.CaseNote;
import com.pnis.backend.intelligence.model.IntelligenceCase;
import com.pnis.backend.intelligence.service.IntelligenceCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cases")
@Tag(name = "Dossiers & Affaires", description = "Gestion des dossiers, affaires, missions §7.5")
public class IntelligenceCaseController {

    private final IntelligenceCaseService caseService;

    public IntelligenceCaseController(
            IntelligenceCaseService caseService) {
        this.caseService = caseService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Créer un dossier")
    public ResponseEntity<ApiResponse<IntelligenceCase>> create(@RequestBody IntelligenceCase c) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(caseService.create(c)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un dossier par ID")
    public ResponseEntity<ApiResponse<IntelligenceCase>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(caseService.getById(id)));
    }

    @GetMapping("/ref/{ref}")
    @Operation(summary = "Obtenir un dossier par référence")
    public ResponseEntity<ApiResponse<IntelligenceCase>> getByRef(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.ok(caseService.getByRef(ref)));
    }

    @GetMapping
    @Operation(summary = "Recherche dans les dossiers")
    public ResponseEntity<ApiResponse<List<IntelligenceCase>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "openedAt") String sort,
            @RequestParam(defaultValue = "DESC") String dir) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(dir), sort));
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                caseService.search(TenantContext.getTenantId(), status, type, q, p)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Mettre à jour un dossier")
    public ResponseEntity<ApiResponse<IntelligenceCase>> update(
            @PathVariable Long id, @RequestBody IntelligenceCase patch) {
        return ResponseEntity.ok(ApiResponse.ok(caseService.update(id, patch)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','COORDINATOR','WORKFLOW_VALIDATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Changer le statut d'un dossier")
    public ResponseEntity<ApiResponse<IntelligenceCase>> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                caseService.updateStatus(id, body.get("status"), body.get("reason"))));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Clôturer un dossier")
    public ResponseEntity<ApiResponse<IntelligenceCase>> close(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(caseService.close(id, body.get("reason"))));
    }

    // ===== NOTES =====
    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Ajouter une note chronologique")
    public ResponseEntity<ApiResponse<CaseNote>> addNote(
            @PathVariable Long id, @RequestBody CaseNote note) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(caseService.addNote(id, note)));
    }

    @GetMapping("/{id}/notes")
    @Operation(summary = "Journal chronologique du dossier")
    public ResponseEntity<ApiResponse<List<CaseNote>>> getNotes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                caseService.getNotes(id, PageRequest.of(page, size))));
    }
}
