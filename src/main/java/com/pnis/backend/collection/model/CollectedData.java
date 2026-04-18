package com.pnis.backend.collection.model;

import com.pnis.backend.common.model.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Donnée brute entrante – conforme §7.3 + cycle de vie §9.1.
 * Aucune donnée brute n'entre en exploitation sans passer par le workflow.
 * Connexion obligatoire §8 : Collecte → Workflow, Collecte → Audit, Collecte → Entités.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "collected_data", indexes = {
        @Index(name = "idx_collect_tenant",  columnList = "tenant_id"),
        @Index(name = "idx_collect_status",  columnList = "record_status"),
        @Index(name = "idx_collect_channel", columnList = "source_channel"),
        @Index(name = "idx_collect_created", columnList = "created_at")
})
public class CollectedData extends AbstractBaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 8000)
    private String rawContent;

    /** Type : HUMAN_REPORT, OSINT, IMPORT_CSV, FORM, MOBILE, INTERCEPT, etc. */
    @Column(name = "data_type", length = 50)
    private String dataType;

    /** Sujet / thématique de la collecte */
    @Column(length = 200)
    private String subject;

    /** Référence de l'opération ou mission liée */
    @Column(name = "operation_ref", length = 100)
    private String operationRef;

    /** Localisation libre texte si connue */
    @Column(length = 300)
    private String locationLabel;

    /** Coordonnées GPS si disponibles */
    private Double latitude;
    private Double longitude;

    /** Horodatage de l'événement réel (peut différer de createdAt) */
    @Column(name = "event_timestamp")
    private Instant eventTimestamp;

    /** Fiabilité de la source (A-F) */
    @Column(name = "source_reliability", length = 5)
    private String sourceReliability;

    /** Fiabilité de l'information (1-6) */
    @Column(name = "info_credibility", length = 5)
    private String infoCredibility;

    /** Score de confiance composite 0-100 */
    @Column(name = "confidence_score")
    private Integer confidenceScore;

    /** Préclassification automatique de sensibilité */
    @Column(name = "auto_classification", length = 20)
    private String autoClassification;

    /** ID du job d'import batch si applicable */
    @Column(name = "import_job_id")
    private Long importJobId;

    /** Identifiant externe (référence origine) */
    @Column(name = "external_ref", length = 200)
    private String externalRef;

    /** Hash de déduplication (sha256 du contenu normalisé) */
    @Column(name = "dedup_hash", length = 64)
    private String dedupHash;

    /** Indicateur de doublon détecté */
    @Column(name = "is_duplicate")
    private Boolean isDuplicate = false;

    @Column(name = "duplicate_of_id")
    private Long duplicateOfId;

    /** Remarques de vérification */
    @Column(name = "verification_notes", length = 2000)
    private String verificationNotes;

    /** Login de l'agent vérificateur */
    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    /** En attente de synchronisation offline */
    @Column(name = "sync_status", length = 20)
    private String syncStatus = "SYNCED";
}
