package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskStatusChangedNotificationDto(
        Long taskId,
        Long projectId,
        Long assigneeId,
        String title,
        TaskStatus previousStatus,
        TaskStatus currentStatus,
        LocalDateTime changedAt
) {
}
