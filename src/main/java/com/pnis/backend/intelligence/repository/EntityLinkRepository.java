package com.pnis.backend.intelligence.repository;

import com.pnis.backend.intelligence.model.EntityLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EntityLinkRepository extends JpaRepository<EntityLink, Long> {

    List<EntityLink> findBySourceEntityId(Long sourceId);
    List<EntityLink> findByTargetEntityId(Long targetId);

    @Query("SELECT l FROM EntityLink l WHERE l.sourceEntity.id = :id OR l.targetEntity.id = :id")
    List<EntityLink> findAllRelatedLinks(@Param("id") Long entityId);

    @Query("SELECT l FROM EntityLink l WHERE (l.sourceEntity.id = :src AND l.targetEntity.id = :tgt) " +
           "OR (l.sourceEntity.id = :tgt AND l.targetEntity.id = :src)")
    List<EntityLink> findBetween(@Param("src") Long sourceId, @Param("tgt") Long targetId);
}
