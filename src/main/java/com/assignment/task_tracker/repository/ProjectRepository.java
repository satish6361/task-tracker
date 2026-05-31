package com.assignment.task_tracker.repository;

import com.assignment.task_tracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOrganizationId(Long organizationId);

    Optional<Project> findByIdAndOrganizationId(Long projectId, Long organizationId);

    boolean existsByOrganizationIdAndName(
            Long organizationId,
            String name
    );
}
