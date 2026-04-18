package com.pnis.backend.notification.repository;

import com.pnis.backend.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    long countByRecipientUsernameAndStatus(String username, Notification.NotifStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipientUsername = :u AND n.status <> 'READ'")
    int markAllAsRead(@Param("u") String username);
}
