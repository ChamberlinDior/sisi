package com.pnis.backend.tenant.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    private Unit parentUnit;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }

    // ===== GETTERS =====
    public Long    getId()          { return id; }
    public Tenant  getTenant()      { return tenant; }
    public Unit    getParentUnit()  { return parentUnit; }
    public String  getCode()        { return code; }
    public String  getName()        { return name; }
    public String  getDescription() { return description; }
    public boolean isActive()       { return active; }
    public Instant getCreatedAt()   { return createdAt; }

    // ===== SETTERS =====
    public void setId(Long id)                 { this.id = id; }
    public void setTenant(Tenant tenant)       { this.tenant = tenant; }
    public void setParentUnit(Unit parentUnit) { this.parentUnit = parentUnit; }
    public void setCode(String code)           { this.code = code; }
    public void setName(String name)           { this.name = name; }
    public void setDescription(String desc)    { this.description = desc; }
    public void setActive(boolean active)      { this.active = active; }
}
