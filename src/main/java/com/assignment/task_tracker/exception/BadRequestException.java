package com.assignment.task_tracker.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}