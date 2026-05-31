package com.assignment.task_tracker.controller;

import com.assignment.task_tracker.dto.AddProjectMemberDto;
import com.assignment.task_tracker.dto.ProjectMemberDto;
import com.assignment.task_tracker.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations/{orgId}/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    @PostMapping
    public ResponseEntity<ProjectMemberDto> addMember(@PathVariable Long orgId, @PathVariable Long projectId, @Valid @RequestBody AddProjectMemberDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectMemberService.addMember(orgId, projectId, dto));
    }

    @GetMapping
    public ResponseEntity<List<ProjectMemberDto>> getMembers(@PathVariable Long orgId, @PathVariable Long projectId) {
        return ResponseEntity.ok(projectMemberService.getMembers(orgId, projectId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long orgId, @PathVariable Long projectId, @PathVariable Long userId) {
        projectMemberService.removeMember(orgId, projectId, userId);

        return ResponseEntity.noContent().build();
    }
}
