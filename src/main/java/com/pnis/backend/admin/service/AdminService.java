package com.pnis.backend.admin.service;

import com.pnis.backend.admin.model.ReferenceData;
import com.pnis.backend.admin.repository.ReferenceDataRepository;
import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.exception.*;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.tenant.model.Tenant;
import com.pnis.backend.tenant.model.Unit;
import com.pnis.backend.tenant.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AdminService {

    private final ReferenceDataRepository refRepo;
    private final TenantRepository        tenantRepo;
    private final UnitRepository          unitRepo;
    private final SharingContractRepository sharingRepo;
    private final AuditService            auditService;

    public AdminService(
            ReferenceDataRepository refRepo,
            TenantRepository tenantRepo,
            UnitRepository unitRepo,
            SharingContractRepository sharingRepo,
            AuditService auditService) {
        this.refRepo = refRepo;
        this.tenantRepo = tenantRepo;
        this.unitRepo = unitRepo;
        this.sharingRepo = sharingRepo;
        this.auditService = auditService;
    }


    // ======= RÉFÉRENTIELS =======

    @Transactional(readOnly = true)
    public List<ReferenceData> getByCategory(String category) {
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null
                ? refRepo.findByCategory(category, tenantId)
                : refRepo.findByCategoryAndActiveTrueOrderBySortOrder(category);
    }

    @Transactional
    public ReferenceData createRef(ReferenceData ref) {
        if (refRepo.existsByCategoryAndCode(ref.getCategory(), ref.getCode())) {
            throw new ConflictException("Code déjà existant dans la catégorie : " + ref.getCode());
        }
        ref.setTenantId(TenantContext.getTenantId());
        ref.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        ReferenceData saved = refRepo.save(ref);
        auditService.log("REFERENCE_CREATED", "ReferenceData", saved.getId(), null,
                "category=" + ref.getCategory() + ", code=" + ref.getCode());
        return saved;
    }

    @Transactional
    public ReferenceData updateRef(Long id, ReferenceData patch) {
        ReferenceData existing = refRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceData", id));
        if (existing.isSystem()) throw new BadRequestException("Les entrées système ne sont pas modifiables.");
        if (patch.getLabel()       != null) existing.setLabel(patch.getLabel());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getExtraData()   != null) existing.setExtraData(patch.getExtraData());
        if (patch.getSortOrder()   != null) existing.setSortOrder(patch.getSortOrder());
        existing.setActive(patch.isActive());
        return refRepo.save(existing);
    }

    @Transactional
    public void deleteRef(Long id) {
        ReferenceData ref = refRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceData", id));
        if (ref.isSystem()) throw new BadRequestException("Les entrées système ne peuvent pas être supprimées.");
        refRepo.deleteById(id);
        auditService.log("REFERENCE_DELETED", "ReferenceData", id, null, null);
    }

    // ======= TENANTS =======

    @Transactional(readOnly = true)
    public Page<Tenant> listTenants(Pageable pageable) {
        return tenantRepo.findAll(pageable);
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepo.existsByCode(tenant.getCode())) {
            throw new ConflictException("Code tenant déjà utilisé : " + tenant.getCode());
        }
        Tenant saved = tenantRepo.save(tenant);
        auditService.log("TENANT_CREATED", "Tenant", saved.getId(), null, "code=" + saved.getCode());
        return saved;
    }

    @Transactional
    public Tenant updateTenant(Long id, Tenant patch) {
        Tenant tenant = tenantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        if (patch.getName()        != null) tenant.setName(patch.getName());
        if (patch.getDescription() != null) tenant.setDescription(patch.getDescription());
        if (patch.getIsolationMode() != null) tenant.setIsolationMode(patch.getIsolationMode());
        tenant.setActive(patch.isActive());
        return tenantRepo.save(tenant);
    }

    // ======= UNITÉS =======

    @Transactional(readOnly = true)
    public List<Unit> listUnits(Long tenantId) {
        return unitRepo.findByTenantId(tenantId);
    }

    @Transactional
    public Unit createUnit(Unit unit) {
        if (unitRepo.existsByCodeAndTenantId(unit.getCode(), unit.getTenant().getId())) {
            throw new ConflictException("Code unité déjà utilisé dans ce tenant.");
        }
        return unitRepo.save(unit);
    }
}
