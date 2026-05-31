package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.*;
import com.assignment.task_tracker.entity.Organization;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.entity.enums.Role;
import com.assignment.task_tracker.exception.ConflictException;
import com.assignment.task_tracker.exception.ForbiddenException;
import com.assignment.task_tracker.exception.ResourceNotFoundException;
import com.assignment.task_tracker.repository.OrganizationRepository;
import com.assignment.task_tracker.repository.UserRepository;
import com.assignment.task_tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrganizationServiceImp implements OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    @Override
//    @Transactional
    public Organization createOrganization(CreateOrganizationDto createOrganizationDto) {
        log.info("Creating a new organization: {}", createOrganizationDto.getName());

        // Get the current user
        User user = SecurityUtils.getCurrentUser();

        if (user.getOrganization() != null) {
            throw new ConflictException(
                    "User already belongs to an organization"
            );
        }

        // If user doesn't belong to any other organization then, create the organization and
        // set the current user as Admin for this organization.
        Organization org = new Organization();
        org.setName(createOrganizationDto.getName());
        Organization createdOrg = organizationRepository.save(org);

        user.setOrganization(createdOrg);
        user.setRoles(EnumSet.of(Role.ADMIN));
        userRepository.save(user);

        log.info("'{}' organization created successfully and user: {} assigned as admin for this organization",
                createdOrg.getName(),
                user.getId());
        return createdOrg;
//        return modelMapper.map(createdOrg, OrganizationDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getById(Long orgId) {
        log.info("Fetching organization with id: {}", orgId);
        User currentUser = SecurityUtils.getCurrentUser();

        Organization organization = organizationRepository.findById(orgId).orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        if (!currentUser.getOrganization().getId().equals(orgId)) {
            throw new ForbiddenException("You are not a member of this organization");
        }

        return organization;
//        return modelMapper.map(currentUser.getOrganization(), OrganizationDto.class);
    }

    @Override
    @Transactional
    public UserDto addMember(Long orgId, AddOrganizationMemberDto addOrganizationMemberDto) {
        User currentUser = SecurityUtils.getCurrentUser();

        if (!currentUser.getOrganization().getId().equals(orgId)) {
            throw new ForbiddenException("You are not a member of this organization " + orgId);
        }

        Organization organization = organizationRepository.findById(orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        User user = userRepository.findById(addOrganizationMemberDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOrganization() != null) {
            throw new ConflictException("User already belongs to an organization");
        }

        user.setOrganization(organization);

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(EnumSet.of(Role.MEMBER));
        }

        User savedUser = userRepository.save(user);

        return modelMapper.map(savedUser, UserDto.class);
    }

}
