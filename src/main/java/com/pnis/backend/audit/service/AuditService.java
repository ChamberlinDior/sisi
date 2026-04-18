package com.pnis.backend.audit.service;

import com.pnis.backend.audit.model.AuditEvent;
import com.pnis.backend.audit.repository.AuditEventRepository;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.HashUtils;
import com.pnis.backend.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service d'audit WORM : chaque événement est signé avec le hash du précédent
 * afin de détecter toute altération de la chaîne.
 * Les écritures se font dans une transaction indépendante pour ne jamais
 * être annulées si la transaction métier échoue.
 */
@Slf4j
@Service
public class AuditService {

    private final AuditEventRepository auditRepo;

    public AuditService(
            AuditEventRepository auditRepo) {
        this.auditRepo = auditRepo;
    }


    /**
     * Journalisation asynchrone non bloquante pour les flux critiques.
     * Utiliser log() pour les cas où la synchronicité n'est pas requise.
     */
    @Async("taskExecutor")
    public void logAsync(String action, String objectType, Long objectId,
                          String objectUuid, String details) {
        log(action, objectType, objectId, objectUuid, details);
    }

    /**
     * Journalisation synchrone dans une transaction NEW pour garantir
     * l'écriture même si la transaction appelante est annulée.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String objectType, Long objectId,
                    String objectUuid, String details) {
        try {
            Long   tenantId      = TenantContext.getTenantId();
            String actor         = SecurityUtils.getCurrentUsernameOrSystem();
            String correlationId = MDC.get("correlationId");

            String previousHash = auditRepo.findLastByTenant(tenantId != null ? tenantId : 0L)
                    .map(AuditEvent::getSelfHash)
                    .orElse("GENESIS");

            AuditEvent event = new AuditEvent(
                    tenantId, actor, null, action,
                    objectType, objectId, objectUuid,
                    "SUCCESS", details, correlationId, previousHash);

            // Calcul de l'empreinte de cet événement
            String payload = tenantId + "|" + actor + "|" + action + "|" +
                    objectType + "|" + objectId + "|" + event.getCreatedAt() + "|" + previousHash;
            event.setSelfHash(HashUtils.sha256Hex(payload.getBytes()));

            auditRepo.save(event);
        } catch (Exception e) {
            log.error("[AUDIT] Échec de journalisation – action: {}, objet: {} #{}: {}",
                    action, objectType, objectId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(String action, String objectType, Long objectId, String details) {
        try {
            Long   tenantId      = TenantContext.getTenantId();
            String actor         = SecurityUtils.getCurrentUsernameOrSystem();
            String correlationId = MDC.get("correlationId");

            String previousHash = auditRepo.findLastByTenant(tenantId != null ? tenantId : 0L)
                    .map(AuditEvent::getSelfHash).orElse("GENESIS");

            AuditEvent event = new AuditEvent(
                    tenantId, actor, null, action,
                    objectType, objectId, null,
                    "FAILURE", details, correlationId, previousHash);

            String payload = tenantId + "|" + actor + "|" + action + "|" + "FAILURE" +
                    "|" + event.getCreatedAt() + "|" + previousHash;
            event.setSelfHash(HashUtils.sha256Hex(payload.getBytes()));

            auditRepo.save(event);
        } catch (Exception e) {
            log.error("[AUDIT] Échec journalisation FAILURE : {}", e.getMessage());
        }
    }

    // ===== Lecture =====

    @Transactional(readOnly = true)
    public Page<AuditEvent> search(Long tenantId, String action, String actor,
                                   Instant from, Instant to, Pageable pageable) {
        return auditRepo.search(tenantId, action, actor, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getObjectHistory(String objectType, Long objectId, Pageable pageable) {
        return auditRepo.findByObjectTypeAndObjectId(objectType, objectId, pageable);
    }
}
