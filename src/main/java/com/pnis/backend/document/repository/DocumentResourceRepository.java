package com.pnis.backend.document.repository;

import com.pnis.backend.document.model.DocumentResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentResourceRepository extends JpaRepository<DocumentResource, Long> {
    Page<DocumentResource> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);
    List<DocumentResource> findByOwnerTypeAndOwnerIdAndDeletedFalse(String ownerType, Long ownerId);
    Optional<DocumentResource> findByFileHash(String fileHash);
    List<DocumentResource> findByOwnerTypeAndOwnerIdAndDocStatus(String ownerType, Long ownerId, String status);
}
