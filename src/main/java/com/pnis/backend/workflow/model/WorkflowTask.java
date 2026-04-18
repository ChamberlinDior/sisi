package com.pnis.backend.workflow.model;

import com.pnis.backend.auth.model.AppUser;
import com.pnis.backend.common.model.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tâche de workflow – §7.11 : orchestration de la vie des objets.
 * Connexion obligatoire §8 : Workflow → Notifications (chaque transition notifie).
 */
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_tasks", indexes = {
        @Index(name = "idx_task_tenant",      columnList = "tenant_id"),
        @Index(name = "idx_task_assignee",    columnList = "assignee_id"),
        @Index(name = "idx_task_object",      columnList = "object_type,object_id"),
        @Index(name = "idx_task_status",      columnList = "task_status"),
        @Index(name = "idx_task_due",         columnList = "due_date")
})
public class WorkflowTask extends AbstractBaseEntity {

    /** Type de l'objet parent : CollectedData, IntelligenceCase, Alert, etc. */
    @Column(name = "object_type", nullable = false, length = 80)
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "object_uuid", length = 36)
    private String objectUuid;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 4000)
    private String description;

    /** VERIFICATION, VALIDATION, ANALYSIS, APPROVAL, CLOSURE, ESCALATION, FIELD_ACTION */
    @Column(name = "task_type", length = 50)
    private String taskType;

    /** PENDING, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED, ESCALATED */
    @Column(name = "task_status", nullable = false, length = 30)
    private String taskStatus = "PENDING";

    /** LOW, MEDIUM, HIGH, CRITICAL */
    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @Column(name = "assignee_username", length = 100)
    private String assigneeUsername;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Transition déclenchée (ex: RECEIVED→PENDING_VERIFICATION) */
    @Column(name = "triggered_transition", length = 100)
    private String triggeredTransition;

    /** Commentaire de complétion ou de rejet */
    @Column(name = "completion_note", length = 2000)
    private String completionNote;

    /** Nombre de relances envoyées */
    @Column(name = "reminder_count")
    private Integer reminderCount = 0;

    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;

    /** Tâche parente (escalade) */
    @Column(name = "parent_task_id")
    private Long parentTaskId;

    /** Nombre d'escalades */
    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;
}
