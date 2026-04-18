package com.pnis.backend.collection.repository;

import com.pnis.backend.collection.model.CollectedData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CollectedDataRepository extends JpaRepository<CollectedData, Long> {

    Page<CollectedData> findByTenantId(Long tenantId, Pageable pageable);

    Optional<CollectedData> findByDedupHash(String dedupHash);

    @Query("SELECT c FROM CollectedData c WHERE c.tenantId = :tid " +
           "AND (:status IS NULL OR c.recordStatus = :status) " +
           "AND (:dataType IS NULL OR c.dataType = :dataType) " +
           "AND (:from IS NULL OR c.createdAt >= :from) " +
           "AND (:to   IS NULL OR c.createdAt <= :to) " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<CollectedData> search(@Param("tid")      Long tenantId,
                                @Param("status")   String status,
                                @Param("dataType") String dataType,
                                @Param("from")     Instant from,
                                @Param("to")       Instant to,
                                Pageable pageable);

    List<CollectedData> findByImportJobId(Long importJobId);

    long countByTenantIdAndRecordStatus(Long tenantId, com.pnis.backend.common.enums.RecordStatus status);
}
