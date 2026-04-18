package com.pnis.backend.intelligence.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Lien entre deux entités – modèle entité–lien–propriété §7.4.
 * Avec dates, source et niveau de certitude.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "entity_links", indexes = {
        @Index(name = "idx_link_source", columnList = "source_entity_id"),
        @Index(name = "idx_link_target", columnList = "target_entity_id"),
        @Index(name = "idx_link_type",   columnList = "link_type")
})
public class EntityLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_entity_id", nullable = false)
    private EntityRecord sourceEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_entity_id", nullable = false)
    private EntityRecord targetEntity;

    /** ASSOCIATED_WITH, MEMBER_OF, OWNS, COMMUNICATES_WITH, LOCATED_AT, etc. */
    @Column(name = "link_type", nullable = false, length = 80)
    private String linkType;

    @Column(length = 500)
    private String description;

    /** Niveau de certitude : CONFIRMED, PROBABLE, POSSIBLE, UNCONFIRMED */
    @Column(name = "certainty_level", length = 20)
    private String certaintyLevel = "POSSIBLE";

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "source_ref", length = 200)
    private String sourceRef;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
