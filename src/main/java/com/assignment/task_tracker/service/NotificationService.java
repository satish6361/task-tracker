package com.assignment.task_tracker.service;

import com.assignment.task_tracker.dto.TaskStatusChangedNotificationDto;
import com.assignment.task_tracker.event.TaskStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationService {
    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emittersByUserId.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ex -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Subscribed to task status notifications"));
        } catch (IOException ex) {
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    @EventListener
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        if (event.assigneeId() == null) {
            return;
        }

        List<SseEmitter> emitters = emittersByUserId.getOrDefault(event.assigneeId(), List.of());
        if (emitters.isEmpty()) {
            return;
        }

        TaskStatusChangedNotificationDto notification = new TaskStatusChangedNotificationDto(
                event.taskId(),
                event.projectId(),
                event.assigneeId(),
                event.title(),
                event.previousStatus(),
                event.currentStatus(),
                event.changedAt()
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("task-status-changed")
                        .data(notification));
            } catch (IOException ex) {
                log.warn("Removing broken SSE emitter for user: {}", event.assigneeId(), ex);
                removeEmitter(event.assigneeId(), emitter);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
