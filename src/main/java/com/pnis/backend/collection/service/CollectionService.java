package com.pnis.backend.collection.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.collection.model.CollectedData;
import com.pnis.backend.collection.model.ImportJob;
import com.pnis.backend.collection.repository.CollectedDataRepository;
import com.pnis.backend.collection.repository.ImportJobRepository;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.enums.RecordStatus;
import com.pnis.backend.common.exception.BadRequestException;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.HashUtils;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.intelligence.service.EntityService;
import com.pnis.backend.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service de collecte.
 * Connexions obligatoires §8 :
 *   1. Collecte → Workflow  : statut RECEIVED / PENDING_VERIFICATION
 *   2. Collecte → Audit     : journal de création + empreinte + auteur + canal
 *   3. Collecte → Entités   : tentative de rattachement ou création d'entités candidates
 */
@Slf4j
@Service
public class CollectionService {

    private final CollectedDataRepository dataRepo;
    private final ImportJobRepository     jobRepo;
    private final AuditService            auditService;
    private final WorkflowService         workflowService;
    private final EntityService           entityService;

    public CollectionService(
            CollectedDataRepository dataRepo,
            ImportJobRepository jobRepo,
            AuditService auditService,
            WorkflowService workflowService,
            EntityService entityService) {
        this.dataRepo = dataRepo;
        this.jobRepo = jobRepo;
        this.auditService = auditService;
        this.workflowService = workflowService;
        this.entityService = entityService;
    }


    // =========================================================
    // CRÉATION / SAISIE
    // =========================================================
    @Transactional
    public CollectedData submit(CollectedData data) {
        Long tenantId = TenantContext.getTenantId();
        String actor  = SecurityUtils.getCurrentUsernameOrSystem();

        data.setTenantId(tenantId);
        data.setCreatedBy(actor);
        data.setSourceChannel(data.getSourceChannel() != null ? data.getSourceChannel() : "MANUAL");

        // Calcul hash de déduplication
        String dedupPayload = StringUtils.defaultString(data.getRawContent(), "")
                + StringUtils.defaultString(data.getTitle(), "")
                + StringUtils.defaultString(data.getExternalRef(), "");
        String dedupHash = HashUtils.sha256Hex(dedupPayload.getBytes());
        data.setDedupHash(dedupHash);

        // Détection de doublon
        dataRepo.findByDedupHash(dedupHash).ifPresent(existing -> {
            data.setIsDuplicate(true);
            data.setDuplicateOfId(existing.getId());
        });

        // §8 : Collecte → Workflow : statut RECEIVED puis A_VERIFIER
        data.setRecordStatus(RecordStatus.RECEIVED);
        CollectedData saved = dataRepo.save(data);

        // §8 : Collecte → Audit
        auditService.log("COLLECTED_DATA_SUBMITTED", "CollectedData", saved.getId(),
                saved.getUuid(),
                "channel=" + saved.getSourceChannel() + ", hash=" + dedupHash +
                ", duplicate=" + saved.getIsDuplicate());

        // Transition automatique vers A_VERIFIER via le moteur workflow
        workflowService.onCollectionReceived(saved);

        // §8 : Collecte → Entités (asynchrone)
        entityService.tryLinkCollectedDataAsync(saved);

        return saved;
    }

    // =========================================================
    // VALIDATION
    // =========================================================
    @Transactional
    public CollectedData validate(Long id, String notes) {
        CollectedData data = getById(id);
        if (data.getRecordStatus() != RecordStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Seules les données en A_VERIFIER peuvent être validées.");
        }
        data.setRecordStatus(RecordStatus.VALIDATED);
        data.setVerifiedBy(SecurityUtils.getCurrentUsernameOrSystem());
        data.setVerifiedAt(Instant.now());
        data.setVerificationNotes(notes);
        CollectedData saved = dataRepo.save(data);

        auditService.log("COLLECTED_DATA_VALIDATED", "CollectedData", id, data.getUuid(),
                "validatedBy=" + data.getVerifiedBy());
        workflowService.onCollectionValidated(saved);
        return saved;
    }

    // =========================================================
    // REJET
    // =========================================================
    @Transactional
    public CollectedData reject(Long id, String reason) {
        CollectedData data = getById(id);
        data.setRecordStatus(RecordStatus.REJECTED);
        data.setVerifiedBy(SecurityUtils.getCurrentUsernameOrSystem());
        data.setVerifiedAt(Instant.now());
        data.setVerificationNotes(reason);
        CollectedData saved = dataRepo.save(data);

        auditService.log("COLLECTED_DATA_REJECTED", "CollectedData", id, data.getUuid(),
                "reason=" + reason);
        return saved;
    }

    // =========================================================
    // LECTURE
    // =========================================================
    @Transactional(readOnly = true)
    public CollectedData getById(Long id) {
        return dataRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CollectedData", id));
    }

    @Transactional(readOnly = true)
    public Page<CollectedData> search(Long tenantId, String status, String dataType,
                                       Instant from, Instant to, Pageable pageable) {
        return dataRepo.search(tenantId, status, dataType, from, to, pageable);
    }

    // =========================================================
    // IMPORT JOB
    // =========================================================
    @Transactional
    public ImportJob createImportJob(String fileName, String fileType, String targetType, String filePath, String fileHash) {
        ImportJob job = new ImportJob();
        job.setTenantId(TenantContext.getTenantId());
        job.setFileName(fileName);
        job.setFilePath(filePath);
        job.setFileHash(fileHash);
        job.setFileType(fileType);
        job.setTargetType(targetType);
        job.setSubmittedBy(SecurityUtils.getCurrentUsernameOrSystem());
        job.setStatus(ImportJob.JobStatus.QUEUED);
        ImportJob saved = jobRepo.save(job);
        auditService.log("IMPORT_JOB_CREATED", "ImportJob", saved.getId(), null,
                "file=" + fileName + ", type=" + fileType);
        return saved;
    }

    @Transactional(readOnly = true)
    public ImportJob getJobById(Long id) {
        return jobRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ImportJob", id));
    }

    @Transactional(readOnly = true)
    public Page<ImportJob> listJobs(Long tenantId, Pageable pageable) {
        return jobRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional
    public void updateJobStatus(Long id, ImportJob.JobStatus status,
                                 Integer processed, Integer success, Integer errors, String errorDetails) {
        ImportJob job = getJobById(id);
        job.setStatus(status);
        if (processed != null)    job.setProcessedRows(processed);
        if (success   != null)    job.setSuccessRows(success);
        if (errors    != null)    job.setErrorRows(errors);
        if (errorDetails != null) job.setErrorDetails(errorDetails);
        if (status == ImportJob.JobStatus.PROCESSING && job.getStartedAt() == null) {
            job.setStartedAt(Instant.now());
        }
        if (status == ImportJob.JobStatus.COMPLETED ||
            status == ImportJob.JobStatus.COMPLETED_WITH_ERRORS ||
            status == ImportJob.JobStatus.FAILED) {
            job.setCompletedAt(Instant.now());
        }
        jobRepo.save(job);
        auditService.log("IMPORT_JOB_STATUS_CHANGED", "ImportJob", id, null,
                "status=" + status + ", success=" + success + ", errors=" + errors);
    }
}
