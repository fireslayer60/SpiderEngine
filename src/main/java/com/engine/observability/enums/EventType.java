package com.engine.observability.enums;

public enum EventType {

    TASK_SUBMITTED,
    TASK_STARTED,
    TASK_COMPLETED,
    TASK_FAILED,
    TASK_RETRY_SCHEDULED,
    TASK_MOVED_TO_DLQ,
    TASK_STOLEN,
    WORKER_STATE_CHANGE
}
