package com.assignment.task_tracker.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ProjectMemberDto {
    private Long user;
    private String name;
    private String email;
    private LocalDateTime joinedAt;
}
