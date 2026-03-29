package com.engine.observability.listner;

import com.engine.Tasks.Task;
import com.engine.observability.enums.EventType;
import com.engine.observability.enums.WorkerState;
import com.engine.observability.stats.ExecutorEvent;

import java.util.HashMap;
import java.util.Map;

public class EventBusExecutorListener implements ExecutorEventListener {

    private final ExecutorEventBus eventBus;

    public EventBusExecutorListener(ExecutorEventBus eventBus) {
        this.eventBus = eventBus;
    }

    private void publish(EventType type, String workerId, Task task, Map<String, Object> metadata) {

        ExecutorEvent event = new ExecutorEvent(
                type,
                System.currentTimeMillis(),
                workerId,
                task != null ? task.getId() : null,
                metadata
        );

        eventBus.publish(event);
    }

    @Override
    public void onTaskSubmitted(Task task) {
        publish(EventType.TASK_SUBMITTED, null, task, null);
    }

    @Override
    public void onTaskStarted(String workerId, Task task) {
        publish(EventType.TASK_STARTED, workerId, task, null);
    }

    @Override
    public void onTaskCompleted(String workerId, Task task) {
        publish(EventType.TASK_COMPLETED, workerId, task, null);
    }

    @Override
    public void onTaskFailed(String workerId, Task task, Throwable error) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("error", error.getMessage());

        publish(EventType.TASK_FAILED, workerId, task, meta);
    }

    @Override
    public void onTaskRetryScheduled(Task task, int retryCount) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("retryCount", retryCount);

        publish(EventType.TASK_RETRY_SCHEDULED, null, task, meta);
    }

    @Override
    public void onTaskMovedToDLQ(Task task) {
        publish(EventType.TASK_MOVED_TO_DLQ, null, task, null);
    }

    @Override
    public void onTaskStolen(String thiefWorkerId, String victimWorkerId, Task task) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("victimWorkerId", victimWorkerId);

        publish(EventType.TASK_STOLEN, thiefWorkerId, task, meta);
    }

    @Override
    public void onWorkerStateChange(String workerId, WorkerState newState) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("state", newState.name());

        publish(EventType.WORKER_STATE_CHANGE, workerId, null, meta);
    }
}
