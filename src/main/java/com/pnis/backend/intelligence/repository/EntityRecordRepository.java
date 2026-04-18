package com.pnis.backend.intelligence.repository;

import com.pnis.backend.intelligence.model.EntityRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EntityRecordRepository extends JpaRepository<EntityRecord, Long> {

    Optional<EntityRecord> findByReferenceCode(String referenceCode);
    boolean existsByReferenceCode(String referenceCode);

    Page<EntityRecord> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);
    Page<EntityRecord> findByTenantIdAndEntityTypeAndDeletedFalse(Long tenantId, String type, Pageable pageable);

    @Query("SELECT e FROM EntityRecord e WHERE e.tenantId = :tid AND e.deleted = false " +
           "AND (:type IS NULL OR e.entityType = :type) " +
           "AND (LOWER(e.label) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(e.firstName) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(e.contactValue) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(e.plateNumber)  LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<EntityRecord> search(@Param("tid")  Long tenantId,
                               @Param("q")    String q,
                               @Param("type") String type,
                               Pageable pageable);
}
