package com.pnis.backend.collection.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Job d'import batch – §7.3 import CSV/Excel avec contrôle qualité.
 * Conforme §11 : support des imports asynchrones via jobs traçables.
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "import_jobs", indexes = {
        @Index(name = "idx_import_tenant", columnList = "tenant_id"),
        @Index(name = "idx_import_status", columnList = "status")
})
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    /** CSV, EXCEL, JSON */
    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(name = "target_type", length = 80)
    private String targetType;   // CollectedData, EntityRecord, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.QUEUED;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "processed_rows")
    private Integer processedRows = 0;

    @Column(name = "success_rows")
    private Integer successRows = 0;

    @Column(name = "error_rows")
    private Integer errorRows = 0;

    @Column(name = "error_details", length = 8000)
    private String errorDetails;

    @Column(name = "submitted_by", nullable = false, length = 100)
    private String submittedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }

    public enum JobStatus {
        QUEUED, PROCESSING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, CANCELLED
    }
}
