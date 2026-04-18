package com.pnis.backend.publication.model;

import com.pnis.backend.common.model.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Publication vers l'interface commune PNIS – §7.8.
 * Connexion §8 : Interface commune → Multi-tenant (vérification contrat de partage avant publication).
 * Statuts : DRAFT, PENDING_REVIEW, PUBLISHED, RETRACTED, EXPIRED.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "publications", indexes = {
        @Index(name = "idx_pub_tenant",    columnList = "tenant_id"),
        @Index(name = "idx_pub_status",    columnList = "pub_status"),
        @Index(name = "idx_pub_type",      columnList = "pub_type"),
        @Index(name = "idx_pub_published", columnList = "published_at")
})
public class Publication extends AbstractBaseEntity {

    @Column(name = "reference_code", nullable = false, unique = true, length = 60)
    private String referenceCode;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    /** ALERT, BULLETIN, INDICATOR, COORDINATION_MESSAGE, CALENDAR_EVENT, SITUATION_REPORT */
    @Column(name = "pub_type", nullable = false, length = 50)
    private String pubType;

    /** DRAFT, PENDING_REVIEW, PUBLISHED, RETRACTED, EXPIRED */
    @Column(name = "pub_status", nullable = false, length = 30)
    private String pubStatus = "DRAFT";

    /** Tenant(s) destinataires ciblés (CSV de codes, ou ALL_TENANTS) */
    @Column(name = "target_tenants", length = 500)
    private String targetTenants = "ALL_TENANTS";

    /** Nominatif ou anonymisé */
    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    /** Lien vers l'alerte source */
    @Column(name = "alert_id")
    private Long alertId;

    /** Lien vers le dossier source */
    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "retracted_at")
    private Instant retractedAt;

    @Column(name = "retract_reason", length = 1000)
    private String retractReason;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "view_count")
    private Long viewCount = 0L;
}
