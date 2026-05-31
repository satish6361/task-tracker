package com.assignment.task_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CreateProjectDto{
        @NotBlank(message = "Project name is required")
        @Size(max = 100)
        private String name;

        @Size(max = 1000)
        private String description;
}
