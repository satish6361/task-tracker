package com.assignment.task_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CreateOrganizationDto{
        @NotBlank(message = "Organization name is required")
        @Size(max = 100, message = "Organization name cannot exceed 100 characters")
        @NotBlank
        private String name;
}
