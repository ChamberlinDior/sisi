package com.pnis.backend.tenant.repository;

import com.pnis.backend.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByCode(String code);
    boolean existsByCode(String code);
}
