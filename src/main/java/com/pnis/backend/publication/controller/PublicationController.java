package com.pnis.backend.publication.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.publication.model.Publication;
import com.pnis.backend.publication.service.PublicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/publications")
@Tag(name = "Publications PNIS", description = "Interface commune inter-services §7.8 avec vérification contrats de partage")
public class PublicationController {

    private final PublicationService pubService;

    public PublicationController(
            PublicationService pubService) {
        this.pubService = pubService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('PUBLICATION_EDITOR','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Créer une publication")
    public ResponseEntity<ApiResponse<Publication>> create(@RequestBody Publication pub) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(pubService.create(pub)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une publication")
    public ResponseEntity<ApiResponse<Publication>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pubService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Recherche dans les publications")
    public ResponseEntity<ApiResponse<List<Publication>>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                pubService.search(TenantContext.getTenantId(), status, type, p)));
    }

    @GetMapping("/pnis-feed")
    @Operation(summary = "Fil PNIS – publications visibles par le tenant courant")
    public ResponseEntity<ApiResponse<List<Publication>>> pnisFeed(
            @RequestParam String tenantCode,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ResponseEntity.ok(PageUtils.toPagedResponse(pubService.getVisibleFor(tenantCode, p)));
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('PUBLICATION_EDITOR','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Soumettre pour relecture")
    public ResponseEntity<ApiResponse<Publication>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pubService.submitForReview(id)));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('PUBLICATION_EDITOR','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Publier sur l'interface commune")
    public ResponseEntity<ApiResponse<Publication>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pubService.publish(id)));
    }

    @PatchMapping("/{id}/retract")
    @PreAuthorize("hasAnyRole('PUBLICATION_EDITOR','ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Retirer une publication")
    public ResponseEntity<ApiResponse<Publication>> retract(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(pubService.retract(id, body.get("reason"))));
    }
}
