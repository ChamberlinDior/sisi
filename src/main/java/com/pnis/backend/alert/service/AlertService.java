package com.pnis.backend.alert.service;

import com.pnis.backend.alert.model.Alert;
import com.pnis.backend.alert.repository.AlertRepository;
import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.BadRequestException;
import com.pnis.backend.common.exception.ConflictException;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Service des alertes.
 * Connexion §8 : Analyse → Alertes, Cartographie → Alertes, Interface commune → Multi-tenant.
 */
@Slf4j
@Service
public class AlertService {

    private final AlertRepository    alertRepo;
    private final AuditService       auditService;
    private final NotificationService notifService;

    public AlertService(
            AlertRepository alertRepo,
            AuditService auditService,
            NotificationService notifService) {
        this.alertRepo = alertRepo;
        this.auditService = auditService;
        this.notifService = notifService;
    }


    @Transactional
    public Alert create(Alert alert) {
        Long tenantId = TenantContext.getTenantId();
        alert.setTenantId(tenantId);
        alert.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        alert.setAlertStatus("DRAFT");

        if (alert.getReferenceCode() == null || alert.getReferenceCode().isBlank()) {
            alert.setReferenceCode("ALT-" + System.currentTimeMillis() / 1000 + "-" +
                    UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        } else if (alertRepo.existsByReferenceCode(alert.getReferenceCode())) {
            throw new ConflictException("Référence alerte déjà utilisée : " + alert.getReferenceCode());
        }

        Alert saved = alertRepo.save(alert);
        auditService.log("ALERT_CREATED", "Alert", saved.getId(), saved.getUuid(),
                "severity=" + saved.getSeverity() + ", type=" + saved.getAlertType());
        return saved;
    }

    /** Diffuser une alerte (DRAFT → ACTIVE) */
    @Transactional
    public Alert publish(Long id) {
        Alert alert = getById(id);
        if (!"DRAFT".equals(alert.getAlertStatus()) && !"PENDING_VERIFICATION".equals(alert.getAlertStatus())) {
            throw new BadRequestException("Seules les alertes DRAFT ou PENDING_VERIFICATION peuvent être diffusées.");
        }
        alert.setAlertStatus("ACTIVE");
        alert.setPublishedAt(Instant.now());
        Alert saved = alertRepo.save(alert);

        // §8 : notifier les destinataires
        if (alert.getRecipients() != null) {
            Arrays.stream(alert.getRecipients().split(","))
                    .map(String::trim)
                    .filter(r -> !r.isBlank())
                    .forEach(recipient -> notifService.sendAlertNotification(
                            recipient, alert.getTenantId(),
                            alert.getId(), alert.getTitle(), alert.getSeverity()));
        }

        auditService.log("ALERT_PUBLISHED", "Alert", id, alert.getUuid(),
                "severity=" + alert.getSeverity());
        return saved;
    }

    /** Accusé de réception */
    @Transactional
    public Alert acknowledge(Long id) {
        Alert alert = getById(id);
        alert.setAlertStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(SecurityUtils.getCurrentUsernameOrSystem());
        alert.setAcknowledgedAt(Instant.now());
        Alert saved = alertRepo.save(alert);
        auditService.log("ALERT_ACKNOWLEDGED", "Alert", id, alert.getUuid(),
                "by=" + alert.getAcknowledgedBy());
        return saved;
    }

    /** Levée de l'alerte */
    @Transactional
    public Alert lift(Long id, String reason) {
        Alert alert = getById(id);
        if ("LIFTED".equals(alert.getAlertStatus()) || "CANCELLED".equals(alert.getAlertStatus())) {
            throw new BadRequestException("Cette alerte est déjà levée ou annulée.");
        }
        alert.setAlertStatus("LIFTED");
        alert.setLiftedReason(reason);
        alert.setLiftedAt(Instant.now());
        Alert saved = alertRepo.save(alert);
        auditService.log("ALERT_LIFTED", "Alert", id, alert.getUuid(), "reason=" + reason);
        return saved;
    }

    /** Déclenchement automatique depuis l'analyse (§8 Analyse → Alertes) */
    @Transactional
    public Alert triggerAutoAlert(Long tenantId, String title, String body, String severity,
                                   String alertType, String triggerKeywords, Long caseId) {
        Alert alert = new Alert();
        alert.setTenantId(tenantId);
        alert.setTitle(title);
        alert.setBody(body);
        alert.setSeverity(severity != null ? severity : "MEDIUM");
        alert.setAlertType(alertType != null ? alertType : "KEYWORD");
        alert.setTriggerKeywords(triggerKeywords);
        alert.setCaseId(caseId);
        alert.setIsAutoGenerated(true);
        alert.setAlertStatus("ACTIVE");
        alert.setPublishedAt(Instant.now());
        alert.setReferenceCode("AUTO-" + System.currentTimeMillis() / 1000 + "-" +
                UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        alert.setCreatedBy("SYSTEM");
        Alert saved = alertRepo.save(alert);
        auditService.log("ALERT_AUTO_TRIGGERED", "Alert", saved.getId(), saved.getUuid(),
                "keywords=" + triggerKeywords);
        return saved;
    }

    @Transactional(readOnly = true)
    public Alert getById(Long id) {
        return alertRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }

    @Transactional(readOnly = true)
    public Page<Alert> search(Long tenantId, String status, String severity, String type,
                               Instant from, Instant to, Pageable pageable) {
        return alertRepo.search(tenantId, status, severity, type, from, to, pageable);
    }
}
