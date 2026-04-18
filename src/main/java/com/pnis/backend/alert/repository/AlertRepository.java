package com.pnis.backend.alert.repository;

import com.pnis.backend.alert.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    boolean existsByReferenceCode(String ref);

    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tid " +
           "AND (:status   IS NULL OR a.alertStatus = :status) " +
           "AND (:severity IS NULL OR a.severity    = :severity) " +
           "AND (:type     IS NULL OR a.alertType   = :type) " +
           "AND (:from     IS NULL OR a.createdAt   >= :from) " +
           "AND (:to       IS NULL OR a.createdAt   <= :to) " +
           "AND a.deleted = false ORDER BY a.createdAt DESC")
    Page<Alert> search(@Param("tid")      Long tenantId,
                        @Param("status")   String status,
                        @Param("severity") String severity,
                        @Param("type")     String type,
                        @Param("from")     Instant from,
                        @Param("to")       Instant to,
                        Pageable pageable);
}
