package com.pnis.backend.intelligence.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.auth.repository.AppUserRepository;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.BadRequestException;
import com.pnis.backend.common.exception.ConflictException;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.intelligence.model.CaseNote;
import com.pnis.backend.intelligence.model.IntelligenceCase;
import com.pnis.backend.intelligence.repository.CaseNoteRepository;
import com.pnis.backend.intelligence.repository.IntelligenceCaseRepository;
import com.pnis.backend.tenant.repository.UnitRepository;
import com.pnis.backend.workflow.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service des dossiers.
 * Connexions obligatoires §8 :
 *   Dossiers → Workflow  : tâches, validations, échéances
 *   Dossiers → Reporting : génération de notes et synthèses
 */
@Slf4j
@Service
public class IntelligenceCaseService {

    private final IntelligenceCaseRepository caseRepo;
    private final CaseNoteRepository         noteRepo;
    private final AppUserRepository          userRepo;
    private final UnitRepository             unitRepo;
    private final AuditService               auditService;
    private final WorkflowService            workflowService;

    public IntelligenceCaseService(
            IntelligenceCaseRepository caseRepo,
            CaseNoteRepository noteRepo,
            AppUserRepository userRepo,
            UnitRepository unitRepo,
            AuditService auditService,
            WorkflowService workflowService) {
        this.caseRepo = caseRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.unitRepo = unitRepo;
        this.auditService = auditService;
        this.workflowService = workflowService;
    }


    // =========================================================
    // CRÉATION
    // =========================================================
    @Transactional
    public IntelligenceCase create(IntelligenceCase c) {
        Long tenantId = TenantContext.getTenantId();
        c.setTenantId(tenantId);
        c.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        c.setCaseStatus("OPEN");

        if (c.getReferenceCode() == null || c.getReferenceCode().isBlank()) {
            c.setReferenceCode(generateRef(c.getCaseType()));
        } else if (caseRepo.existsByReferenceCode(c.getReferenceCode())) {
            throw new ConflictException("Référence déjà utilisée : " + c.getReferenceCode());
        }

        // Résolution des relations
        if (c.getOpenedBy() == null) {
            userRepo.findByUsername(SecurityUtils.getCurrentUsernameOrSystem())
                    .ifPresent(c::setOpenedBy);
        }

        IntelligenceCase saved = caseRepo.save(c);

        // §8 Dossiers → Workflow
        workflowService.onCaseCreated(saved);

        auditService.log("CASE_CREATED", "IntelligenceCase", saved.getId(), saved.getUuid(),
                "ref=" + saved.getReferenceCode() + ", type=" + saved.getCaseType());
        return saved;
    }

    // =========================================================
    // MISE À JOUR DU STATUT
    // =========================================================
    @Transactional
    public IntelligenceCase updateStatus(Long id, String newStatus, String reason) {
        IntelligenceCase c = getById(id);
        String oldStatus = c.getCaseStatus();
        c.setCaseStatus(newStatus);

        if ("CLOSED".equals(newStatus) || "ARCHIVED".equals(newStatus)) {
            c.setClosedAt(Instant.now());
            c.setClosureReason(reason);
        }

        IntelligenceCase saved = caseRepo.save(c);
        auditService.log("CASE_STATUS_CHANGED", "IntelligenceCase", id, c.getUuid(),
                oldStatus + "→" + newStatus + (reason != null ? ", reason=" + reason : ""));
        return saved;
    }

    // =========================================================
    // MISE À JOUR GÉNÉRALE
    // =========================================================
    @Transactional
    public IntelligenceCase update(Long id, IntelligenceCase patch) {
        IntelligenceCase c = getById(id);
        if (patch.getTitle()         != null) c.setTitle(patch.getTitle());
        if (patch.getDescription()   != null) c.setDescription(patch.getDescription());
        if (patch.getPriorityLevel() != null) c.setPriorityLevel(patch.getPriorityLevel());
        if (patch.getObjective()     != null) c.setObjective(patch.getObjective());
        if (patch.getTargetSummary() != null) c.setTargetSummary(patch.getTargetSummary());
        if (patch.getGeoArea()       != null) c.setGeoArea(patch.getGeoArea());
        if (patch.getDeadline()      != null) c.setDeadline(patch.getDeadline());
        if (patch.getClassificationLevel() != null)
            c.setClassificationLevel(patch.getClassificationLevel());
        if (patch.getLeadOfficer()   != null) c.setLeadOfficer(patch.getLeadOfficer());

        IntelligenceCase saved = caseRepo.save(c);
        auditService.log("CASE_UPDATED", "IntelligenceCase", id, c.getUuid(), null);
        return saved;
    }

    // =========================================================
    // LECTURE
    // =========================================================
    @Transactional(readOnly = true)
    public IntelligenceCase getById(Long id) {
        return caseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IntelligenceCase", id));
    }

    @Transactional(readOnly = true)
    public IntelligenceCase getByRef(String ref) {
        return caseRepo.findByReferenceCode(ref)
                .orElseThrow(() -> new ResourceNotFoundException("IntelligenceCase", ref));
    }

    @Transactional(readOnly = true)
    public Page<IntelligenceCase> search(Long tenantId, String status, String type, String q, Pageable p) {
        return caseRepo.search(tenantId, status, type, q, p);
    }

    // =========================================================
    // NOTES CHRONOLOGIQUES
    // =========================================================
    @Transactional
    public CaseNote addNote(Long caseId, CaseNote note) {
        IntelligenceCase c = getById(caseId);
        note.setIntelligenceCase(c);
        note.setTenantId(c.getTenantId());

        userRepo.findByUsername(SecurityUtils.getCurrentUsernameOrSystem())
                .ifPresent(note::setAuthor);

        CaseNote saved = noteRepo.save(note);
        auditService.log("CASE_NOTE_ADDED", "CaseNote", saved.getId(), null,
                "caseId=" + caseId + ", type=" + note.getNoteType());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<CaseNote> getNotes(Long caseId, Pageable p) {
        return noteRepo.findByIntelligenceCaseIdOrderByCreatedAtDesc(caseId, p);
    }

    // =========================================================
    // CLÔTURE
    // =========================================================
    @Transactional
    public IntelligenceCase close(Long id, String reason) {
        IntelligenceCase c = getById(id);
        if ("CLOSED".equals(c.getCaseStatus()) || "ARCHIVED".equals(c.getCaseStatus())) {
            throw new BadRequestException("Le dossier est déjà clôturé ou archivé.");
        }
        return updateStatus(id, "CLOSED", reason);
    }

    // =========================================================
    // PRIVATE
    // =========================================================
    private String generateRef(String type) {
        String prefix = type != null ? type.substring(0, Math.min(3, type.length())).toUpperCase() : "DOS";
        return prefix + "-" + System.currentTimeMillis() / 1000 + "-" +
               UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
