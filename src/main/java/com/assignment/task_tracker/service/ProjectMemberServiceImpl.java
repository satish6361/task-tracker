package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.AddProjectMemberDto;
import com.assignment.task_tracker.dto.ProjectMemberDto;
import com.assignment.task_tracker.entity.Project;
import com.assignment.task_tracker.entity.ProjectMember;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.exception.BadRequestException;
import com.assignment.task_tracker.exception.ConflictException;
import com.assignment.task_tracker.exception.ForbiddenException;
import com.assignment.task_tracker.exception.ResourceNotFoundException;
import com.assignment.task_tracker.repository.ProjectMemberRepository;
import com.assignment.task_tracker.repository.ProjectRepository;
import com.assignment.task_tracker.repository.UserRepository;
import com.assignment.task_tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMemberServiceImpl implements ProjectMemberService {
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ModelMapper modelMapper;

    private User currentUser() {
        return SecurityUtils.getCurrentUser();
    }

    private Project getProject(Long orgId, Long projectId) {
        return projectRepository.findByIdAndOrganizationId(projectId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    @Override
    public ProjectMemberDto addMember(Long orgId, Long projectId, AddProjectMemberDto addProjectMemberDto) {
        Long memberId = addProjectMemberDto.getUserId();
        log.info("Adding member with id: " + memberId + " project with id: " + projectId + " in organization: " + orgId);

        User currentUser = currentUser();

        if (!currentUser.getOrganization().getId().equals(orgId)) {
            throw new ForbiddenException("You are not a member of this organization");
        }

        Project project = getProject(orgId, projectId);

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

        if (!member.getOrganization().getId().equals(orgId)) {
            throw new BadRequestException("User does not belong to this organization");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, memberId)) {
            throw new ConflictException("User already added to project");
        }

        ProjectMember pm = new ProjectMember();

        pm.setProject(project);
        pm.setUser(member);

        ProjectMember saved = projectMemberRepository.save(pm);

        return new ProjectMemberDto(member.getId(), member.getName(), member.getEmail(), saved.getJoinedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberDto> getMembers(Long orgId, Long projectId) {
        log.info("Fetching all members of project id: {} in organization id: {}", projectId, orgId);

        User currentUser = currentUser();

        if (!currentUser.getOrganization().getId().equals(orgId)) {
            throw new ForbiddenException("You are not a member of this organization");
        }

        getProject(orgId, projectId);

        return projectMemberRepository
                .findByProjectId(projectId)
                .stream()
                .map(pm ->
                        new ProjectMemberDto(
                                pm.getUser().getId(),
                                pm.getUser().getName(),
                                pm.getUser().getEmail(),
                                pm.getJoinedAt()
                        )
                )
                .toList();
    }

    @Override
    public void removeMember(Long orgId, Long projectId, Long userId) {
        log.info("Deleting member: {} from project id: {} in organization id: {}", userId, projectId, orgId);
        User currentUser = currentUser();

        if (!currentUser.getOrganization().getId().equals(orgId)) {
            throw new ForbiddenException(
                    "You are not a member of this organization"
            );
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project member not found with id: " + userId));

        projectMemberRepository.delete(member);
    }
}
