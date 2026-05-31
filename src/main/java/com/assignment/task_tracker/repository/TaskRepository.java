package com.assignment.task_tracker.repository;

import com.assignment.task_tracker.dto.TaskDto;
import com.assignment.task_tracker.entity.Task;
import com.assignment.task_tracker.entity.enums.Priority;
import com.assignment.task_tracker.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    interface TaskAnalyticsProjection {
        Long getUserId();
        String getName();
        String getEmail();
        Long getOverdueTaskCount();
        Double getAvgCompletionSeconds();
    }

    Optional<Task> findByIdAndProjectIdAndIsDeletedFalse(Long taskId, Long projectId);

    @Query("""
    SELECT new com.assignment.task_tracker.dto.TaskDto(t.id, t.title, t.description, t.priority, t.status, t.assignee.id, t.project.id, t.dueDate, t.createdAt)
    FROM Task t
    WHERE t.project.id = :projectId
      AND t.isDeleted = false
      AND (:status IS NULL OR t.status = :status)
      AND (:priority IS NULL OR t.priority = :priority)
      AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
""")
    Page<TaskDto> findTasks(
            @Param("projectId") Long projectId,
            @Param("status") TaskStatus status,
            @Param("priority") Priority priority,
            @Param("assigneeId") Long assigneeId,
            Pageable pageable
    );

    List<Task> findByAssigneeId(Long id);

    @Query(value = """
            SELECT
                u.id AS userId,
                u.name AS name,
                u.email AS email,
                COALESCE(SUM(
                    CASE
                        WHEN t.due_date < CURRENT_DATE
                         AND t.status <> 'DONE'
                         AND t.is_deleted = false
                        THEN 1
                        ELSE 0
                    END
                ), 0) AS overdueTaskCount,
                AVG(
                    CASE
                        WHEN t.status = 'DONE'
                         AND t.completed_at IS NOT NULL
                         AND t.is_deleted = false
                        THEN EXTRACT(EPOCH FROM (t.completed_at - t.created_at))
                        ELSE NULL
                    END
                ) AS avgCompletionSeconds
            FROM app_user u
            JOIN project_member pm ON pm.user_id = u.id
            JOIN project p ON p.id = pm.project_id
            LEFT JOIN task t ON t.assignee_id = u.id
                AND t.project_id = p.id
            WHERE p.id = :projectId
              AND p.organization_id = :organizationId
            GROUP BY u.id, u.name, u.email
            ORDER BY overdueTaskCount DESC, u.id ASC
            """, nativeQuery = true)
    List<TaskAnalyticsProjection> getTaskAnalytics(
            @Param("projectId") Long projectId,
            @Param("organizationId") Long organizationId
    );
}
