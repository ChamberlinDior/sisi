package com.pnis.backend.tenant.repository;

import com.pnis.backend.tenant.model.SharingContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SharingContractRepository extends JpaRepository<SharingContract, Long> {

    List<SharingContract> findBySourceTenantIdAndStatus(Long sourceTenantId, SharingContract.ContractStatus status);
    List<SharingContract> findByTargetTenantIdAndStatus(Long targetTenantId, SharingContract.ContractStatus status);

    @Query("SELECT s FROM SharingContract s WHERE s.sourceTenant.id = :src AND s.targetTenant.id = :tgt " +
           "AND s.status = 'ACTIVE' AND (s.objectType = :type OR s.objectType = 'ALL')")
    List<SharingContract> findActiveContract(@Param("src") Long sourceId,
                                              @Param("tgt") Long targetId,
                                              @Param("type") String objectType);
}
