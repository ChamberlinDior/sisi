package com.pnis.backend.workflow.repository;

import com.pnis.backend.workflow.model.WorkflowTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {

    Page<WorkflowTask> findByAssigneeUsernameAndTaskStatusIn(
            String username, List<String> statuses, Pageable pageable);

    List<WorkflowTask> findByObjectTypeAndObjectId(String objectType, Long objectId);

    Page<WorkflowTask> findByTenantIdAndTaskStatus(Long tenantId, String status, Pageable pageable);

    @Query("SELECT t FROM WorkflowTask t WHERE t.tenantId = :tid " +
           "AND t.taskStatus IN ('PENDING','IN_PROGRESS') " +
           "AND t.dueDate < :now")
    List<WorkflowTask> findOverdueTasks(@Param("tid") Long tenantId, @Param("now") Instant now);

    @Query("SELECT t FROM WorkflowTask t WHERE t.taskStatus = 'PENDING' " +
           "AND t.dueDate < :threshold " +
           "AND (t.lastReminderAt IS NULL OR t.lastReminderAt < :reminderThreshold)")
    List<WorkflowTask> findTasksNeedingReminder(@Param("threshold")       Instant threshold,
                                                 @Param("reminderThreshold") Instant reminderThreshold);
}
