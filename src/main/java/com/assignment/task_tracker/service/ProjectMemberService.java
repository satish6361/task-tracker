package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.AddProjectMemberDto;
import com.assignment.task_tracker.dto.ProjectMemberDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProjectMemberService {
    ProjectMemberDto addMember(Long orgId, Long projectId, AddProjectMemberDto addProjectMemberDto);

    List<ProjectMemberDto> getMembers(Long orgId, Long projectId);

    void removeMember(Long orgId, Long projectId, Long userId);
}
