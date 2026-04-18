package com.pnis.backend.document.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.document.model.DocumentResource;
import com.pnis.backend.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents & Pièces jointes", description = "Upload, SHA-256, antivirus, versionning §7.10 + §9.4")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(
            DocumentService documentService) {
        this.documentService = documentService;
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('COLLECTOR','ANALYST','ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Téléverser un document")
    public ResponseEntity<ApiResponse<DocumentResource>> upload(
            @RequestParam("file")        MultipartFile file,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) Long   ownerId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        documentService.upload(file, ownerType, ownerId, title, description)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Métadonnées d'un document")
    public ResponseEntity<ApiResponse<DocumentResource>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getById(id)));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Télécharger un document")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        DocumentResource meta = documentService.getById(id);
        Resource resource = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        meta.getMimeType() != null ? meta.getMimeType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getOriginalName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Prévisualiser un document (inline)")
    public ResponseEntity<Resource> preview(@PathVariable Long id) throws Exception {
        DocumentResource meta = documentService.getById(id);
        Resource resource = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        meta.getMimeType() != null ? meta.getMimeType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + meta.getOriginalName() + "\"")
                .body(resource);
    }

    @GetMapping
    @Operation(summary = "Liste des documents du tenant")
    public ResponseEntity<ApiResponse<List<DocumentResource>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                documentService.listByTenant(TenantContext.getTenantId(), p)));
    }

    @GetMapping("/owner/{ownerType}/{ownerId}")
    @Operation(summary = "Documents liés à un objet (dossier, entité, alerte…)")
    public ResponseEntity<ApiResponse<List<DocumentResource>>> byOwner(
            @PathVariable String ownerType, @PathVariable Long ownerId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getByOwner(ownerType, ownerId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Suppression logique d'un document")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        documentService.softDelete(id, reason);
        return ResponseEntity.ok(ApiResponse.noContent("Document supprimé logiquement."));
    }

    @PatchMapping("/{id}/seal")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Sceller un document (verrouillage définitif)")
    public ResponseEntity<ApiResponse<DocumentResource>> seal(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.seal(id)));
    }
}
