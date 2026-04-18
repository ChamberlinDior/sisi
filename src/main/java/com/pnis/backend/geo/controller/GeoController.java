package com.pnis.backend.geo.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.geo.model.GeoEvent;
import com.pnis.backend.geo.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/maps")
@Tag(name = "Cartographie & SIG", description = "Événements géolocalisés, géofencing §7.7")
public class GeoController {

    private final GeoService geoService;

    public GeoController(
            GeoService geoService) {
        this.geoService = geoService;
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('GEO_OPERATOR','COLLECTOR','ANALYST','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Créer un événement géolocalisé")
    public ResponseEntity<ApiResponse<GeoEvent>> create(@RequestBody GeoEvent event) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(geoService.create(event)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un événement")
    public ResponseEntity<ApiResponse<GeoEvent>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(geoService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Recherche dans les événements géolocalisés")
    public ResponseEntity<ApiResponse<List<GeoEvent>>> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable p = PageRequest.of(page, size);
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                geoService.search(TenantContext.getTenantId(), type, from, to, p)));
    }

    @GetMapping("/radius")
    @Operation(summary = "Événements dans un rayon (géofencing)")
    public ResponseEntity<ApiResponse<List<GeoEvent>>> radius(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5000") double radiusMeters) {
        return ResponseEntity.ok(ApiResponse.ok(geoService.findWithinRadius(lat, lon, radiusMeters)));
    }

    @GetMapping("/entity/{entityId}")
    @Operation(summary = "Historique des positions d'une entité")
    public ResponseEntity<ApiResponse<List<GeoEvent>>> byEntity(
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                geoService.getByEntity(entityId, PageRequest.of(page, size))));
    }

    @GetMapping("/case/{caseId}")
    @Operation(summary = "Carte opérationnelle d'un dossier")
    public ResponseEntity<ApiResponse<List<GeoEvent>>> byCase(
            @PathVariable Long caseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                geoService.getByCase(caseId, PageRequest.of(page, size))));
    }
}
