package com.pnis.backend.intelligence.model;

import com.pnis.backend.common.model.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entité métier (personne, organisation, véhicule, lieu, événement, téléphone, etc.)
 * Conforme §7.4 : modèle entité–lien–propriété, alias, fusion, score de confiance.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "entity_records", indexes = {
        @Index(name = "idx_entity_tenant",   columnList = "tenant_id"),
        @Index(name = "idx_entity_type",     columnList = "entity_type"),
        @Index(name = "idx_entity_label",    columnList = "label"),
        @Index(name = "idx_entity_ref",      columnList = "reference_code", unique = true),
        @Index(name = "idx_entity_status",   columnList = "record_status")
})
public class EntityRecord extends AbstractBaseEntity {

    @Column(name = "reference_code", nullable = false, unique = true, length = 60)
    private String referenceCode;

    /** PERSON, ORGANIZATION, VEHICLE, LOCATION, PHONE, ACCOUNT, EVENT, DOCUMENT, INCIDENT */
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    /** Libellé principal (nom, immatriculation, adresse, etc.) */
    @Column(nullable = false, length = 300)
    private String label;

    @Column(length = 6000)
    private String description;

    // ===== PERSONNE =====
    @Column(name = "first_name",  length = 100)
    private String firstName;
    @Column(name = "last_name",   length = 100)
    private String lastName;
    @Column(name = "birth_date",  length = 20)
    private String birthDate;
    @Column(name = "birth_place", length = 200)
    private String birthPlace;
    @Column(name = "nationality", length = 100)
    private String nationality;
    @Column(name = "id_number",   length = 100)
    private String idNumber;

    // ===== ORGANISATION =====
    @Column(name = "org_type",   length = 100)
    private String orgType;
    @Column(name = "country",    length = 100)
    private String country;

    // ===== VÉHICULE =====
    @Column(name = "plate_number", length = 50)
    private String plateNumber;
    @Column(name = "vehicle_type", length = 80)
    private String vehicleType;

    // ===== TÉLÉPHONE / COMPTE =====
    @Column(name = "contact_value", length = 200)
    private String contactValue;

    // ===== GÉOLOCALISATION =====
    private Double latitude;
    private Double longitude;

    // ===== SCORING =====
    @Column(name = "confidence_score")
    private Integer confidenceScore = 50;

    @Column(name = "sensitivity_score")
    private Integer sensitivityScore = 0;

    @Column(name = "threat_level", length = 20)
    private String threatLevel;

    // ===== FUSION =====
    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    @Column(name = "is_merged")
    private Boolean isMerged = false;
}
