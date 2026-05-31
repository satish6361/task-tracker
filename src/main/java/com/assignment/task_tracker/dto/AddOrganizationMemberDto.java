package com.assignment.task_tracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AddOrganizationMemberDto {
    @NotNull(message = "User id is required")
    private Long userId;
}
