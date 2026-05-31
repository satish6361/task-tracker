package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.CreateProjectDto;
import com.assignment.task_tracker.dto.ProjectDto;
import com.assignment.task_tracker.dto.UpdateProjectDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProjectService {

    ProjectDto createProject(Long orgId, CreateProjectDto dto);

    List<ProjectDto> getAllProjects(Long orgId);

    ProjectDto getProjectById(Long orgId, Long projectId);

    ProjectDto updateProject(
            Long orgId,
            Long projectId,
            UpdateProjectDto dto
    );

    void deleteProject(
            Long orgId,
            Long projectId
    );
}
