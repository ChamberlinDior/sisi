package com.pnis.backend.tenant.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Contrat de partage inter-tenants.
 * Un tenant propriétaire autorise explicitement un tenant cible à accéder
 * à certains types d'objets avec un niveau de classification maximum.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "sharing_contracts")
public class SharingContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_tenant_id", nullable = false)
    private Tenant sourceTenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_tenant_id", nullable = false)
    private Tenant targetTenant;

    /** Type d'objet concerné: ALERT, ENTITY, CASE, PUBLICATION, ALL */
    @Column(name = "object_type", nullable = false, length = 50)
    private String objectType = "ALL";

    /** Niveau de classification maximum autorisé à partager */
    @Column(name = "max_classification", length = 20)
    private String maxClassification = "RESTRICTED";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "granted_by", length = 100)
    private String grantedBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }

    public enum ContractStatus { PENDING, ACTIVE, SUSPENDED, REVOKED, EXPIRED }
}
