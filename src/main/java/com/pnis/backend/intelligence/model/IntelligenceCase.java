package com.pnis.backend.intelligence.model;

import com.pnis.backend.auth.model.AppUser;
import com.pnis.backend.common.model.AbstractBaseEntity;
import com.pnis.backend.tenant.model.Unit;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "intelligence_cases", indexes = {
        @Index(name = "idx_case_tenant",  columnList = "tenant_id"),
        @Index(name = "idx_case_ref",     columnList = "reference_code", unique = true),
        @Index(name = "idx_case_status",  columnList = "case_status"),
        @Index(name = "idx_case_lead",    columnList = "lead_officer_id")
})
public class IntelligenceCase extends AbstractBaseEntity {

    @Column(name = "reference_code", nullable = false, unique = true, length = 80)
    private String referenceCode;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 8000)
    private String description;

    @Column(name = "case_type", length = 50)
    private String caseType;

    @Column(name = "case_status", nullable = false, length = 30)
    private String caseStatus = "OPEN";

    @Column(name = "priority_level", length = 20)
    private String priorityLevel = "MEDIUM";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_unit_id")
    private Unit ownerUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_id")
    private AppUser openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_officer_id")
    private AppUser leadOfficer;

    @Column(name = "target_summary", length = 2000)
    private String targetSummary;

    @Column(name = "objective", length = 2000)
    private String objective;

    @Column(name = "geo_area", length = 500)
    private String geoArea;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closure_reason", length = 2000)
    private String closureReason;

    @Column(name = "parent_case_id")
    private Long parentCaseId;

    @Override
    @PrePersist
    protected void prePersist() {
        super.prePersist();
        if (openedAt == null) openedAt = Instant.now();
    }

    // ===== GETTERS =====
    public String  getReferenceCode() { return referenceCode; }
    public String  getTitle()         { return title; }
    public String  getDescription()   { return description; }
    public String  getCaseType()      { return caseType; }
    public String  getCaseStatus()    { return caseStatus; }
    public String  getPriorityLevel() { return priorityLevel; }
    public Unit    getOwnerUnit()     { return ownerUnit; }
    public AppUser getOpenedBy()      { return openedBy; }
    public AppUser getLeadOfficer()   { return leadOfficer; }
    public String  getTargetSummary() { return targetSummary; }
    public String  getObjective()     { return objective; }
    public String  getGeoArea()       { return geoArea; }
    public Instant getOpenedAt()      { return openedAt; }
    public Instant getDeadline()      { return deadline; }
    public Instant getClosedAt()      { return closedAt; }
    public String  getClosureReason() { return closureReason; }
    public Long    getParentCaseId()  { return parentCaseId; }

    // ===== SETTERS =====
    public void setReferenceCode(String v)  { this.referenceCode = v; }
    public void setTitle(String v)          { this.title = v; }
    public void setDescription(String v)    { this.description = v; }
    public void setCaseType(String v)       { this.caseType = v; }
    public void setCaseStatus(String v)     { this.caseStatus = v; }
    public void setPriorityLevel(String v)  { this.priorityLevel = v; }
    public void setOwnerUnit(Unit v)        { this.ownerUnit = v; }
    public void setOpenedBy(AppUser v)      { this.openedBy = v; }
    public void setLeadOfficer(AppUser v)   { this.leadOfficer = v; }
    public void setTargetSummary(String v)  { this.targetSummary = v; }
    public void setObjective(String v)      { this.objective = v; }
    public void setGeoArea(String v)        { this.geoArea = v; }
    public void setOpenedAt(Instant v)      { this.openedAt = v; }
    public void setDeadline(Instant v)      { this.deadline = v; }
    public void setClosedAt(Instant v)      { this.closedAt = v; }
    public void setClosureReason(String v)  { this.closureReason = v; }
    public void setParentCaseId(Long v)     { this.parentCaseId = v; }
}