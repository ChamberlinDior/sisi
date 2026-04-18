package com.pnis.backend.tenant.repository;

import com.pnis.backend.tenant.model.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByTenantId(Long tenantId);
    Page<Unit> findByTenantId(Long tenantId, Pageable pageable);
    List<Unit> findByParentUnitId(Long parentUnitId);
    boolean existsByCodeAndTenantId(String code, Long tenantId);
}
