package com.pnis.backend.corridor.repository;

import com.pnis.backend.corridor.model.CorridorMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorridorMessageRepository extends JpaRepository<CorridorMessage, Long> {

    @Query("SELECT m FROM CorridorMessage m WHERE " +
           "(m.senderUsername = :u OR m.recipientUsername = :u) " +
           "AND m.messageStatus NOT IN ('REVOKED','EXPIRED') " +
           "ORDER BY m.createdAt DESC")
    Page<CorridorMessage> findMyMessages(@Param("u") String username, Pageable pageable);

    @Query("SELECT m FROM CorridorMessage m WHERE m.channelId = :ch " +
           "ORDER BY m.createdAt ASC")
    Page<CorridorMessage> findByChannel(@Param("ch") String channelId, Pageable pageable);

    long countByRecipientUsernameAndMessageStatus(String username, String status);
}
