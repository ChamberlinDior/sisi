package com.pnis.backend.workflow.controller;

import com.pnis.backend.common.api.ApiResponse;
import com.pnis.backend.common.config.TenantContext;
import com.pnis.backend.common.util.PageUtils;
import com.pnis.backend.workflow.model.WorkflowTask;
import com.pnis.backend.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
@Tag(name = "Workflow & Tâches", description = "Tâches, validations, escalades §7.11")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(
            WorkflowService workflowService) {
        this.workflowService = workflowService;
    }


    @GetMapping("/my-tasks")
    @Operation(summary = "Mes tâches en cours")
    public ResponseEntity<ApiResponse<List<WorkflowTask>>> myTasks(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "PENDING,IN_PROGRESS") String statuses,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        List<String> statusList = List.of(statuses.split(","));
        Pageable p = PageRequest.of(page, size, Sort.by("dueDate").ascending());
        return ResponseEntity.ok(PageUtils.toPagedResponse(
                workflowService.getMyTasks(user.getUsername(), statusList, p)));
    }

    @GetMapping("/{objectType}/{objectId}/tasks")
    @Operation(summary = "Tâches liées à un objet")
    public ResponseEntity<ApiResponse<List<WorkflowTask>>> objectTasks(
            @PathVariable String objectType, @PathVariable Long objectId) {
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.getObjectTasks(objectType, objectId)));
    }

    @PatchMapping("/tasks/{id}/complete")
    @PreAuthorize("hasAnyRole('ANALYST','ANALYST_SENIOR','COORDINATOR','WORKFLOW_VALIDATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Compléter une tâche")
    public ResponseEntity<ApiResponse<WorkflowTask>> complete(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(ApiResponse.ok(workflowService.completeTask(id, note)));
    }

    @PatchMapping("/tasks/{id}/reject")
    @PreAuthorize("hasAnyRole('WORKFLOW_VALIDATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Rejeter une tâche (avec motif)")
    public ResponseEntity<ApiResponse<WorkflowTask>> reject(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.rejectTask(id, body.get("reason"))));
    }

    @PostMapping("/tasks/{id}/escalate")
    @PreAuthorize("hasAnyRole('ANALYST_SENIOR','COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Escalader une tâche vers un responsable supérieur")
    public ResponseEntity<ApiResponse<WorkflowTask>> escalate(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.escalate(id, body.get("newAssignee"), body.get("reason"))));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Tâches en retard (tableau de bord superviseur)")
    public ResponseEntity<ApiResponse<List<WorkflowTask>>> overdue() {
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.getOverdueTasks(TenantContext.getTenantId())));
    }
}
