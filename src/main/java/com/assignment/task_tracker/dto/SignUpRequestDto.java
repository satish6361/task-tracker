package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.Role;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class SignUpRequestDto {
    private String name;
    private String email;
    private String password;
}
