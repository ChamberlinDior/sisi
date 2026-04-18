package com.pnis.backend.audit.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Événement d'audit immuable (WORM).
 * Aucun UPDATE ni DELETE ne doit être fait sur cette table.
 * Conforme §7.13 : journal d'accès, journal métier, journal sécurité,
 * horodatage sécurisé, chaînage d'intégrité.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_actor",      columnList = "actor"),
        @Index(name = "idx_audit_object_type",columnList = "object_type"),
        @Index(name = "idx_audit_object_id",  columnList = "object_id"),
        @Index(name = "idx_audit_tenant",     columnList = "tenant_id"),
        @Index(name = "idx_audit_created",    columnList = "created_at"),
        @Index(name = "idx_audit_action",     columnList = "action")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant concerné */
    @Column(name = "tenant_id")
    private Long tenantId;

    /** Login de l'acteur (SYSTEM si automatique) */
    @Column(nullable = false, length = 100)
    private String actor;

    /** IP source */
    @Column(name = "actor_ip", length = 45)
    private String actorIp;

    /** Type d'événement : USER_LOGIN, CASE_CREATED, ALERT_PUBLISHED, etc. */
    @Column(nullable = false, length = 100)
    private String action;

    /** Type de l'objet cible */
    @Column(name = "object_type", length = 80)
    private String objectType;

    /** ID de l'objet cible */
    @Column(name = "object_id")
    private Long objectId;

    /** UUID de l'objet cible (pour les exports) */
    @Column(name = "object_uuid", length = 36)
    private String objectUuid;

    /** Résultat : SUCCESS, FAILURE, DENIED */
    @Column(length = 20)
    private String result = "SUCCESS";

    /** Détails libres en JSON ou texte */
    @Column(length = 4000)
    private String details;

    /** Corrélation request */
    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    /** Hash du précédent événement – chaînage d'intégrité */
    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    /** Hash de cet événement (calculé sur les champs + previous_hash) */
    @Column(name = "self_hash", length = 64)
    private String selfHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructeur utilisé par AuditService */
    public AuditEvent(Long tenantId, String actor, String actorIp, String action,
                      String objectType, Long objectId, String objectUuid,
                      String result, String details, String correlationId,
                      String previousHash) {
        this.tenantId      = tenantId;
        this.actor         = actor;
        this.actorIp       = actorIp;
        this.action        = action;
        this.objectType    = objectType;
        this.objectId      = objectId;
        this.objectUuid    = objectUuid;
        this.result        = result != null ? result : "SUCCESS";
        this.details       = details;
        this.correlationId = correlationId;
        this.previousHash  = previousHash;
        this.createdAt     = Instant.now();
    }

    public void setSelfHash(String hash) { this.selfHash = hash; }
}
