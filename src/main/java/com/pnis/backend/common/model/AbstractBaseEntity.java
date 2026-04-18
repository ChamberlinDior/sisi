package com.pnis.backend.common.model;

import com.pnis.backend.common.enums.ClassificationLevel;
import com.pnis.backend.common.enums.RecordStatus;
import com.pnis.backend.common.enums.VisibilityScope;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité de base héritée par tous les objets métier importants.
 * Conforme au §13 du CDC : id, uuid, tenant_id, owner_org_id,
 * created_at, updated_at, created_by, updated_by, status,
 * classification_level, visibility_scope, source_channel,
 * deleted, version.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid;

    /** Tenant (institution) propriétaire de l'enregistrement */
    @Column(name = "tenant_id")
    private Long tenantId;

    /** Organisation / bureau propriétaire au sein du tenant */
    @Column(name = "owner_org_id")
    private Long ownerOrgId;

    /** Statut dans le cycle de vie de la collecte */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecordStatus recordStatus = RecordStatus.DRAFT;

    /** Niveau de classification de sécurité */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassificationLevel classificationLevel = ClassificationLevel.RESTRICTED;

    /** Portée de visibilité inter-tenants */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisibilityScope visibilityScope = VisibilityScope.TENANT_ONLY;

    /** Canal d'acquisition (MANUAL, API, CSV_IMPORT, OSINT, FORM, MOBILE, etc.) */
    @Column(name = "source_channel", length = 50)
    private String sourceChannel;

    /** Suppression logique (jamais physique sur données métier sensibles) */
    @Column(nullable = false)
    private Boolean deleted = false;

    /** Raison de la suppression logique */
    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    /** Optimistic locking */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Login de l'agent créateur */
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    /** Login du dernier modificateur */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /** Empreinte SHA-256 pour l'intégrité (renseignée si applicable) */
    @Column(name = "integrity_hash", length = 64)
    private String integrityHash;

    @PrePersist
    protected void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }
}