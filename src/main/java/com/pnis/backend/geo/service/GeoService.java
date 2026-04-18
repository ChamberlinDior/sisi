package com.pnis.backend.geo.service;

import com.pnis.backend.alert.service.AlertService;
import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.geo.model.GeoEvent;
import com.pnis.backend.geo.repository.GeoEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class GeoService {

    private final GeoEventRepository geoRepo;
    private final AuditService       auditService;
    private final AlertService       alertService;

    public GeoService(
            GeoEventRepository geoRepo,
            AuditService auditService,
            AlertService alertService) {
        this.geoRepo = geoRepo;
        this.auditService = auditService;
        this.alertService = alertService;
    }


    @Transactional
    public GeoEvent create(GeoEvent event) {
        event.setTenantId(TenantContext.getTenantId());
        event.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        if (event.getEventTimestamp() == null) event.setEventTimestamp(Instant.now());

        GeoEvent saved = geoRepo.save(event);
        auditService.log("GEO_EVENT_CREATED", "GeoEvent", saved.getId(), saved.getUuid(),
                "type=" + saved.getEventType() + ", lat=" + saved.getLatitude() + ", lon=" + saved.getLongitude());

        // §8 : Cartographie → Alertes (géofencing)
        if ("GEOFENCE_ENTRY".equals(event.getEventType()) || "GEOFENCE_EXIT".equals(event.getEventType())) {
            try {
                var alert = alertService.triggerAutoAlert(
                        event.getTenantId(),
                        "[GÉOFENCE] " + event.getEventType() + " – " + event.getTitle(),
                        "Événement géographique détecté : " + event.getEventType() +
                        " à " + event.getLatitude() + "/" + event.getLongitude(),
                        "HIGH", "GEO_FENCE", null, event.getCaseId());
                saved.setTriggeredAlertId(alert.getId());
                geoRepo.save(saved);
                log.info("[GEO] Alerte géofencing créée #{} pour événement #{}", alert.getId(), saved.getId());
            } catch (Exception e) {
                log.error("[GEO] Erreur création alerte géofencing : {}", e.getMessage());
            }
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public GeoEvent getById(Long id) {
        return geoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GeoEvent", id));
    }

    @Transactional(readOnly = true)
    public Page<GeoEvent> search(Long tenantId, String type, Instant from, Instant to, Pageable p) {
        return geoRepo.search(tenantId, type, from, to, p);
    }

    @Transactional(readOnly = true)
    public List<GeoEvent> findWithinRadius(double lat, double lon, double radiusMeters) {
        return geoRepo.findWithinRadius(TenantContext.getTenantId(), lat, lon, radiusMeters);
    }

    @Transactional(readOnly = true)
    public Page<GeoEvent> getByEntity(Long entityId, Pageable p) {
        return geoRepo.findByEntityIdAndDeletedFalse(entityId, p);
    }

    @Transactional(readOnly = true)
    public Page<GeoEvent> getByCase(Long caseId, Pageable p) {
        return geoRepo.findByCaseIdAndDeletedFalse(caseId, p);
    }
}
