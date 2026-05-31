package com.assignment.task_tracker.controller;

import com.assignment.task_tracker.dto.CreateTaskDto;
import com.assignment.task_tracker.dto.TaskAnalyticsDto;
import com.assignment.task_tracker.dto.TaskDto;
import com.assignment.task_tracker.dto.UpdateTaskDto;
import com.assignment.task_tracker.dto.UpdateTaskStatusDto;
import com.assignment.task_tracker.entity.enums.Priority;
import com.assignment.task_tracker.entity.enums.TaskStatus;
import com.assignment.task_tracker.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/tasks")
public class TaskController {
    private final TaskService taskService;
    @PostMapping
    public ResponseEntity<TaskDto> createTask(@PathVariable Long projectId, @Valid @RequestBody CreateTaskDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(projectId, dto));
    }

    @GetMapping
    public ResponseEntity<Page<TaskDto>> getTasks(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId
    ) {
        return ResponseEntity.ok(taskService.getAllTasks(projectId, page, limit, status, priority, assigneeId));
    }

    @GetMapping("/analytics")
    public ResponseEntity<List<TaskAnalyticsDto>> getTaskAnalytics(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTaskAnalytics(projectId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTask(@PathVariable Long projectId, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskById(projectId, taskId));
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long projectId, @PathVariable Long taskId, @RequestBody UpdateTaskDto dto) {
        return ResponseEntity.ok(taskService.updateTask(projectId, taskId, dto));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskDto> updateStatus(@PathVariable Long projectId, @PathVariable Long taskId, @Valid @RequestBody UpdateTaskStatusDto dto) {
        return ResponseEntity.ok(taskService.updateStatus(projectId, taskId, dto.getStatus()));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long projectId, @PathVariable Long taskId) {
        taskService.deleteTask(projectId, taskId);

        return ResponseEntity.noContent().build();
    }
}
