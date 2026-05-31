package com.assignment.task_tracker.exception;

public class InvalidStatusTransitionException extends BadRequestException {
    public InvalidStatusTransitionException(String from, String to) {
        super("Cannot transition task from %s to %s".formatted(from, to));
    }
}
