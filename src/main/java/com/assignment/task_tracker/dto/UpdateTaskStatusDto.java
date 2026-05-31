package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UpdateTaskStatusDto {
    @NotNull
    TaskStatus status;
}
