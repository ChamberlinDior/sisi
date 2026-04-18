package com.pnis.backend.intelligence.repository;

import com.pnis.backend.intelligence.model.EntityAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EntityAliasRepository extends JpaRepository<EntityAlias, Long> {
    List<EntityAlias> findByEntityId(Long entityId);

    @Query("SELECT a FROM EntityAlias a WHERE LOWER(a.aliasValue) LIKE LOWER(CONCAT('%',:val,'%'))")
    List<EntityAlias> findByAliasValueContaining(@Param("val") String value);
}
