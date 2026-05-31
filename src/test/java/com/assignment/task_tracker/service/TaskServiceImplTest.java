package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.TaskDto;
import com.assignment.task_tracker.entity.Organization;
import com.assignment.task_tracker.entity.Project;
import com.assignment.task_tracker.entity.Task;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.entity.enums.Priority;
import com.assignment.task_tracker.entity.enums.Role;
import com.assignment.task_tracker.entity.enums.TaskStatus;
import com.assignment.task_tracker.event.TaskStatusChangedEvent;
import com.assignment.task_tracker.exception.ForbiddenException;
import com.assignment.task_tracker.exception.InvalidStatusTransitionException;
import com.assignment.task_tracker.repository.ProjectMemberRepository;
import com.assignment.task_tracker.repository.ProjectRepository;
import com.assignment.task_tracker.repository.TaskRepository;
import com.assignment.task_tracker.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TaskServiceImpl taskService;

    private Organization organization;
    private Project project;
    private User member;
    private User otherMember;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(
                taskRepository,
                projectRepository,
                userRepository,
                projectMemberRepository,
                new ModelMapper(),
                redisTemplate,
                new ObjectMapper(),
                eventPublisher
        );

        organization = new Organization();
        organization.setId(1L);

        project = new Project();
        project.setId(10L);
        project.setOrganization(organization);

        member = user(100L, Role.MEMBER);
        otherMember = user(200L, Role.MEMBER);
        authenticate(member);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assigneeCanAdvanceTaskStatusToNextAllowedState() {
        Task task = task(500L, member, TaskStatus.TODO);

        when(taskRepository.findByIdAndProjectIdAndIsDeletedFalse(500L, 10L))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(redisTemplate.keys("tasks:project:10:assignee:100:*")).thenReturn(Set.of());

        TaskDto result = taskService.updateStatus(10L, 500L, TaskStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository).save(task);
        verify(eventPublisher).publishEvent(any(TaskStatusChangedEvent.class));
    }

    @Test
    void invalidStatusTransitionIsRejected() {
        Task task = task(500L, member, TaskStatus.TODO);

        when(taskRepository.findByIdAndProjectIdAndIsDeletedFalse(500L, 10L))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(10L, 500L, TaskStatus.DONE))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Cannot transition task from TODO to DONE");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void completingTaskSetsCompletedAtForAnalytics() {
        Task task = task(500L, member, TaskStatus.IN_REVIEW);

        when(taskRepository.findByIdAndProjectIdAndIsDeletedFalse(500L, 10L))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(redisTemplate.keys("tasks:project:10:assignee:100:*")).thenReturn(Set.of());

        TaskDto result = taskService.updateStatus(10L, 500L, TaskStatus.DONE);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
        verify(taskRepository).save(task);
        verify(eventPublisher).publishEvent(any(TaskStatusChangedEvent.class));
    }

    @Test
    void memberCannotViewTaskAssignedToSomeoneElse() {
        Task task = task(500L, otherMember, TaskStatus.TODO);

        when(taskRepository.findByIdAndProjectIdAndIsDeletedFalse(500L, 10L))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getTaskById(10L, 500L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Members can only view tasks assigned to them");
    }

    @Test
    void memberCannotUpdateStatusForTaskAssignedToSomeoneElse() {
        Task task = task(500L, otherMember, TaskStatus.TODO);

        when(taskRepository.findByIdAndProjectIdAndIsDeletedFalse(500L, 10L))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(10L, 500L, TaskStatus.IN_PROGRESS))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only assignee or manager can update status");

        verify(taskRepository, never()).save(any(Task.class));
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user%s@example.com".formatted(id));
        user.setPassword("password");
        user.setOrganization(organization);
        user.setRoles(EnumSet.of(role));
        user.setIsActive(true);
        return user;
    }

    private Task task(Long id, User assignee, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Critical flow");
        task.setPriority(Priority.MEDIUM);
        task.setStatus(status);
        task.setProject(project);
        task.setAssignee(assignee);
        task.setCreatedBy(member);
        task.setIsDeleted(false);
        return task;
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }
}
