package com.assignment.task_tracker.service;


import com.assignment.task_tracker.dto.CreateProjectDto;
import com.assignment.task_tracker.dto.ProjectDto;
import com.assignment.task_tracker.dto.UpdateProjectDto;
import com.assignment.task_tracker.entity.Organization;
import com.assignment.task_tracker.entity.Project;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.exception.ConflictException;
import com.assignment.task_tracker.exception.ForbiddenException;
import com.assignment.task_tracker.exception.ResourceNotFoundException;
import com.assignment.task_tracker.repository.OrganizationRepository;
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
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    private User getCurrentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateOrgMembership(User user, Long orgId) {
        if (user.getOrganization() == null) {
            throw new ForbiddenException("You don't belong to any organization");
        }
        if (!user.getOrganization().getId().equals(orgId)) {
            throw new ForbiddenException("You are not a member of this organization");
        }
    }

    @Override
    public ProjectDto createProject(Long orgId, CreateProjectDto dto) {
        log.info("Creating project");
        User currentUser = SecurityUtils.getCurrentUser();

        // Validate that current user belongs to the organization or not in which he/she is trying to create new project
        validateOrgMembership(currentUser, orgId);

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        if (projectRepository.existsByOrganizationIdAndName(orgId, dto.getName())) {
            throw new ConflictException("Project already exists with name: " + dto.getName());
        }

        Project project = modelMapper.map(dto, Project.class);
        project.setOrganization(organization);
        project.setCreatedBy(currentUser);

        Project saved = projectRepository.save(project);

        return modelMapper.map(saved, ProjectDto.class);
    }

    @Override
    public List<ProjectDto> getAllProjects(Long orgId) {
        log.info("Fetching all products in organization: {}", orgId);
        User currentUser = SecurityUtils.getCurrentUser();
        validateOrgMembership(currentUser, orgId);

        List<Project> projects = projectRepository.findByOrganizationId(orgId);

        return projects
                .stream()
                .map(project -> modelMapper.map(project, ProjectDto.class))
                .toList();
    }

    @Override
    public ProjectDto getProjectById(Long orgId, Long projectId) {
        log.info("Fetching project with id: {}", projectId);
        User currentUser = SecurityUtils.getCurrentUser();
        validateOrgMembership(currentUser, orgId);

        Project project = projectRepository.findByIdAndOrganizationId(projectId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        return modelMapper.map(project, ProjectDto.class);
    }

    @Override
    public ProjectDto updateProject(Long orgId, Long projectId, UpdateProjectDto dto) {
        log.info("Updating project with id: {}", projectId);
        User currentUser = SecurityUtils.getCurrentUser();
        validateOrgMembership(currentUser, orgId);

        Project project = projectRepository.findByIdAndOrganizationId(projectId, orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (dto.getName() != null) {
            project.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }

        if (dto.getIsArchived() != null) {
            project.setIsArchived(dto.getIsArchived());
        }

        return modelMapper.map(project, ProjectDto.class);
    }

    @Override
    public void deleteProject(Long orgId, Long projectId) {
        log.info("Deleting project with id: {}", projectId);
        User currentUser = SecurityUtils.getCurrentUser();
        validateOrgMembership(currentUser, orgId);

        Project project = projectRepository.findByIdAndOrganizationId(projectId, orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        projectRepository.delete(project);
        log.info("Successfully deleted project with id: {}", projectId);
    }
}
