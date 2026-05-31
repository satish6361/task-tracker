package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.*;
import com.assignment.task_tracker.entity.Organization;
import org.springframework.stereotype.Service;

@Service
public interface OrganizationService {
    Organization createOrganization(CreateOrganizationDto dto);
    Organization getById(Long orgId);
    UserDto addMember(Long orgId, AddOrganizationMemberDto addOrganizationMemberDto);
}