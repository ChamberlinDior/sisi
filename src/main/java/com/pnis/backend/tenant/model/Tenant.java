package com.pnis.backend.tenant.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Institution / tenant de la plateforme.
 * Conforme §7.2 : isolation logique ou renforcée, clés, politiques, contrats de partage.
 */
@Entity
@Table(name = "tenants", indexes = @Index(name = "idx_tenant_code", columnList = "code", unique = true))
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "isolation_mode", length = 30)
    private String isolationMode = "LOGICAL";

    @Column(name = "encryption_key_id", length = 100)
    private String encryptionKeyId;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }

    // ===== GETTERS =====
    public Long    getId()              { return id; }
    public String  getCode()            { return code; }
    public String  getName()            { return name; }
    public String  getDescription()     { return description; }
    public String  getIsolationMode()   { return isolationMode; }
    public String  getEncryptionKeyId() { return encryptionKeyId; }
    public boolean isActive()           { return active; }
    public Instant getCreatedAt()       { return createdAt; }
    public Instant getUpdatedAt()       { return updatedAt; }

    // ===== SETTERS =====
    public void setId(Long id)                       { this.id = id; }
    public void setCode(String code)                 { this.code = code; }
    public void setName(String name)                 { this.name = name; }
    public void setDescription(String description)   { this.description = description; }
    public void setIsolationMode(String mode)        { this.isolationMode = mode; }
    public void setEncryptionKeyId(String keyId)     { this.encryptionKeyId = keyId; }
    public void setActive(boolean active)            { this.active = active; }
}
