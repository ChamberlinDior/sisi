package com.pnis.backend.workflow.service;

import com.pnis.backend.audit.service.AuditService;
import com.pnis.backend.collection.model.CollectedData;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.enums.RecordStatus;
import com.pnis.backend.common.exception.BadRequestException;
import com.pnis.backend.common.exception.ResourceNotFoundException;
import com.pnis.backend.common.util.SecurityUtils;
import com.pnis.backend.intelligence.model.IntelligenceCase;
import com.pnis.backend.notification.service.NotificationService;
import com.pnis.backend.workflow.model.WorkflowTask;
import com.pnis.backend.workflow.repository.WorkflowTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Moteur de workflow – §7.11.
 * Connexions obligatoires §8 :
 *   Dossiers → Workflow : chaque dossier déclenche tâches, validations et échéances
 *   Workflow → Notifications : chaque transition importante notifie les acteurs
 */
@Slf4j
@Service
public class WorkflowService {

    private final WorkflowTaskRepository taskRepo;
    private final AuditService           auditService;
    private final NotificationService    notifService;

    public WorkflowService(
            WorkflowTaskRepository taskRepo,
            AuditService auditService,
            NotificationService notifService) {
        this.taskRepo = taskRepo;
        this.auditService = auditService;
        this.notifService = notifService;
    }


    // =========================================================
    // CONNEXION §8 : Collecte → Workflow
    // =========================================================
    @Transactional
    public WorkflowTask onCollectionReceived(CollectedData data) {
        WorkflowTask task = buildTask(
                "CollectedData", data.getId(), data.getUuid(),
                "Vérification requise – " + data.getTitle(),
                "La donnée collectée doit être vérifiée avant exploitation.",
                "VERIFICATION", "HIGH",
                data.getCreatedBy(),   // assignée au créateur par défaut
                Instant.now().plus(48, ChronoUnit.HOURS),
                "RECEIVED→PENDING_VERIFICATION",
                data.getTenantId());

        WorkflowTask saved = taskRepo.save(task);

        // Mise à jour statut collecte
        data.setRecordStatus(RecordStatus.PENDING_VERIFICATION);

        auditService.log("WORKFLOW_TASK_CREATED", "WorkflowTask", saved.getId(), null,
                "for=CollectedData#" + data.getId() + ", transition=RECEIVED→PENDING_VERIFICATION");

        // §8 Workflow → Notifications
        if (data.getCreatedBy() != null) {
            notifService.sendWorkflowNotification(
                    data.getCreatedBy(), data.getTenantId(),
                    "CollectedData", data.getId(),
                    "RECEIVED → PENDING_VERIFICATION");
        }

        return saved;
    }

    @Transactional
    public void onCollectionValidated(CollectedData data) {
        closeTasksForObject("CollectedData", data.getId(), "VALIDATED", "Validation effectuée.");
        auditService.log("WORKFLOW_COLLECTION_VALIDATED", "CollectedData", data.getId(),
                data.getUuid(), null);
    }

    // =========================================================
    // CONNEXION §8 : Dossiers → Workflow
    // =========================================================
    @Transactional
    public WorkflowTask onCaseCreated(IntelligenceCase c) {
        WorkflowTask task = buildTask(
                "IntelligenceCase", c.getId(), c.getUuid(),
                "Dossier ouvert – " + c.getReferenceCode(),
                "Prise en charge du dossier par le chef désigné.",
                "VALIDATION", "MEDIUM",
                c.getLeadOfficer() != null ? c.getLeadOfficer().getUsername() : null,
                c.getDeadline() != null ? c.getDeadline() : Instant.now().plus(7, ChronoUnit.DAYS),
                "OPEN→IN_PROGRESS",
                c.getTenantId());

        WorkflowTask saved = taskRepo.save(task);

        auditService.log("WORKFLOW_TASK_CREATED", "WorkflowTask", saved.getId(), null,
                "for=IntelligenceCase#" + c.getId());

        String recipient = c.getLeadOfficer() != null ? c.getLeadOfficer().getUsername() : c.getCreatedBy();
        if (recipient != null) {
            notifService.sendWorkflowNotification(recipient, c.getTenantId(),
                    "IntelligenceCase", c.getId(), "OPEN→IN_PROGRESS");
        }
        return saved;
    }

    // =========================================================
    // COMPLÉTION D'UNE TÂCHE
    // =========================================================
    @Transactional
    public WorkflowTask completeTask(Long taskId, String note) {
        WorkflowTask task = getById(taskId);
        if ("COMPLETED".equals(task.getTaskStatus()) || "CANCELLED".equals(task.getTaskStatus())) {
            throw new BadRequestException("La tâche est déjà terminée.");
        }
        task.setTaskStatus("COMPLETED");
        task.setCompletedAt(Instant.now());
        task.setCompletionNote(note);
        WorkflowTask saved = taskRepo.save(task);

        auditService.log("WORKFLOW_TASK_COMPLETED", "WorkflowTask", taskId, null,
                "note=" + note);

        if (task.getAssigneeUsername() != null) {
            notifService.sendWorkflowNotification(task.getAssigneeUsername(),
                    task.getTenantId(), task.getObjectType(), task.getObjectId(), "TASK_COMPLETED");
        }
        return saved;
    }

