package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.UserDto;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.entity.enums.Role;
import com.assignment.task_tracker.exception.ResourceNotFoundException;
import com.assignment.task_tracker.repository.UserRepository;
import com.assignment.task_tracker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        ));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + username
                        ));
    }

    public UserDto updateRoles(Long id, Set<Role> newRoles) {
        log.info("Updating roles for the user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setRoles(newRoles);
        user = userRepository.save(user);

        log.info("Roles updated successfully!");

        return modelMapper.map(user, UserDto.class);
    }

    public UserDto getMyDetails() {
        User user = SecurityUtils.getCurrentUser();

        if (user == null) {
            throw new IllegalStateException("User not found");
        }

        return modelMapper.map(user, UserDto.class);
    }
}
