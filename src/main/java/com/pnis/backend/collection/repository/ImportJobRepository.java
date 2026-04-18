package com.pnis.backend.collection.repository;

import com.pnis.backend.collection.model.ImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    Page<ImportJob> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
    Page<ImportJob> findBySubmittedByOrderByCreatedAtDesc(String submittedBy, Pageable pageable);
}
