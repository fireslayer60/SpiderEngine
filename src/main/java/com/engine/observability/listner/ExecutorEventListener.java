package com.engine.observability.listner;

import com.engine.Tasks.Task;
import com.engine.observability.enums.WorkerState;

public interface ExecutorEventListener {

    void onTaskSubmitted(Task task);

    void onTaskStarted(String workerId, Task task);

    void onTaskCompleted(String workerId, Task task);

    void onTaskFailed(String workerId, Task task, Throwable error);

    void onTaskRetryScheduled(Task task, int retryCount);

    void onTaskMovedToDLQ(Task task);

    void onTaskStolen(String thiefWorkerId, String victimWorkerId, Task task);

    void onWorkerStateChange(String workerId, WorkerState newState);
}
