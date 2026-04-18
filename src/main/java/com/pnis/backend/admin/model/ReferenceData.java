package com.pnis.backend.admin.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Données de référence paramétrables – §7.14.
 * Connexion §8 : Administration → Tous modules (paramétrage central).
 *
 * NOTE : getters/setters explicites pour éviter tout problème de génération
 * Lombok avec les champs boolean primitifs et les mots réservés.
 */
@Entity
@Table(name = "reference_data", indexes = {
        @Index(name = "idx_ref_category", columnList = "category"),
        @Index(name = "idx_ref_tenant",   columnList = "tenant_id"),
        @Index(name = "idx_ref_code",     columnList = "code"),
        @Index(name = "idx_ref_active",   columnList = "active")
})
public class ReferenceData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 300)
    private String label;

    @Column(length = 1000)
    private String description;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "is_system")
    private boolean systemEntry = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist void prePersist() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }

    // ===== GETTERS =====
    public Long    getId()          { return id; }
    public Long    getTenantId()    { return tenantId; }
    public String  getCategory()    { return category; }
    public String  getCode()        { return code; }
    public String  getLabel()       { return label; }
    public String  getDescription() { return description; }
    public String  getExtraData()   { return extraData; }
    public Integer getSortOrder()   { return sortOrder; }
    public boolean isActive()       { return active; }
    public boolean isSystem()       { return systemEntry; }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getUpdatedAt()   { return updatedAt; }
    public String  getCreatedBy()   { return createdBy; }

    // ===== SETTERS =====
    public void setId(Long id)                   { this.id = id; }
    public void setTenantId(Long tenantId)       { this.tenantId = tenantId; }
    public void setCategory(String category)     { this.category = category; }
    public void setCode(String code)             { this.code = code; }
    public void setLabel(String label)           { this.label = label; }
    public void setDescription(String desc)      { this.description = desc; }
    public void setExtraData(String extraData)   { this.extraData = extraData; }
    public void setSortOrder(Integer sortOrder)  { this.sortOrder = sortOrder; }
    public void setActive(boolean active)        { this.active = active; }
    public void setSystem(boolean systemEntry)   { this.systemEntry = systemEntry; }
    public void setCreatedBy(String createdBy)   { this.createdBy = createdBy; }
    public void setUpdatedAt(Instant updatedAt)  { this.updatedAt = updatedAt; }
}
