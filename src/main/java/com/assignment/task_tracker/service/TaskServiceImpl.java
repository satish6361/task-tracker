package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.CreateTaskDto;
import com.assignment.task_tracker.dto.TaskAnalyticsDto;
import com.assignment.task_tracker.dto.TaskDto;
import com.assignment.task_tracker.dto.UpdateTaskDto;
import com.assignment.task_tracker.entity.Project;
import com.assignment.task_tracker.entity.Task;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.entity.enums.Priority;
import com.assignment.task_tracker.entity.enums.TaskStatus;
import com.assignment.task_tracker.event.TaskStatusChangedEvent;
import com.assignment.task_tracker.exception.BadRequestException;
import com.assignment.task_tracker.exception.ForbiddenException;
import com.assignment.task_tracker.exception.InvalidStatusTransitionException;
import com.assignment.task_tracker.exception.ResourceNotFoundException;
import com.assignment.task_tracker.repository.ProjectMemberRepository;
import com.assignment.task_tracker.repository.ProjectRepository;
import com.assignment.task_tracker.repository.TaskRepository;
import com.assignment.task_tracker.repository.UserRepository;
import com.assignment.task_tracker.util.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.assignment.task_tracker.entity.enums.TaskStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService{
    private static final String TASK_LIST_CACHE_PREFIX = "tasks:project:%d:assignee:%d:page:%d:limit:%d:status:%s:priority:%s";

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ModelMapper modelMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.cache.task-list-ttl-seconds:300}")
    private long taskListTtlSeconds;

    private User currentUser() {
        return SecurityUtils.getCurrentUser();
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private Task getTask(Long projectId, Long taskId) {
        return taskRepository.findByIdAndProjectIdAndIsDeletedFalse(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    @Override
    public TaskDto createTask(Long projectId, CreateTaskDto dto) {
        log.info("Creating task for the project: {}", projectId);
        User currentUser = currentUser();

        Project project = getProject(projectId);
        validateProjectAccess(project);

        Task task = modelMapper.map(dto, Task.class);
        task.setProject(project);
        task.setCreatedBy(currentUser);
        if (dto.getAssigneeId() != null) {
            User assignee = userRepository.findById(dto.getAssigneeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found with id: " + dto.getAssigneeId()));

            boolean member = projectMemberRepository.existsByProjectIdAndUserId(projectId, assignee.getId());

            if (!member) {
                throw new BadRequestException("User is not a project member");
            }

            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);
        invalidateTaskListCache(projectId, getAssigneeId(savedTask));
        log.info("Task created successfully");

        return modelMapper.map(savedTask, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long projectId, Long taskId) {
        log.info("Fetching task by id: ", taskId);

        User currentUser = currentUser();

        Task task = getTask(projectId, taskId);
        validateProjectAccess(task.getProject());

        boolean isMember = isCurrentUserMember();

        if (isMember && (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("Members can only view tasks assigned to them");
        }

        return modelMapper.map(task, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskDto> getAllTasks(Long projectId, int page, int limit, TaskStatus status, Priority priority, Long assigneeId) {
        log.info("Fetching all tasks of project: {}", projectId);
        Project project = getProject(projectId);
        validateProjectAccess(project);
        Pageable pageable = PageRequest.of(page, limit);

        User currentUser = currentUser();

        boolean isMember = isCurrentUserMember();
        if(isMember){
            log.info("Current user is a member. Hence, fetching only those tasks which as assigned to current user");
            assigneeId = currentUser.getId();
        }

        if (assigneeId != null) {
            return getCachedTaskList(projectId, page, limit, status, priority, assigneeId, pageable);
        }

        Page<TaskDto> tasksPage = taskRepository.findTasks(
                projectId,
                status,
                priority,
                assigneeId,
                pageable
        );

        return tasksPage;
    }

    @Override
    public TaskDto updateTask(Long projectId, Long taskId, UpdateTaskDto dto) {
        log.info("Updating task with id: {} of project id: {}", taskId, projectId);
        Task task = getTask(projectId, taskId);
        validateProjectAccess(task.getProject());
        Long previousAssigneeId = getAssigneeId(task);

        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }

        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }

        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }

        if (dto.getAssigneeId() != null) {
            User assignee = userRepository.findById(dto.getAssigneeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));

            boolean member = projectMemberRepository.existsByProjectIdAndUserId(projectId, assignee.getId());

            if (!member) {
                throw new BadRequestException("User is not a project member");
            }

            task.setAssignee(assignee);
        }

        Task updatedTask = taskRepository.save(task);
        invalidateTaskListCache(projectId, previousAssigneeId);
        invalidateTaskListCache(projectId, getAssigneeId(updatedTask));
        log.info("Successfully updated task with id: {}", taskId);

        return modelMapper.map(updatedTask, TaskDto.class);
    }

    @Override
    public TaskDto updateStatus(Long projectId, Long taskId, TaskStatus target) {
        log.info("Updating task status with id: {} of project id: {}", taskId, projectId);
        User currentUser = currentUser();

        Task task = getTask(projectId, taskId);
        validateProjectAccess(task.getProject());

        boolean isMember = isCurrentUserMember();

        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(currentUser.getId());

        if (isMember && !isAssignee) {
            throw new ForbiddenException("Only assignee or manager can update status");
        }

        log.info("Current status: {} changing to new status: {}", task.getStatus(), target.toString());
        TaskStatus previousStatus = task.getStatus();
        validateTransition(task.getStatus(), target);

        task.setStatus(target);
        if (target == TaskStatus.DONE && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }

        Task updatedTask = taskRepository.save(task);
        invalidateTaskListCache(projectId, getAssigneeId(updatedTask));
        publishStatusChangedEvent(updatedTask, previousStatus, target);

        return modelMapper.map(updatedTask, TaskDto.class);
    }

    private void validateTransition(TaskStatus current, TaskStatus target) {
        Map<TaskStatus, Set<TaskStatus>> transitions =
                Map.of(
                        TaskStatus.TODO,
                        Set.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED),

                        TaskStatus.IN_PROGRESS,
                        Set.of(TaskStatus.IN_REVIEW, TaskStatus.BLOCKED),

                        TaskStatus.IN_REVIEW,
                        Set.of(TaskStatus.DONE, TaskStatus.BLOCKED)
                );

        if (!transitions.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidStatusTransitionException(current.name(), target.name());
        }
    }

    @Override
    public void deleteTask(Long projectId, Long taskId) {
        log.info("Deleting task with id: {}", taskId);
        Task task = getTask(projectId, taskId);
        validateProjectAccess(task.getProject());
        Long assigneeId = getAssigneeId(task);

        task.setIsDeleted(true);
        taskRepository.save(task);
        invalidateTaskListCache(projectId, assigneeId);

        log.info("Successfully, deleted task with id: {}", taskId);
    }

    @Override
    public List<TaskDto> getMyTasks() {
        User currentUser = currentUser();
        log.info("Fetching all tasks of user: {}", currentUser.getId());

        List<Task> myTasks = taskRepository.findByAssigneeId(currentUser.getId());

        return myTasks.stream()
                .map(task -> modelMapper.map(task, TaskDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAnalyticsDto> getTaskAnalytics(Long projectId) {
        Project project = getProject(projectId);
        validateProjectAccess(project);

        return taskRepository.getTaskAnalytics(projectId, project.getOrganization().getId())
                .stream()
                .map(row -> new TaskAnalyticsDto(
                        row.getUserId(),
                        row.getName(),
                        row.getEmail(),
                        row.getOverdueTaskCount(),
                        row.getAvgCompletionSeconds()
                ))
                .toList();
    }

    private void validateProjectAccess(Project project) {
        User currentUser = currentUser();

        if (!project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new ForbiddenException("You do not have access to this project");
        }
    }

    private Page<TaskDto> getCachedTaskList(
            Long projectId,
            int page,
            int limit,
            TaskStatus status,
            Priority priority,
            Long assigneeId,
            Pageable pageable
    ) {
        String cacheKey = buildTaskListCacheKey(projectId, assigneeId, page, limit, status, priority);

        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            try {
                CachedTaskPage cachedPage = objectMapper.readValue(cachedValue, CachedTaskPage.class);
                log.info("Task list cache hit for key: {}", cacheKey);
                return new PageImpl<>(cachedPage.content(), pageable, cachedPage.totalElements());
            } catch (JsonProcessingException ex) {
                log.warn("Failed to read cached task list for key: {}. Falling back to DB.", cacheKey, ex);
                redisTemplate.delete(cacheKey);
            }
        }

        Page<TaskDto> tasksPage = taskRepository.findTasks(
                projectId,
                status,
                priority,
                assigneeId,
                pageable
        );

        try {
            CachedTaskPage cachedPage = new CachedTaskPage(tasksPage.getContent(), tasksPage.getTotalElements());
            redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(cachedPage),
                    Duration.ofSeconds(taskListTtlSeconds)
            );
            log.info("Task list cached for key: {}", cacheKey);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to cache task list for key: {}", cacheKey, ex);
        }

        return tasksPage;
    }

    private String buildTaskListCacheKey(
            Long projectId,
            Long assigneeId,
            int page,
            int limit,
            TaskStatus status,
            Priority priority
    ) {
        return TASK_LIST_CACHE_PREFIX.formatted(
                projectId,
                assigneeId,
                page,
                limit,
                status == null ? "ALL" : status.name(),
                priority == null ? "ALL" : priority.name()
        );
    }

    private void invalidateTaskListCache(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }

        String pattern = "tasks:project:%d:assignee:%d:*".formatted(projectId, assigneeId);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        redisTemplate.delete(keys);
        log.info("Invalidated {} task list cache keys for project: {}, assignee: {}", keys.size(), projectId, assigneeId);
    }

    private Long getAssigneeId(Task task) {
        return task.getAssignee() == null ? null : task.getAssignee().getId();
    }

    private void publishStatusChangedEvent(Task task, TaskStatus previousStatus, TaskStatus currentStatus) {
        eventPublisher.publishEvent(new TaskStatusChangedEvent(
                task.getId(),
                task.getProject().getId(),
                getAssigneeId(task),
                task.getTitle(),
                previousStatus,
                currentStatus,
                LocalDateTime.now()
        ));
    }

    private boolean isCurrentUserMember() {
        User currentUser = currentUser();
        boolean isMember = currentUser.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER"));

        return isMember;
    }

    private record CachedTaskPage(List<TaskDto> content, long totalElements) {
        private CachedTaskPage {
            content = content == null ? Collections.emptyList() : content;
        }
    }
}
