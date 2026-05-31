package com.assignment.task_tracker.dto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class ProjectDto{
        private Long id;
        private String name;
        private String description;
        private Boolean isArchived;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
}
