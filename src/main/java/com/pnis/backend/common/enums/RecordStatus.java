package com.pnis.backend.common.enums;

/** Statut générique appliqué à la collecte / données entrantes (cf. CDC §9.1) */
public enum RecordStatus {
    DRAFT,
    RECEIVED,
    PENDING_VERIFICATION,
    REJECTED,
    VALIDATED,
    ENRICHED,
    CLASSIFIED,
    ARCHIVED,
    DELETED_LOGICAL
}
