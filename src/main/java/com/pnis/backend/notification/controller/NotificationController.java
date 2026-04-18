package com.pnis.backend.notification.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.notification.model.Notification;
import com.pnis.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Gestion des notifications in-app §7.12")
public class NotificationController {

    private final NotificationService notifService;

    public NotificationController(
            NotificationService notifService) {
        this.notifService = notifService;
    }


    @GetMapping
    @Operation(summary = "Mes notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size);
        return ResponseEntity.ok(PageUtils.toPagedResponse(notifService.getMyNotifications(user.getUsername(), p)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal UserDetails user) {
        long count = notifService.countUnread(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unread", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        notifService.markRead(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent("Notification marquée comme lue."));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(
            @AuthenticationPrincipal UserDetails user) {
        int count = notifService.markAllRead(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("marked", count)));
    }
}