    // =========================================================
    // REJET
    // =========================================================
    @Transactional
    public WorkflowTask rejectTask(Long taskId, String reason) {
        WorkflowTask task = getById(taskId);
        task.setTaskStatus("REJECTED");
        task.setCompletedAt(Instant.now());
        task.setCompletionNote(reason);
        WorkflowTask saved = taskRepo.save(task);

        auditService.log("WORKFLOW_TASK_REJECTED", "WorkflowTask", taskId, null, "reason=" + reason);
        return saved;
    }

    // =========================================================
    // ESCALADE
    // =========================================================
    @Transactional
    public WorkflowTask escalate(Long taskId, String newAssignee, String reason) {
        WorkflowTask original = getById(taskId);
        original.setTaskStatus("ESCALATED");
        original.setCompletionNote("Escaladé : " + reason);
        taskRepo.save(original);

        WorkflowTask escalated = buildTask(
                original.getObjectType(), original.getObjectId(), original.getObjectUuid(),
                "[ESCALADE] " + original.getTitle(),
                reason, "ESCALATION", "CRITICAL",
                newAssignee,
                Instant.now().plus(24, ChronoUnit.HOURS),
                "ESCALATED",
                original.getTenantId());
        escalated.setParentTaskId(taskId);
        escalated.setEscalationLevel(original.getEscalationLevel() + 1);
        WorkflowTask saved = taskRepo.save(escalated);

        auditService.log("WORKFLOW_TASK_ESCALATED", "WorkflowTask", taskId, null,
                "to=" + newAssignee + ", reason=" + reason);

        if (newAssignee != null) {
            notifService.send(newAssignee, original.getTenantId(),
                    "ESCALADE – " + original.getObjectType() + " #" + original.getObjectId(),
                    reason, "WORKFLOW", original.getObjectType(), original.getObjectId(), "CRITICAL");
        }
        return saved;
    }

    // =========================================================
    // RELANCES AUTOMATIQUES (Quartz / @Scheduled)
    // =========================================================
    @Scheduled(cron = "0 0 */4 * * *")   // toutes les 4h
    @Transactional
    public void sendReminders() {
        Instant now            = Instant.now();
        Instant threshold      = now.plus(24, ChronoUnit.HOURS);
        Instant reminderWindow = now.minus(6, ChronoUnit.HOURS);

        List<WorkflowTask> tasks = taskRepo.findTasksNeedingReminder(threshold, reminderWindow);
        for (WorkflowTask task : tasks) {
            if (task.getAssigneeUsername() != null) {
                notifService.send(task.getAssigneeUsername(), task.getTenantId(),
                        "Rappel tâche – échéance proche",
                        "La tâche '" + task.getTitle() + "' arrive à échéance.",
                        "WORKFLOW", task.getObjectType(), task.getObjectId(), "WARNING");
                task.setReminderCount(task.getReminderCount() + 1);
                task.setLastReminderAt(now);
                taskRepo.save(task);
            }
        }
        if (!tasks.isEmpty()) {
            log.info("[WORKFLOW] {} relance(s) envoyée(s)", tasks.size());
        }
    }

    // =========================================================
    // LECTURE
    // =========================================================
    @Transactional(readOnly = true)
    public WorkflowTask getById(Long id) {
        return taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTask", id));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowTask> getMyTasks(String username, List<String> statuses, Pageable pageable) {
        return taskRepo.findByAssigneeUsernameAndTaskStatusIn(username, statuses, pageable);
    }

    @Transactional(readOnly = true)
    public List<WorkflowTask> getObjectTasks(String objectType, Long objectId) {
        return taskRepo.findByObjectTypeAndObjectId(objectType, objectId);
    }

    @Transactional(readOnly = true)
    public List<WorkflowTask> getOverdueTasks(Long tenantId) {
        return taskRepo.findOverdueTasks(tenantId, Instant.now());
    }

    // =========================================================
    // PRIVATE
    // =========================================================
    private WorkflowTask buildTask(String objectType, Long objectId, String objectUuid,
                                    String title, String description, String taskType,
                                    String priority, String assignee, Instant dueDate,
                                    String transition, Long tenantId) {
        WorkflowTask task = new WorkflowTask();
        task.setTenantId(tenantId);
        task.setObjectType(objectType);
        task.setObjectId(objectId);
        task.setObjectUuid(objectUuid);
        task.setTitle(title);
        task.setDescription(description);
        task.setTaskType(taskType);
        task.setPriority(priority);
        task.setAssigneeUsername(assignee);
        task.setDueDate(dueDate);
        task.setTriggeredTransition(transition);
        task.setTaskStatus("PENDING");
        task.setCreatedBy(SecurityUtils.getCurrentUsernameOrSystem());
        return task;
    }

    private void closeTasksForObject(String objectType, Long objectId, String status, String note) {
        taskRepo.findByObjectTypeAndObjectId(objectType, objectId).forEach(t -> {
            if (!"COMPLETED".equals(t.getTaskStatus()) && !"CANCELLED".equals(t.getTaskStatus())) {
                t.setTaskStatus(status.equals("VALIDATED") ? "COMPLETED" : "CANCELLED");
                t.setCompletedAt(Instant.now());
                t.setCompletionNote(note);
                taskRepo.save(t);
            }
        });
    }
}
