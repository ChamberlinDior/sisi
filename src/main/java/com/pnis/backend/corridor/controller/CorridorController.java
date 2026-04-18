package com.pnis.backend.corridor.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.corridor.model.CorridorMessage;
import com.pnis.backend.corridor.service.CorridorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/corridor")
@PreAuthorize("hasRole('CORRIDOR_USER')")
@Tag(name = "Couloir Privé", description = "Canal chiffré institutionnel point à point §7.9")
public class CorridorController {

    private final CorridorService corridorService;

    public CorridorController(
            CorridorService corridorService) {
        this.corridorService = corridorService;
    }


    @PostMapping("/send")
    @Operation(summary = "Envoyer un message sécurisé")
    public ResponseEntity<ApiResponse<CorridorMessage>> send(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody Map<String, Object> body) {
        String recipient = (String) body.get("recipientUsername");
        String subject   = (String) body.get("subject");
        String plainBody = (String) body.get("body");
        Integer ttl      = body.get("ttlHours") != null ? Integer.parseInt(body.get("ttlHours").toString()) : null;
        Boolean receipt  = body.get("readReceiptRequired") != null && (Boolean) body.get("readReceiptRequired");
        String priority  = (String) body.getOrDefault("priority", "NORMAL");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        corridorService.send(recipient, subject, plainBody, ttl, receipt, priority)));
    }

    @GetMapping("/messages")
    @Operation(summary = "Mes messages sécurisés (liste – sans déchiffrement)")
    public ResponseEntity<ApiResponse<List<CorridorMessage>>> myMessages(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size);
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                corridorService.getMyMessages(user.getUsername(), p)));
    }

    @GetMapping("/messages/{id}")
    @Operation(summary = "Lire un message (déchiffrement + accusé de lecture)")
    public ResponseEntity<ApiResponse<CorridorMessage>> read(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(corridorService.read(id)));
    }

    @DeleteMapping("/messages/{id}")
    @Operation(summary = "Révoquer un message envoyé")
    public ResponseEntity<ApiResponse<CorridorMessage>> revoke(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Révocation manuelle";
        return ResponseEntity.ok(ApiResponse.ok(corridorService.revoke(id, reason)));
    }

    @GetMapping("/channel/{channelId}")
    @Operation(summary = "Historique d'un canal de conversation")
    public ResponseEntity<ApiResponse<List<CorridorMessage>>> channel(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                corridorService.getChannel(channelId, PageRequest.of(page, size))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de messages non lus")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("unread", corridorService.countUnread(user.getUsername()))));
    }
}
