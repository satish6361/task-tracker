package com.assignment.task_tracker.dto;

import com.assignment.task_tracker.entity.enums.Gender;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UserDto {
    private Long id;
    private String name;
    private Gender gender;
    private String email;
}
