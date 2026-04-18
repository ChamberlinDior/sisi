package com.pnis.backend.corridor.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Message du canal privé institutionnel – §7.9.
 * Connexion §8 : Couloir privé → Identity & Access (contrôle hyper restreint).
 * Statuts §9.5 : CREATED, ENCRYPTED, SENT, DELIVERED, READ, EXPIRED, REVOKED.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "corridor_messages", indexes = {
        @Index(name = "idx_corridor_sender",    columnList = "sender_username"),
        @Index(name = "idx_corridor_recipient", columnList = "recipient_username"),
        @Index(name = "idx_corridor_channel",   columnList = "channel_id"),
        @Index(name = "idx_corridor_status",    columnList = "message_status"),
        @Index(name = "idx_corridor_tenant",    columnList = "tenant_id")
})
public class CorridorMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    /** Identifiant de canal (conversation entre deux parties) */
    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    @Column(name = "sender_username", nullable = false, length = 100)
    private String senderUsername;

    @Column(name = "recipient_username", nullable = false, length = 100)
    private String recipientUsername;

    @Column(nullable = false, length = 300)
    private String subject;

    /** Corps chiffré côté applicatif (AES-256 simulé – clé par canal) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedBody;

    /** Indicateur que le corps est chiffré */
    @Column(name = "is_encrypted")
    private Boolean isEncrypted = true;

    /** CREATED, ENCRYPTED, SENT, DELIVERED, READ, EXPIRED, REVOKED */
    @Column(name = "message_status", nullable = false, length = 20)
    private String messageStatus = "CREATED";

    /** Accusé de lecture activé */
    @Column(name = "read_receipt_required")
    private Boolean readReceiptRequired = false;

    /** Durée de vie en heures (0 = illimitée) */
    @Column(name = "ttl_hours")
    private Integer ttlHours = 0;

    /** Interdiction de retransfert */
    @Column(name = "forward_forbidden")
    private Boolean forwardForbidden = true;

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
