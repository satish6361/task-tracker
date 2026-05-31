package com.assignment.task_tracker.controller;

import com.assignment.task_tracker.dto.*;
import com.assignment.task_tracker.entity.Organization;
import com.assignment.task_tracker.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<Organization> createOrganization(
            @Valid @RequestBody CreateOrganizationDto dto) {

        return new ResponseEntity<>(organizationService.createOrganization(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<Organization> getById(@PathVariable Long orgId) {
        return ResponseEntity.ok(
                organizationService.getById(orgId)
        );
    }

    @PostMapping("/{orgId}/members")
    public ResponseEntity<UserDto> addMember(@PathVariable Long orgId, @Valid @RequestBody AddOrganizationMemberDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.addMember(orgId, dto));
    }

}
