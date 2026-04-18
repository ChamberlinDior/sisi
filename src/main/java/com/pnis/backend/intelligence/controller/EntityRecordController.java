package com.pnis.backend.intelligence.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.intelligence.model.*;
import com.pnis.backend.intelligence.service.EntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entities")
@Tag(name = "Registre des Entités", description = "Personnes, organisations, véhicules, lieux, liens §7.4")
public class EntityRecordController {

    private final EntityService entityService;

    public EntityRecordController(
            EntityService entityService) {
        this.entityService = entityService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Créer une entité")
    public ResponseEntity<ApiResponse<EntityRecord>> create(@RequestBody EntityRecord entity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(entityService.create(entity)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche entité par ID")
    public ResponseEntity<ApiResponse<EntityRecord>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(entityService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Recherche multicritères dans les entités")
    public ResponseEntity<ApiResponse<List<EntityRecord>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size);
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                entityService.search(TenantContext.getTenantId(), q, type, p)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Mettre à jour une entité")
    public ResponseEntity<ApiResponse<EntityRecord>> update(
            @PathVariable Long id, @RequestBody EntityRecord patch) {
        return ResponseEntity.ok(ApiResponse.ok(entityService.update(id, patch)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Suppression logique d'une entité")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        entityService.softDelete(id, reason);
        return ResponseEntity.ok(ApiResponse.noContent("Entité supprimée logiquement."));
    }

    @PostMapping("/{id}/merge/{targetId}")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Fusionner deux entités")
    public ResponseEntity<ApiResponse<EntityRecord>> merge(
            @PathVariable Long id, @PathVariable Long targetId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Fusion manuelle";
        return ResponseEntity.ok(ApiResponse.ok(entityService.merge(id, targetId, reason)));
    }

    // ===== ALIAS =====
    @PostMapping("/{id}/aliases")
    @Operation(summary = "Ajouter un alias")
    public ResponseEntity<ApiResponse<EntityAlias>> addAlias(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(entityService.addAlias(id, body.get("value"), body.get("type"))));
    }

    @GetMapping("/{id}/aliases")
    @Operation(summary = "Lister les alias d'une entité")
    public ResponseEntity<ApiResponse<List<EntityAlias>>> getAliases(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(entityService.getAliases(id)));
    }

    // ===== LIENS =====
    @PostMapping("/{id}/links")
    @Operation(summary = "Créer un lien entre entités")
    public ResponseEntity<ApiResponse<EntityLink>> addLink(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long targetId   = Long.valueOf(body.get("targetId").toString());
        String linkType = (String) body.get("linkType");
        String desc     = (String) body.get("description");
        String certainty = (String) body.get("certainty");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(entityService.addLink(id, targetId, linkType, desc, certainty)));
    }

    @GetMapping("/{id}/links")
    @Operation(summary = "Graphe de liens d'une entité")
    public ResponseEntity<ApiResponse<List<EntityLink>>> getLinks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(entityService.getLinks(id)));
    }
}
