package com.assignment.task_tracker.repository;

import com.assignment.task_tracker.entity.User;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    List<User> findByOrganization_id(Long orgId);
    Optional<User> findByIdAndOrganization_id(Long userId, Long orgId);
}
