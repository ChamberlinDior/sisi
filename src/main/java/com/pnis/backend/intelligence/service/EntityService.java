package com.pnis.backend.intelligence.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.collection.model.CollectedData;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.ConflictException;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.HashUtils;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.intelligence.model.*;
import com.pnis.backend.intelligence.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service des entités métier.
 * Connexion obligatoire §8 : Entités → Analyse (indexation et corrélation).
 */
@Slf4j
@Service
public class EntityService {

    private final EntityRecordRepository  entityRepo;
    private final EntityAliasRepository   aliasRepo;
    private final EntityLinkRepository    linkRepo;
    private final AuditService            auditService;

    public EntityService(
            EntityRecordRepository entityRepo,
            EntityAliasRepository aliasRepo,
            EntityLinkRepository linkRepo,
            AuditService auditService) {
        this.entityRepo = entityRepo;
        this.aliasRepo = aliasRepo;
        this.linkRepo = linkRepo;
        this.auditService = auditService;
    }


    private static final String REF_PREFIX = "ENT-";

    // =========================================================
    // CRÉATION
    // =========================================================
    @Transactional
    public EntityRecord create(EntityRecord entity) {
        Long tenantId = TenantContext.getTenantId();
        entity.setTenantId(tenantId);
        entity.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());

        if (entity.getReferenceCode() == null || entity.getReferenceCode().isBlank()) {
            entity.setReferenceCode(generateRef());
        } else if (entityRepo.existsByReferenceCode(entity.getReferenceCode())) {
            throw new ConflictException("Référence déjà utilisée : " + entity.getReferenceCode());
        }

