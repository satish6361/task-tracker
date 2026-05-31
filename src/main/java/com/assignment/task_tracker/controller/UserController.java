package com.assignment.task_tracker.controller;

import com.assignment.task_tracker.dto.TaskDto;
import com.assignment.task_tracker.dto.UpdateRoleDto;
import com.assignment.task_tracker.dto.UserDto;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.service.TaskService;
import com.assignment.task_tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final TaskService taskService;

    @PatchMapping("/{id}/roles")
    public ResponseEntity<UserDto> updateRoles(@PathVariable Long id, @RequestBody UpdateRoleDto dto) {
        return ResponseEntity.ok(userService.updateRoles(id, dto.getRoles()));
    }

    @GetMapping("/me/tasks")
    public ResponseEntity<List<TaskDto>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTasks());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyDetails() {
        return ResponseEntity.ok(userService.getMyDetails());
    }

}
