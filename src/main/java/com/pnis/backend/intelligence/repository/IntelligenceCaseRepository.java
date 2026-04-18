package com.pnis.backend.intelligence.repository;

import com.pnis.backend.intelligence.model.IntelligenceCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IntelligenceCaseRepository extends JpaRepository<IntelligenceCase, Long> {
    Optional<IntelligenceCase> findByReferenceCode(String referenceCode);
    boolean existsByReferenceCode(String referenceCode);

    Page<IntelligenceCase> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);

    @Query("SELECT c FROM IntelligenceCase c WHERE c.tenantId = :tid AND c.deleted = false " +
           "AND (:status IS NULL OR c.caseStatus = :status) " +
           "AND (:type   IS NULL OR c.caseType   = :type) " +
           "AND (:q      IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(c.referenceCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<IntelligenceCase> search(@Param("tid")    Long tenantId,
                                   @Param("status") String status,
                                   @Param("type")   String type,
                                   @Param("q")      String q,
                                   Pageable pageable);
}
