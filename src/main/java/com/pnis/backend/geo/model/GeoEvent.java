package com.pnis.backend.geo.model;

import com.pnis.backend.common.model.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Événement géolocalisé – §7.7 cartographie, SIG, mobilité.
 * Connexion §8 : Cartographie → Alertes (entrée/sortie zone sensible).
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "geo_events", indexes = {
        @Index(name = "idx_geo_tenant",   columnList = "tenant_id"),
        @Index(name = "idx_geo_type",     columnList = "event_type"),
        @Index(name = "idx_geo_entity",   columnList = "entity_id"),
        @Index(name = "idx_geo_case",     columnList = "case_id"),
        @Index(name = "idx_geo_created",  columnList = "created_at")
})
public class GeoEvent extends AbstractBaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 3000)
    private String description;

    /** INCIDENT, MOVEMENT, CHECKPOINT, GEOFENCE_ENTRY, GEOFENCE_EXIT, OBSERVATION, OPERATION */
    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /** Altitude en mètres */
    private Double altitude;

    /** Précision GPS en mètres */
    private Double accuracy;

    /** Rayon de la zone concernée (pour géofencing) */
    @Column(name = "radius_meters")
    private Double radiusMeters;

    /** Entité impliquée */
    @Column(name = "entity_id")
    private Long entityId;

    /** Dossier associé */
    @Column(name = "case_id")
    private Long caseId;

    /** Alerte déclenchée si géofencing */
    @Column(name = "triggered_alert_id")
    private Long triggeredAlertId;

    /** Horodatage de l'événement réel */
    @Column(name = "event_timestamp")
    private Instant eventTimestamp;

    /** Adresse lisible */
    @Column(name = "address_label", length = 500)
    private String addressLabel;

    /** Pays, région, commune */
    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String city;

    /** Données brutes GeoJSON optionnel */
    @Column(name = "geojson", columnDefinition = "TEXT")
    private String geojson;
}
