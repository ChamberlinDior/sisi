package com.pnis.backend.audit.repository;

import com.pnis.backend.audit.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByTenantId(Long tenantId, Pageable pageable);
    Page<AuditEvent> findByActor(String actor, Pageable pageable);
    Page<AuditEvent> findByObjectTypeAndObjectId(String objectType, Long objectId, Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tid " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:actor  IS NULL OR a.actor  = :actor) " +
           "AND (:from   IS NULL OR a.createdAt >= :from) " +
           "AND (:to     IS NULL OR a.createdAt <= :to) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditEvent> search(@Param("tid")    Long tenantId,
                             @Param("action") String action,
                             @Param("actor")  String actor,
                             @Param("from")   Instant from,
                             @Param("to")     Instant to,
                             Pageable pageable);

    /** Pour le chaînage d'intégrité WORM */
    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tid ORDER BY a.id DESC LIMIT 1")
    Optional<AuditEvent> findLastByTenant(@Param("tid") Long tenantId);

    long countByTenantId(Long tenantId);
}
