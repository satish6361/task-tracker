package com.assignment.task_tracker.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse{
    private String error;
    private String message;
}