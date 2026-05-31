package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.CreateTaskDto;
import com.assignment.task_tracker.dto.TaskAnalyticsDto;
import com.assignment.task_tracker.dto.TaskDto;
import com.assignment.task_tracker.dto.UpdateTaskDto;
import com.assignment.task_tracker.entity.enums.Priority;
import com.assignment.task_tracker.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TaskService {
    TaskDto createTask(Long projectId, CreateTaskDto dto);
    TaskDto getTaskById(Long projectId, Long taskId);

    Page<TaskDto> getAllTasks(Long projectId, int page, int limit, TaskStatus status, Priority priority, Long assigneeId);
    TaskDto updateTask(Long projectId, Long taskId, UpdateTaskDto dto);
    TaskDto updateStatus(Long projectId, Long taskId, TaskStatus target);
    void deleteTask(Long projectId, Long taskId);

    List<TaskDto> getMyTasks();

    List<TaskAnalyticsDto> getTaskAnalytics(Long projectId);
}
