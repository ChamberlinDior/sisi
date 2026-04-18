package com.pnis.backend.auth.repository;

import com.pnis.backend.auth.model.AppRole;
import com.pnis.backend.auth.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {
    Optional<AppRole> findByName(RoleName name);
    boolean existsByName(RoleName name);
}
