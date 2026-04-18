package com.pnis.backend.document.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.common.config.AppProperties;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.BadRequestException;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.HashUtils;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.document.model.DocumentResource;
import com.pnis.backend.document.repository.DocumentResourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DocumentService {

    private final DocumentResourceRepository docRepo;
    private final AuditService               auditService;
    private final AppProperties              appProperties;

    public DocumentService(
            DocumentResourceRepository docRepo,
            AuditService auditService,
            AppProperties appProperties) {
        this.docRepo = docRepo;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }


    @Transactional
    public DocumentResource upload(MultipartFile file, String ownerType, Long ownerId,
                                    String title, String description) throws IOException {
        // Validation MIME
        String mime = file.getContentType();
        if (mime == null || !appProperties.getStorage().getAllowedMimeTypes().contains(mime)) {
            throw new BadRequestException("Type de fichier non autorisé : " + mime);
        }
        // Validation taille
        if (file.getSize() > appProperties.getStorage().getMaxFileSizeBytes()) {
            throw new BadRequestException("Fichier trop volumineux.");
        }

        // Calcul SHA-256
        String fileHash = HashUtils.sha256Hex(file.getInputStream());

        // Détection doublon
        docRepo.findByFileHash(fileHash).ifPresent(existing -> {
            log.warn("[DOC] Doublon détecté – hash={} existant #{}", fileHash, existing.getId());
        });

        // Stockage
        String ext        = FilenameUtils.getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);
        Path uploadDir    = Paths.get(appProperties.getStorage().getUploadDir(),
                                      String.valueOf(TenantContext.getTenantId()));
        Files.createDirectories(uploadDir);
        Path dest = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        DocumentResource doc = new DocumentResource();
        doc.setTenantId(TenantContext.getTenantId());
        doc.setOriginalName(file.getOriginalFilename());
        doc.setStoredName(storedName);
        doc.setStoragePath(dest.toString());
        doc.setMimeType(mime);
        doc.setFileSize(file.getSize());
        doc.setFileHash(fileHash);
        doc.setDocStatus("HASHED");
        doc.setOwnerType(ownerType);
        doc.setOwnerId(ownerId);
        doc.setTitle(title != null ? title : file.getOriginalFilename());
        doc.setDescription(description);
        doc.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        doc.setWatermarkText(appProperties.getReporting().getWatermarkText());
        doc.setIntegrityHash(fileHash);
        doc.setAvStatus("PENDING");

        DocumentResource saved = docRepo.save(doc);

        // Simulation scan AV (à remplacer par ClamAV ou équivalent)
        scheduleAvScan(saved);

        auditService.log("DOCUMENT_UPLOADED", "DocumentResource", saved.getId(), saved.getUuid(),
                "name=" + file.getOriginalFilename() + ", size=" + file.getSize() +
                ", hash=" + fileHash + ", owner=" + ownerType + "#" + ownerId);

        return saved;
    }

    @Transactional(readOnly = true)
    public Resource download(Long id) throws IOException {
        DocumentResource doc = getById(id);
        auditService.log("DOCUMENT_DOWNLOADED", "DocumentResource", id, doc.getUuid(),
                "name=" + doc.getOriginalName());
        Path path = Paths.get(doc.getStoragePath());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) throw new ResourceNotFoundException("DocumentResource", id);
        return resource;
    }

    @Transactional(readOnly = true)
    public DocumentResource getById(Long id) {
        return docRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentResource", id));
    }

    @Transactional(readOnly = true)
    public List<DocumentResource> getByOwner(String ownerType, Long ownerId) {
        return docRepo.findByOwnerTypeAndOwnerIdAndDeletedFalse(ownerType, ownerId);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResource> listByTenant(Long tenantId, Pageable pageable) {
        return docRepo.findByTenantIdAndDeletedFalse(tenantId, pageable);
    }

    @Transactional
    public void softDelete(Long id, String reason) {
        DocumentResource doc = getById(id);
        if (Boolean.TRUE.equals(doc.getIsSealed())) {
            throw new BadRequestException("Ce document est scellé et ne peut pas être supprimé.");
        }
        doc.setDeleted(true);
        doc.setDocStatus("DELETED_LOGICAL");
        doc.setDeletionReason(reason);
        docRepo.save(doc);
        auditService.log("DOCUMENT_DELETED_LOGICAL", "DocumentResource", id, doc.getUuid(),
                "reason=" + reason);
    }

    @Transactional
    public DocumentResource seal(Long id) {
        DocumentResource doc = getById(id);
        doc.setIsSealed(true);
        doc.setDocStatus("SEALED");
        DocumentResource saved = docRepo.save(doc);
        auditService.log("DOCUMENT_SEALED", "DocumentResource", id, doc.getUuid(), null);
        return saved;
    }

    private void scheduleAvScan(DocumentResource doc) {
        // Stub – intégrer ClamAV via socket TCP en prod
        doc.setAvStatus("CLEAN");
        doc.setAvScannedAt(Instant.now());
        doc.setDocStatus("AVAILABLE");
        docRepo.save(doc);
    }
}
