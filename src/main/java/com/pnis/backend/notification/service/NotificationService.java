package com.pnis.backend.notification.service;

import com.pnis.backend.common.config.AppProperties;
import com.pnis.backend.notification.model.Notification;
import com.pnis.backend.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Service de notification – connexion obligatoire §8 : Workflow → Notifications.
 * Chaque transition workflow importante notifie les acteurs concernés.
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final SimpMessagingTemplate  messagingTemplate;
    private final AppProperties          appProperties;

    public NotificationService(
            NotificationRepository notifRepo,
            SimpMessagingTemplate messagingTemplate,
            AppProperties appProperties) {
        this.notifRepo = notifRepo;
        this.messagingTemplate = messagingTemplate;
        this.appProperties = appProperties;
    }


    /**
     * Crée et dispatche une notification vers un utilisateur.
     * Appelé par Workflow, Alert, Corridor, Publication, etc.
     */
    @Async("notificationExecutor")
    @Transactional
    public void send(String recipientUsername, Long tenantId, String title, String body,
                     String eventType, String sourceType, Long sourceId, String priority) {
        Notification notif = new Notification();
        notif.setTenantId(tenantId);
        notif.setRecipientUsername(recipientUsername);
        notif.setTitle(title);
        notif.setBody(body);
        notif.setEventType(eventType);
        notif.setSourceType(sourceType);
        notif.setSourceId(sourceId);
        notif.setPriority(priority != null ? priority : "INFO");
        notif.setChannel("IN_APP");
        notif.setStatus(Notification.NotifStatus.SENT);
        notif.setSentAt(Instant.now());

        notifRepo.save(notif);

        // Push WebSocket
        if (appProperties.getNotification().isWebsocketEnabled()) {
            try {
                messagingTemplate.convertAndSendToUser(
                        recipientUsername,
                        "/queue/notifications",
                        Map.of("id", notif.getId(), "title", title, "body", body,
                               "eventType", eventType, "priority", notif.getPriority(),
                               "createdAt", notif.getCreatedAt()));
            } catch (Exception e) {
                log.warn("[NOTIF] Impossible d'envoyer WS à {} : {}", recipientUsername, e.getMessage());
            }
        }
    }

    /** Raccourci pour les événements workflow */
    public void sendWorkflowNotification(String recipient, Long tenantId,
                                          String objectType, Long objectId, String transition) {
        send(recipient, tenantId,
             "Mise à jour – " + objectType,
             "Le statut de " + objectType + " #" + objectId + " a changé : " + transition,
             "WORKFLOW", objectType, objectId, "INFO");
    }

    /** Raccourci pour les alertes */
    public void sendAlertNotification(String recipient, Long tenantId,
                                       Long alertId, String alertTitle, String severity) {
        send(recipient, tenantId,
             "Alerte : " + alertTitle,
             "Une nouvelle alerte de niveau " + severity + " a été créée.",
             "ALERT", "Alert", alertId, mapSeverityToPriority(severity));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getMyNotifications(String username, Pageable pageable) {
        return notifRepo.findByRecipientUsernameOrderByCreatedAtDesc(username, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return notifRepo.countByRecipientUsernameAndStatus(username, Notification.NotifStatus.SENT);
    }

    @Transactional
    public int markAllRead(String username) {
        return notifRepo.markAllAsRead(username);
    }

    @Transactional
    public void markRead(Long id, String username) {
        notifRepo.findById(id).ifPresent(n -> {
            if (n.getRecipientUsername().equals(username)) {
                n.setStatus(Notification.NotifStatus.READ);
                n.setReadAt(Instant.now());
                notifRepo.save(n);
            }
        });
    }

    private String mapSeverityToPriority(String severity) {
        return switch (severity != null ? severity.toUpperCase() : "") {
            case "CRITICAL", "URGENT" -> "CRITICAL";
            case "HIGH"               -> "WARNING";
            default                   -> "INFO";
        };
    }
}
