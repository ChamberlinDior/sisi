package com.pnis.backend.corridor.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.BadRequestException;
import com.pnis.backend.common.exception.InsufficientClearanceException;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.HashUtils;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.corridor.model.CorridorMessage;
import com.pnis.backend.corridor.repository.CorridorMessageRepository;
import com.pnis.backend.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Canal privé institutionnel – §7.9.
 * Chiffrement applicatif AES-256 (simulation par Base64+hash en attendant
 * l'intégration d'un KMS souverain).
 * Connexion §8 : Couloir privé → Identity & Access (contrôle hyper restreint + traçabilité séparée).
 */
@Slf4j
@Service
public class CorridorService {

    private final CorridorMessageRepository corridorRepo;
    private final AuditService              auditService;
    private final NotificationService       notifService;

    public CorridorService(
            CorridorMessageRepository corridorRepo,
            AuditService auditService,
            NotificationService notifService) {
        this.corridorRepo = corridorRepo;
        this.auditService = auditService;
        this.notifService = notifService;
    }


    // =========================================================
    // ENVOI
    // =========================================================
    @Transactional
    public CorridorMessage send(String recipientUsername, String subject, String plainBody,
                                 Integer ttlHours, Boolean readReceiptRequired, String priority) {
        String sender = SecurityUtils.getCurrentUsernameOrSystem();

        // Génération de l'ID de canal déterministe (ordre alphabétique pour symétrie)
        String channelId = buildChannelId(sender, recipientUsername,
                TenantContext.getTenantId());

        // Chiffrement applicatif (Base64 + marquage – remplacer par AES-256/KMS en prod)
        String encryptedBody = encryptBody(plainBody, channelId);

        CorridorMessage msg = new CorridorMessage();
        msg.setTenantId(TenantContext.getTenantId());
        msg.setChannelId(channelId);
        msg.setSenderUsername(sender);
        msg.setRecipientUsername(recipientUsername);
        msg.setSubject(subject);
        msg.setEncryptedBody(encryptedBody);
        msg.setIsEncrypted(true);
        msg.setMessageStatus("SENT");
        msg.setSentAt(Instant.now());
        msg.setTtlHours(ttlHours != null ? ttlHours : 0);
        msg.setReadReceiptRequired(readReceiptRequired != null && readReceiptRequired);
        msg.setForwardForbidden(true);
        msg.setPriority(priority != null ? priority : "NORMAL");

        if (ttlHours != null && ttlHours > 0) {
            msg.setExpiresAt(Instant.now().plusSeconds(ttlHours * 3600L));
        }

        CorridorMessage saved = corridorRepo.save(msg);

        // Audit séparé §7.9 (journal séparé pour le canal privé)
        auditService.log("CORRIDOR_MESSAGE_SENT", "CorridorMessage", saved.getId(), null,
                "from=" + sender + ", to=" + recipientUsername + ", channel=" + channelId);

        // Notification temps réel
        notifService.send(recipientUsername, msg.getTenantId(),
                "Message sécurisé – " + subject,
                "Vous avez reçu un message confidentiel de " + sender,
                "CORRIDOR", "CorridorMessage", saved.getId(), "CRITICAL");

        return saved;
    }

    // =========================================================
    // LECTURE (accusé de lecture)
    // =========================================================
    @Transactional
    public CorridorMessage read(Long id) {
        String currentUser = SecurityUtils.getCurrentUsernameOrSystem();
        CorridorMessage msg = getById(id);

        // Seul le destinataire peut lire
        if (!msg.getRecipientUsername().equals(currentUser) &&
            !msg.getSenderUsername().equals(currentUser)) {
            throw new InsufficientClearanceException("Accès au message refusé.");
        }

        // Vérification expiration
        if (msg.getExpiresAt() != null && Instant.now().isAfter(msg.getExpiresAt())) {
            msg.setMessageStatus("EXPIRED");
            corridorRepo.save(msg);
            throw new BadRequestException("Ce message a expiré.");
        }

        // Accusé de lecture
        if ("SENT".equals(msg.getMessageStatus()) || "DELIVERED".equals(msg.getMessageStatus())) {
            if (msg.getRecipientUsername().equals(currentUser)) {
                msg.setMessageStatus("READ");
                msg.setReadAt(Instant.now());
                corridorRepo.save(msg);
            }
        }

        auditService.log("CORRIDOR_MESSAGE_READ", "CorridorMessage", id, null,
                "by=" + currentUser);

        // Déchiffrement pour retour
        msg.setEncryptedBody(decryptBody(msg.getEncryptedBody(), msg.getChannelId()));
        return msg;
    }

    // =========================================================
    // RÉVOCATION
    // =========================================================
    @Transactional
    public CorridorMessage revoke(Long id, String reason) {
        String currentUser = SecurityUtils.getCurrentUsernameOrSystem();
        CorridorMessage msg = getById(id);

        if (!msg.getSenderUsername().equals(currentUser)) {
            throw new InsufficientClearanceException("Seul l'expéditeur peut révoquer ce message.");
        }
        if ("REVOKED".equals(msg.getMessageStatus())) {
            throw new BadRequestException("Message déjà révoqué.");
        }

        msg.setMessageStatus("REVOKED");
        msg.setRevokedAt(Instant.now());
        msg.setRevokedBy(currentUser);
        msg.setRevokeReason(reason);
        msg.setEncryptedBody("[RÉVOQUÉ]");
        CorridorMessage saved = corridorRepo.save(msg);

        auditService.log("CORRIDOR_MESSAGE_REVOKED", "CorridorMessage", id, null,
                "by=" + currentUser + ", reason=" + reason);
        return saved;
    }

    // =========================================================
    // LECTURE
    // =========================================================
    @Transactional(readOnly = true)
    public Page<CorridorMessage> getMyMessages(String username, Pageable pageable) {
        return corridorRepo.findMyMessages(username, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CorridorMessage> getChannel(String channelId, Pageable pageable) {
        return corridorRepo.findByChannel(channelId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return corridorRepo.countByRecipientUsernameAndMessageStatus(username, "SENT");
    }

    @Transactional(readOnly = true)
    public CorridorMessage getById(Long id) {
        return corridorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CorridorMessage", id));
    }

    // =========================================================
    // CHIFFREMENT (simulation – remplacer par AES-256/KMS souverain)
    // =========================================================
    private String encryptBody(String plainText, String channelId) {
        String key = HashUtils.sha256Hex((channelId + "PNIS-KEY").getBytes());
        String payload = key.substring(0, 8) + "|" + plainText;
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String decryptBody(String encrypted, String channelId) {
        try {
            if ("[RÉVOQUÉ]".equals(encrypted)) return "[RÉVOQUÉ]";
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            int sep = payload.indexOf('|');
            return sep >= 0 ? payload.substring(sep + 1) : payload;
        } catch (Exception e) {
            return "[Erreur de déchiffrement]";
        }
    }

    private String buildChannelId(String userA, String userB, Long tenantId) {
        String sorted = userA.compareTo(userB) <= 0
                ? userA + ":" + userB
                : userB + ":" + userA;
        return "CH-" + tenantId + "-" + HashUtils.sha256Hex(sorted.getBytes()).substring(0, 16);
    }
}
