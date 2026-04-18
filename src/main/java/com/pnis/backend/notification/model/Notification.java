package com.pnis.backend.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Message de notification in-app, email ou SMS.
 * Conforme §7.12 : événements métier, bus interne, anti-bruit, accusés.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient", columnList = "recipient_username"),
        @Index(name = "idx_notif_tenant",    columnList = "tenant_id"),
        @Index(name = "idx_notif_status",    columnList = "status"),
        @Index(name = "idx_notif_read",      columnList = "read_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "recipient_username", nullable = false, length = 100)
    private String recipientUsername;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    /** ALERT, WORKFLOW, CASE, CORRIDOR, PUBLICATION, SYSTEM */
    @Column(name = "event_type", length = 50)
    private String eventType;

    /** Objet source lié */
    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    /** INFO, WARNING, CRITICAL, URGENT */
    @Column(length = 20)
    private String priority = "INFO";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotifStatus status = NotifStatus.PENDING;

    /** Canal : IN_APP, EMAIL, SMS */
    @Column(length = 20)
    private String channel = "IN_APP";

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }

    public enum NotifStatus { PENDING, SENT, DELIVERED, READ, FAILED, EXPIRED }
}
