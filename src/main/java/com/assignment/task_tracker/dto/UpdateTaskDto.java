package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.Priority;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Data
@Getter
@Setter
public class UpdateTaskDto {
    private String title;
    private String description;
    private Priority priority;
    private Long assigneeId;
    private LocalDate dueDate;
}
