package com.pnis.backend.audit.controller;

import com.pnis.backend.audit.model.AuditEvent;
import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/audit")
@PreAuthorize("hasAnyRole('AUDIT_REVIEWER','SUPER_ADMIN')")
@Tag(name = "Audit & Conformité", description = "Journal d'audit WORM, preuves, traçabilité §7.13")
public class AuditController {

    private final AuditService auditService;

    public AuditController(
            AuditService auditService) {
        this.auditService = auditService;
    }


    @GetMapping
    @Operation(summary = "Recherche dans le journal d'audit")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> search(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                auditService.search(tenantId, action, actor, from, to, pageable)));
    }

    @GetMapping("/{objectType}/{objectId}/history")
    @Operation(summary = "Historique d'un objet spécifique")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> objectHistory(
            @PathVariable String objectType,
            @PathVariable Long objectId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                auditService.getObjectHistory(objectType, objectId, pageable)));
    }
}
