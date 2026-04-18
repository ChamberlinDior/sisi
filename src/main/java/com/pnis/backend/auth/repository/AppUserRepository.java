package com.pnis.backend.auth.repository;

import com.pnis.backend.auth.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Page<AppUser> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE u.tenant.id = :tenantId AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<AppUser> searchByTenant(@Param("tenantId") Long tenantId,
                                 @Param("q") String q,
                                 Pageable pageable);
}
