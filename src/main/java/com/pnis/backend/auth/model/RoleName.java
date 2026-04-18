package com.pnis.backend.auth.model;

/**
 * Rôles RBAC de la plateforme PNIS.
 * Complété par l'ABAC (tenant, classification, mission) dans les services.
 */
public enum RoleName {
    SUPER_ADMIN,        // Administrateur plateforme global
    ADMIN,              // Administrateur institutionnel (par tenant)
    ANALYST_SENIOR,     // Analyste senior avec accès étendu
    ANALYST,            // Analyste standard
    COLLECTOR,          // Agent de collecte terrain
    COORDINATOR,        // Coordinateur opérationnel
    CORRIDOR_USER,      // Accès au canal privé institutionnel
    PUBLICATION_EDITOR, // Éditeur de publications inter-services
    REPORT_VIEWER,      // Consultation des rapports uniquement
    AUDIT_REVIEWER,     // Accès aux journaux d'audit
    WORKFLOW_VALIDATOR, // Validation des étapes workflow
    GEO_OPERATOR,       // Opérateur cartographie / SIG
    CONNECTOR_ADMIN,    // Administration des connecteurs
    ACTUATOR_ADMIN,     // Accès aux endpoints Actuator
    READ_ONLY           // Consultation pure sans écriture
}
