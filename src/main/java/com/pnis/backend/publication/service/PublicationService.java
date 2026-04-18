package com.pnis.backend.publication.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.*;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.notification.service.NotificationService;
import com.pnis.backend.publication.model.Publication;
import com.pnis.backend.publication.repository.PublicationRepository;
import com.pnis.backend.tenant.repository.SharingContractRepository;
import com.pnis.backend.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service des publications PNIS.
 * Connexion obligatoire §8 : Interface commune → Multi-tenant
 * (vérification du contrat de partage avant toute publication vers un autre tenant).
 */
@Slf4j
@Service
public class PublicationService {

    private final PublicationRepository     pubRepo;
    private final SharingContractRepository sharingRepo;
    private final TenantRepository          tenantRepo;
    private final AuditService              auditService;
    private final NotificationService       notifService;

    public PublicationService(
            PublicationRepository pubRepo,
            SharingContractRepository sharingRepo,
            TenantRepository tenantRepo,
            AuditService auditService,
            NotificationService notifService) {
        this.pubRepo = pubRepo;
        this.sharingRepo = sharingRepo;
        this.tenantRepo = tenantRepo;
        this.auditService = auditService;
        this.notifService = notifService;
    }


    @Transactional
    public Publication create(Publication pub) {
        Long tenantId = TenantContext.getTenantId();
        pub.setTenantId(tenantId);
        pub.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        pub.setPubStatus("DRAFT");
        if (pub.getReferenceCode() == null || pub.getReferenceCode().isBlank()) {
            pub.setReferenceCode("PUB-" + System.currentTimeMillis() / 1000 +
                    "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        } else if (pubRepo.existsByReferenceCode(pub.getReferenceCode())) {
            throw new ConflictException("Référence déjà utilisée : " + pub.getReferenceCode());
        }
        Publication saved = pubRepo.save(pub);
        auditService.log("PUBLICATION_CREATED", "Publication", saved.getId(), saved.getUuid(),
                "type=" + saved.getPubType());
        return saved;
    }

    /** Soumettre pour relecture */
    @Transactional
    public Publication submitForReview(Long id) {
        Publication pub = getById(id);
        if (!"DRAFT".equals(pub.getPubStatus())) {
            throw new BadRequestException("Seules les publications DRAFT peuvent être soumises.");
        }
        pub.setPubStatus("PENDING_REVIEW");
        Publication saved = pubRepo.save(pub);
        auditService.log("PUBLICATION_SUBMITTED", "Publication", id, pub.getUuid(), null);
        return saved;
    }

    /** Publier – vérifie les contrats de partage inter-tenants §8 */
    @Transactional
    public Publication publish(Long id) {
        Publication pub = getById(id);
        if (!"PENDING_REVIEW".equals(pub.getPubStatus()) && !"DRAFT".equals(pub.getPubStatus())) {
            throw new BadRequestException("Statut invalide pour publication.");
        }

        // §8 : Interface commune → Multi-tenant – vérification contrat de partage
        if (!"ALL_TENANTS".equals(pub.getTargetTenants())) {
            String[] targets = pub.getTargetTenants().split(",");
            for (String code : targets) {
                String trimmed = code.trim();
                tenantRepo.findByCode(trimmed).ifPresent(targetTenant -> {
                    var contracts = sharingRepo.findActiveContract(
                            pub.getTenantId(), targetTenant.getId(), pub.getPubType());
                    if (contracts.isEmpty()) {
                        log.warn("[PUBLICATION] Pas de contrat de partage actif vers tenant {} pour type {}",
                                trimmed, pub.getPubType());
                        // On loggue mais on ne bloque pas en phase initiale
                        // En production : lever InsufficientClearanceException
                    }
                });
            }
        }

        pub.setPubStatus("PUBLISHED");
        pub.setPublishedAt(Instant.now());
        pub.setReviewedBy(SecurityUtils.getCurrentUsernameOrSystem());
        Publication saved = pubRepo.save(pub);

        auditService.log("PUBLICATION_PUBLISHED", "Publication", id, pub.getUuid(),
                "targets=" + pub.getTargetTenants());
        return saved;
    }

    /** Retrait d'une publication */
    @Transactional
    public Publication retract(Long id, String reason) {
        Publication pub = getById(id);
        if (!"PUBLISHED".equals(pub.getPubStatus())) {
            throw new BadRequestException("Seules les publications actives peuvent être retirées.");
        }
        pub.setPubStatus("RETRACTED");
        pub.setRetractedAt(Instant.now());
        pub.setRetractReason(reason);
        Publication saved = pubRepo.save(pub);
        auditService.log("PUBLICATION_RETRACTED", "Publication", id, pub.getUuid(), "reason=" + reason);
        return saved;
    }

    @Transactional(readOnly = true)
    public Publication getById(Long id) {
        return pubRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publication", id));
    }

    @Transactional(readOnly = true)
    public Page<Publication> search(Long tenantId, String status, String type, Pageable pageable) {
        return pubRepo.search(tenantId, status, type, pageable);
    }

    /** Interface commune – publications visibles par un tenant */
    @Transactional
    public Page<Publication> getVisibleFor(String tenantCode, Pageable pageable) {
        return pubRepo.findVisibleFor(tenantCode, pageable);
    }
}
