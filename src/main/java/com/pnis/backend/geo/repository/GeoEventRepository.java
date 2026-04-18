package com.pnis.backend.geo.repository;

import com.pnis.backend.geo.model.GeoEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface GeoEventRepository extends JpaRepository<GeoEvent, Long> {

    Page<GeoEvent> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);
    Page<GeoEvent> findByEntityIdAndDeletedFalse(Long entityId, Pageable pageable);
    Page<GeoEvent> findByCaseIdAndDeletedFalse(Long caseId, Pageable pageable);

    @Query("SELECT g FROM GeoEvent g WHERE g.tenantId = :tid AND g.deleted = false " +
           "AND (:type IS NULL OR g.eventType = :type) " +
           "AND (:from IS NULL OR g.eventTimestamp >= :from) " +
           "AND (:to   IS NULL OR g.eventTimestamp <= :to) " +
           "ORDER BY g.eventTimestamp DESC")
    Page<GeoEvent> search(@Param("tid")  Long tenantId,
                           @Param("type") String type,
                           @Param("from") Instant from,
                           @Param("to")   Instant to,
                           Pageable pageable);

    /** Événements dans un rayon (Haversine approximation) */
    @Query(value = "SELECT * FROM geo_events g WHERE g.tenant_id = :tid AND g.deleted = 0 " +
           "AND (6371000 * acos(cos(radians(:lat)) * cos(radians(g.latitude)) * " +
           "cos(radians(g.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(g.latitude)))) <= :radius",
           nativeQuery = true)
    java.util.List<GeoEvent> findWithinRadius(@Param("tid")    Long tenantId,
                                               @Param("lat")    double lat,
                                               @Param("lon")    double lon,
                                               @Param("radius") double radiusMeters);
}
