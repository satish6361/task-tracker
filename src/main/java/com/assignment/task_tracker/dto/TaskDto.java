package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.Priority;
import com.assignment.task_tracker.entity.enums.TaskStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private Long assigneeId;
    private Long projectId;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
}
