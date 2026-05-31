package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.Priority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Data
@Getter
@Setter
public class CreateTaskDto {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Priority priority;

    private Long assigneeId;

    @Future
    private LocalDate dueDate;
}
