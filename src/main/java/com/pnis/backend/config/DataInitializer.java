package com.pnis.backend.config;

import com.pnis.backend.admin.model.ReferenceData;
import com.pnis.backend.admin.repository.ReferenceDataRepository;
import com.pnis.backend.auth.model.AppRole;
import com.pnis.backend.auth.model.AppUser;
import com.pnis.backend.auth.model.RoleName;
import com.pnis.backend.auth.repository.AppRoleRepository;
import com.pnis.backend.auth.repository.AppUserRepository;
import com.pnis.backend.common.config.AppProperties;
import com.pnis.backend.common.enums.ClassificationLevel;
import com.pnis.backend.tenant.model.Tenant;
import com.pnis.backend.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final AppRoleRepository      roleRepo;
    private final AppUserRepository      userRepo;
    private final TenantRepository       tenantRepo;
    private final ReferenceDataRepository refRepo;
    private final PasswordEncoder        passwordEncoder;
    private final AppProperties          appProperties;

    public DataInitializer(
            AppRoleRepository roleRepo,
            AppUserRepository userRepo,
            TenantRepository tenantRepo,
            ReferenceDataRepository refRepo,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.refRepo = refRepo;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }


    @Override
    @Transactional
    public void run(String... args) {
        log.info("[INIT] Démarrage de l'initialisation des données…");
        initRoles();
        initSystemTenant();
        initSuperAdmin();
        initReferenceData();
        log.info("[INIT] Initialisation terminée.");
    }

    // =========================================================
    private void initRoles() {
        for (RoleName name : RoleName.values()) {
            if (!roleRepo.existsByName(name)) {
                AppRole role = new AppRole();
                role.setName(name);
                role.setDescription(roleDescription(name));
                roleRepo.save(role);
                log.info("[INIT] Rôle créé : {}", name);
            }
        }
    }

    private void initSystemTenant() {
        if (!tenantRepo.existsByCode("SYSTEM")) {
            Tenant t = new Tenant();
            t.setCode("SYSTEM");
            t.setName("Plateforme PNIS – Système");
            t.setDescription("Tenant système – usage interne plateforme");
            t.setIsolationMode("PHYSICAL");
            tenantRepo.save(t);
            log.info("[INIT] Tenant SYSTEM créé.");
        }
        if (!tenantRepo.existsByCode("DGSS")) {
            Tenant t = new Tenant();
            t.setCode("DGSS");
            t.setName("Direction Générale de la Surveillance du Territoire");
            t.setIsolationMode("LOGICAL");
            tenantRepo.save(t);
            log.info("[INIT] Tenant DGSS créé.");
        }
        if (!tenantRepo.existsByCode("DGR")) {
            Tenant t = new Tenant();
            t.setCode("DGR");
            t.setName("Direction Générale des Renseignements");
            t.setIsolationMode("LOGICAL");
            tenantRepo.save(t);
            log.info("[INIT] Tenant DGR créé.");
        }
    }

    private void initSuperAdmin() {
        String adminEmail = appProperties.getAdmin().getDefaultSuperAdminEmail();
        if (!userRepo.existsByEmail(adminEmail)) {
            Tenant systemTenant = tenantRepo.findByCode("SYSTEM").orElseThrow();
            AppRole superAdminRole = roleRepo.findByName(RoleName.SUPER_ADMIN).orElseThrow();

            AppUser admin = new AppUser();
            admin.setUsername("superadmin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(appProperties.getAdmin().getDefaultSuperAdminPassword()));
            admin.setFullName("Super Administrateur PNIS");
            admin.setTenant(systemTenant);
            admin.setRoles(Set.of(superAdminRole));
            admin.setMaxClassification(ClassificationLevel.TOP_SECRET);
            admin.setEnabled(true);
            admin.setMustChangePassword(true);
            userRepo.save(admin);
            log.info("[INIT] Super-administrateur créé : {}", adminEmail);
        }
    }

    private void initReferenceData() {
        // Niveaux de classification
        saveRef(null, "CLASSIFICATION_LEVEL", "PUBLIC",       "Public",       0);
        saveRef(null, "CLASSIFICATION_LEVEL", "INTERNAL",     "Interne",      1);
        saveRef(null, "CLASSIFICATION_LEVEL", "RESTRICTED",   "Restreint",    2);
        saveRef(null, "CLASSIFICATION_LEVEL", "CONFIDENTIAL", "Confidentiel", 3);
        saveRef(null, "CLASSIFICATION_LEVEL", "SECRET",       "Secret",       4);
        saveRef(null, "CLASSIFICATION_LEVEL", "TOP_SECRET",   "Très Secret",  5);

        // Types d'entités
        saveRef(null, "ENTITY_TYPE", "PERSON",       "Personne physique",  0);
        saveRef(null, "ENTITY_TYPE", "ORGANIZATION", "Organisation",        1);
        saveRef(null, "ENTITY_TYPE", "VEHICLE",      "Véhicule",            2);
        saveRef(null, "ENTITY_TYPE", "LOCATION",     "Lieu / Site",         3);
        saveRef(null, "ENTITY_TYPE", "PHONE",        "Numéro de téléphone", 4);
        saveRef(null, "ENTITY_TYPE", "ACCOUNT",      "Compte / Identifiant",5);
        saveRef(null, "ENTITY_TYPE", "EVENT",        "Événement",           6);
        saveRef(null, "ENTITY_TYPE", "DOCUMENT",     "Document",            7);
        saveRef(null, "ENTITY_TYPE", "INCIDENT",     "Incident",            8);

        // Types de dossiers
        saveRef(null, "CASE_TYPE", "INVESTIGATION", "Investigation",   0);
        saveRef(null, "CASE_TYPE", "OPERATION",     "Opération",       1);
        saveRef(null, "CASE_TYPE", "MISSION",       "Mission",         2);
        saveRef(null, "CASE_TYPE", "SURVEILLANCE",  "Surveillance",    3);
        saveRef(null, "CASE_TYPE", "ANALYSIS",      "Analyse",         4);

        // Niveaux de priorité
        saveRef(null, "PRIORITY_LEVEL", "LOW",      "Faible",   0);
        saveRef(null, "PRIORITY_LEVEL", "MEDIUM",   "Moyen",    1);
        saveRef(null, "PRIORITY_LEVEL", "HIGH",     "Élevé",    2);
        saveRef(null, "PRIORITY_LEVEL", "CRITICAL", "Critique", 3);
        saveRef(null, "PRIORITY_LEVEL", "URGENT",   "Urgent",   4);

        // Catégories d'alertes
        saveRef(null, "ALERT_CATEGORY", "SECURITY",     "Sécurité",         0);
        saveRef(null, "ALERT_CATEGORY", "PUBLIC_ORDER", "Ordre public",     1);
        saveRef(null, "ALERT_CATEGORY", "TERRORISM",    "Terrorisme",       2);
        saveRef(null, "ALERT_CATEGORY", "CYBER",        "Cyber",            3);
        saveRef(null, "ALERT_CATEGORY", "BORDER",       "Frontières",       4);
        saveRef(null, "ALERT_CATEGORY", "ECONOMY",      "Criminalité éco.", 5);

        // Types de liens entre entités
        saveRef(null, "LINK_TYPE", "ASSOCIATED_WITH",    "Associé à",           0);
        saveRef(null, "LINK_TYPE", "MEMBER_OF",          "Membre de",           1);
        saveRef(null, "LINK_TYPE", "OWNS",               "Détient / Possède",   2);
        saveRef(null, "LINK_TYPE", "COMMUNICATES_WITH",  "Communique avec",     3);
        saveRef(null, "LINK_TYPE", "LOCATED_AT",         "Localisé à",          4);
        saveRef(null, "LINK_TYPE", "FUNDS",              "Finance",             5);
        saveRef(null, "LINK_TYPE", "OPERATES",           "Opère",               6);
        saveRef(null, "LINK_TYPE", "REPORTS_TO",         "Rend compte à",       7);

        // Canaux de collecte
        saveRef(null, "SOURCE_CHANNEL", "MANUAL",      "Saisie manuelle", 0);
        saveRef(null, "SOURCE_CHANNEL", "MOBILE",      "Terrain mobile",  1);
        saveRef(null, "SOURCE_CHANNEL", "CSV_IMPORT",  "Import CSV",      2);
        saveRef(null, "SOURCE_CHANNEL", "API",         "API externe",     3);
        saveRef(null, "SOURCE_CHANNEL", "OSINT",       "OSINT",           4);
        saveRef(null, "SOURCE_CHANNEL", "FORM",        "Formulaire web",  5);

        log.info("[INIT] Données de référence système initialisées.");
    }

    private void saveRef(Long tenantId, String category, String code, String label, int order) {
        if (!refRepo.existsByCategoryAndCode(category, code)) {
            ReferenceData r = new ReferenceData();
            r.setTenantId(tenantId);
            r.setCategory(category);
            r.setCode(code);
            r.setLabel(label);
            r.setSortOrder(order);
            r.setSystem(true);  // appelle setSystem(boolean) → systemEntry
            r.setActive(true);
            r.setCreatedBy("SYSTEM");
            refRepo.save(r);
        }
    }

    private String roleDescription(RoleName name) {
        return switch (name) {
            case SUPER_ADMIN        -> "Administrateur global de la plateforme PNIS";
            case ADMIN              -> "Administrateur institutionnel (tenant)";
            case ANALYST_SENIOR     -> "Analyste senior – accès étendu et validation";
            case ANALYST            -> "Analyste standard";
            case COLLECTOR          -> "Agent de collecte terrain";
            case COORDINATOR        -> "Coordinateur opérationnel";
            case CORRIDOR_USER      -> "Accès au canal privé institutionnel";
            case PUBLICATION_EDITOR -> "Éditeur de publications inter-services";
            case REPORT_VIEWER      -> "Consultation des rapports uniquement";
            case AUDIT_REVIEWER     -> "Accès aux journaux d'audit";
            case WORKFLOW_VALIDATOR -> "Validation des étapes workflow";
            case GEO_OPERATOR       -> "Opérateur cartographie / SIG";
            case CONNECTOR_ADMIN    -> "Administration des connecteurs externes";
            case ACTUATOR_ADMIN     -> "Accès aux endpoints Actuator/Prometheus";
            case READ_ONLY          -> "Consultation pure sans écriture";
        };
    }
}
