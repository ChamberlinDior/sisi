package com.pnis.backend.publication.repository;

import com.pnis.backend.publication.model.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    boolean existsByReferenceCode(String ref);

    @Query("SELECT p FROM Publication p WHERE " +
           "(:tid IS NULL OR p.tenantId = :tid) " +
           "AND (:status IS NULL OR p.pubStatus = :status) " +
           "AND (:type   IS NULL OR p.pubType   = :type) " +
           "AND p.deleted = false ORDER BY p.publishedAt DESC NULLS LAST")
    Page<Publication> search(@Param("tid")    Long tenantId,
                              @Param("status") String status,
                              @Param("type")   String type,
                              Pageable pageable);

    /** Publications visibles par un tenant cible (interface commune) */
    @Query("SELECT p FROM Publication p WHERE p.pubStatus = 'PUBLISHED' AND p.deleted = false " +
           "AND (p.targetTenants = 'ALL_TENANTS' " +
           "     OR p.targetTenants LIKE CONCAT('%',:tenantCode,'%'))")
    Page<Publication> findVisibleFor(@Param("tenantCode") String tenantCode, Pageable pageable);
}
