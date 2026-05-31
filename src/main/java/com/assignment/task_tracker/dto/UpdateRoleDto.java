package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.Role;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Data
@Getter
@Setter
public class UpdateRoleDto {
    private Set<Role> roles;
}