        EntityRecord saved = entityRepo.save(entity);
        auditService.log("ENTITY_CREATED", "EntityRecord", saved.getId(), saved.getUuid(),
                "type=" + saved.getEntityType() + ", label=" + saved.getLabel());
        return saved;
    }

    // =========================================================
    // MISE À JOUR
    // =========================================================
    @Transactional
    public EntityRecord update(Long id, EntityRecord patch) {
        EntityRecord existing = getById(id);
        if (patch.getLabel()           != null) existing.setLabel(patch.getLabel());
        if (patch.getDescription()     != null) existing.setDescription(patch.getDescription());
        if (patch.getFirstName()       != null) existing.setFirstName(patch.getFirstName());
        if (patch.getLastName()        != null) existing.setLastName(patch.getLastName());
        if (patch.getBirthDate()       != null) existing.setBirthDate(patch.getBirthDate());
        if (patch.getNationality()     != null) existing.setNationality(patch.getNationality());
        if (patch.getIdNumber()        != null) existing.setIdNumber(patch.getIdNumber());
        if (patch.getContactValue()    != null) existing.setContactValue(patch.getContactValue());
        if (patch.getConfidenceScore() != null) existing.setConfidenceScore(patch.getConfidenceScore());
        if (patch.getThreatLevel()     != null) existing.setThreatLevel(patch.getThreatLevel());
        if (patch.getLatitude()        != null) existing.setLatitude(patch.getLatitude());
        if (patch.getLongitude()       != null) existing.setLongitude(patch.getLongitude());

        EntityRecord saved = entityRepo.save(existing);
        auditService.log("ENTITY_UPDATED", "EntityRecord", id, existing.getUuid(), null);
        return saved;
    }

    // =========================================================
    // LECTURE
    // =========================================================
    @Transactional(readOnly = true)
    public EntityRecord getById(Long id) {
        return entityRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EntityRecord", id));
    }

    @Transactional(readOnly = true)
    public Page<EntityRecord> search(Long tenantId, String q, String type, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return entityRepo.search(tenantId, q, type, pageable);
        }
        if (type != null) {
            return entityRepo.findByTenantIdAndEntityTypeAndDeletedFalse(tenantId, type, pageable);
        }
        return entityRepo.findByTenantIdAndDeletedFalse(tenantId, pageable);
    }

    // =========================================================
    // SUPPRESSION LOGIQUE
    // =========================================================
    @Transactional
    public void softDelete(Long id, String reason) {
        EntityRecord e = getById(id);
        e.setDeleted(true);
        e.setDeletionReason(reason);
        entityRepo.save(e);
        auditService.log("ENTITY_DELETED_LOGICAL", "EntityRecord", id, e.getUuid(), "reason=" + reason);
    }

    // =========================================================
    // FUSION
    // =========================================================
    @Transactional
    public EntityRecord merge(Long sourceId, Long targetId, String reason) {
        EntityRecord source = getById(sourceId);
        EntityRecord target = getById(targetId);

        // Rediriger les liens
        linkRepo.findBySourceEntityId(sourceId).forEach(l -> {
            if (!l.getTargetEntity().getId().equals(targetId)) {
                l.setSourceEntity(target);
                linkRepo.save(l);
            }
        });
        linkRepo.findByTargetEntityId(sourceId).forEach(l -> {
            if (!l.getSourceEntity().getId().equals(targetId)) {
                l.setTargetEntity(target);
                linkRepo.save(l);
            }
        });

        // Transférer les alias
        aliasRepo.findByEntityId(sourceId).forEach(a -> {
            a.setEntity(target);
            aliasRepo.save(a);
        });
        // Ajouter un alias avec l'ancien label
        EntityAlias alias = new EntityAlias();
        alias.setEntity(target);
        alias.setAliasValue(source.getLabel());
        alias.setAliasType("MERGED_FROM");
        aliasRepo.save(alias);

        source.setIsMerged(true);
        source.setMergedIntoId(targetId);
        source.setDeleted(true);
        source.setDeletionReason("Fusionné avec " + target.getReferenceCode() + " – " + reason);
        entityRepo.save(source);

        auditService.log("ENTITY_MERGED", "EntityRecord", sourceId, source.getUuid(),
                "mergedInto=" + targetId + ", reason=" + reason);
        return target;
    }

    // =========================================================
    // ALIAS
    // =========================================================
    @Transactional
    public EntityAlias addAlias(Long entityId, String value, String type) {
        EntityRecord entity = getById(entityId);
        EntityAlias alias = new EntityAlias();
        alias.setEntity(entity);
        alias.setAliasValue(value);
        alias.setAliasType(type);
        return aliasRepo.save(alias);
    }

    @Transactional(readOnly = true)
    public List<EntityAlias> getAliases(Long entityId) {
        return aliasRepo.findByEntityId(entityId);
    }

    // =========================================================
    // LIENS
    // =========================================================
    @Transactional
    public EntityLink addLink(Long sourceId, Long targetId, String linkType,
                               String description, String certainty) {
        EntityRecord source = getById(sourceId);
        EntityRecord target = getById(targetId);

        EntityLink link = new EntityLink();
        link.setSourceEntity(source);
        link.setTargetEntity(target);
        link.setLinkType(linkType);
        link.setDescription(description);
        link.setCertaintyLevel(certainty != null ? certainty : "POSSIBLE");
        link.setTenantId(TenantContext.getTenantId());
        link.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        EntityLink saved = linkRepo.save(link);

        auditService.log("ENTITY_LINK_CREATED", "EntityLink", saved.getId(), null,
                sourceId + " -[" + linkType + "]-> " + targetId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<EntityLink> getLinks(Long entityId) {
        return linkRepo.findAllRelatedLinks(entityId);
    }

    // =========================================================
    // CONNEXION §8 : Collecte → Entités (async)
    // =========================================================
    @Async("taskExecutor")
    @Transactional
    public void tryLinkCollectedDataAsync(CollectedData data) {
        try {
            // Recherche phonétique simple sur le titre de la collecte
            Page<EntityRecord> candidates = entityRepo.search(
                    data.getTenantId(),
                    data.getTitle(),
                    null,
                    PageRequest.of(0, 5));

            if (!candidates.isEmpty()) {
                log.info("[ENTITY-LINK] {} candidat(s) trouvé(s) pour la collecte #{}",
                        candidates.getTotalElements(), data.getId());
                // Ici on pourrait créer des CaseEntity liens ou des suggestions
                // Pour l'instant on loggue l'audit
                auditService.log("ENTITY_AUTO_MATCH_CANDIDATE", "CollectedData", data.getId(),
                        data.getUuid(), "candidates=" + candidates.getTotalElements());
            }
        } catch (Exception e) {
            log.warn("[ENTITY-LINK] Erreur lors du rattachement automatique : {}", e.getMessage());
        }
    }

    // =========================================================
    // PRIVATE
    // =========================================================
    private String generateRef() {
        return REF_PREFIX + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
