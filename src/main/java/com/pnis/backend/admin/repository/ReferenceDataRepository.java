package com.pnis.backend.admin.repository;

import com.pnis.backend.admin.model.ReferenceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReferenceDataRepository extends JpaRepository<ReferenceData, Long> {

    /** Retourne les entrées système + celles du tenant */
    @Query("SELECT r FROM ReferenceData r WHERE r.category = :cat AND r.active = true " +
           "AND (r.tenantId IS NULL OR r.tenantId = :tid) ORDER BY r.sortOrder, r.label")
    List<ReferenceData> findByCategory(@Param("cat") String category, @Param("tid") Long tenantId);

    List<ReferenceData> findByCategoryAndActiveTrueOrderBySortOrder(String category);

    Optional<ReferenceData> findByCategoryAndCode(String category, String code);

    boolean existsByCategoryAndCode(String category, String code);
}
