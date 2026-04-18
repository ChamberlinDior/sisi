package com.pnis.backend.document.model;

import com.pnis.backend.common.model.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Pièce jointe / document – §7.10.
 * Statuts §9.4 : UPLOADED, SCANNED, HASHED, CLASSIFIED, AVAILABLE, SUPERSEDED, SEALED, DELETED_LOGICAL.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "document_resources", indexes = {
        @Index(name = "idx_doc_tenant",  columnList = "tenant_id"),
        @Index(name = "idx_doc_owner",   columnList = "owner_type,owner_id"),
        @Index(name = "idx_doc_status",  columnList = "record_status"),
        @Index(name = "idx_doc_hash",    columnList = "file_hash")
})
public class DocumentResource extends AbstractBaseEntity {

    @Column(name = "original_name", nullable = false, length = 300)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 300)
    private String storedName;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    /** SHA-256 du fichier */
    @Column(name = "file_hash", length = 64)
    private String fileHash;

    /** UPLOADED, SCANNED, HASHED, CLASSIFIED, AVAILABLE, SUPERSEDED, SEALED, DELETED_LOGICAL */
    @Column(name = "doc_status", nullable = false, length = 30)
    private String docStatus = "UPLOADED";

    /** Type de l'objet propriétaire (IntelligenceCase, EntityRecord, CollectedData, Alert…) */
    @Column(name = "owner_type", length = 80)
    private String ownerType;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    /** Numéro de version (pour le versionning §9.4 SUPERSEDED) */
    @Column(name = "version_number")
    private Integer versionNumber = 1;

    @Column(name = "previous_version_id")
    private Long previousVersionId;

    /** Texte du filigrane appliqué */
    @Column(name = "watermark_text", length = 200)
    private String watermarkText;

    /** Scellé (non modifiable) */
    @Column(name = "is_sealed")
    private Boolean isSealed = false;

    /** Résultat antivirus */
    @Column(name = "av_status", length = 30)
    private String avStatus = "PENDING";

    @Column(name = "av_scanned_at")
    private java.time.Instant avScannedAt;
}
