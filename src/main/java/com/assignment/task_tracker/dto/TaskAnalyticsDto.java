package com.assignment.task_tracker.dto;

public record TaskAnalyticsDto(
        Long userId,
        String name,
        String email,
        Long overdueTaskCount,
        Double avgCompletionSeconds
) {
}
