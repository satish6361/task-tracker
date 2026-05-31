package com.assignment.task_tracker.controller;

import com.assignment.task_tracker.dto.CreateProjectDto;
import com.assignment.task_tracker.dto.ProjectDto;
import com.assignment.task_tracker.dto.UpdateProjectDto;
import com.assignment.task_tracker.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations/{orgId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@PathVariable Long orgId, @Valid @RequestBody CreateProjectDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(orgId,dto));
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(@PathVariable Long orgId) {
        return ResponseEntity.ok(projectService.getAllProjects(orgId));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable Long orgId, @PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectById(orgId,projectId));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @RequestBody UpdateProjectDto dto) {

        return ResponseEntity.ok(
                projectService.updateProject(
                        orgId,
                        projectId,
                        dto
                )
        );
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long orgId,
            @PathVariable Long projectId) {

        projectService.deleteProject(
                orgId,
                projectId
        );

        return ResponseEntity.noContent().build();
    }
}