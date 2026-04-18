package com.pnis.backend.intelligence.model;

import com.pnis.backend.auth.model.AppUser;
import com.pnis.backend.common.enums.ClassificationLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Note chronologique d'enquête liée à un dossier §7.5 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "case_notes", indexes = {
        @Index(name = "idx_note_case",    columnList = "case_id"),
        @Index(name = "idx_note_created", columnList = "created_at")
})
public class CaseNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private IntelligenceCase intelligenceCase;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    /** SITUATION_UPDATE, LEAD, ANALYSIS, DECISION, FIELD_REPORT, MEETING_MINUTES */
    @Column(name = "note_type", length = 50)
    private String noteType;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 20)
    private ClassificationLevel classification = ClassificationLevel.RESTRICTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist  void prePersist() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   void preUpdate()  { updatedAt = Instant.now(); }
}
