package com.pnis.backend.common.config;

import com.pnis.backend.common.model.AbstractBaseEntity;
import com.pnis.backend.common.util.SecurityUtils;
import jakarta.persistence.*;

/**
 * JPA EntityListener : alimente automatiquement createdBy / updatedBy
 * depuis le contexte de sécurité Spring.
 */
public class AuditableEntityListener {

    @PrePersist
    public void prePersist(AbstractBaseEntity entity) {
        String user = SecurityUtils.getCurrentUsernameOrSystem();
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
    }

    @PreUpdate
    public void preUpdate(AbstractBaseEntity entity) {
        entity.setUpdatedBy(SecurityUtils.getCurrentUsernameOrSystem());
    }
}
