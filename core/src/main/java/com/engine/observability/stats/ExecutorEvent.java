package com.engine.observability.stats;

import java.util.Map;

import com.engine.observability.enums.EventType;

public class ExecutorEvent {

    private final EventType type;
    private final long timestamp;

    private final String workerId;
    private final String taskId;

    private final Map<String, Object> metadata;

    public ExecutorEvent(
            EventType type,
            long timestamp,
            String workerId,
            String taskId,
            Map<String, Object> metadata
    ) {
        this.type = type;
        this.timestamp = timestamp;
        this.workerId = workerId;
        this.taskId = taskId;
        this.metadata = metadata;
    }

    public EventType getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public String getWorkerId() { return workerId; }
    public String getTaskId() { return taskId; }
    public Map<String, Object> getMetadata() { return metadata; }
}
