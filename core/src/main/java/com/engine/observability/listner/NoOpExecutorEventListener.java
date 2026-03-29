package com.engine.observability.listner;

import com.engine.Tasks.Task;
import com.engine.observability.enums.WorkerState;

public class NoOpExecutorEventListener implements ExecutorEventListener {

    @Override
    public void onTaskSubmitted(Task task) {}

    @Override
    public void onTaskStarted(String workerId, Task task) {}

    @Override
    public void onTaskCompleted(String workerId, Task task) {}

    @Override
    public void onTaskFailed(String workerId, Task task, Throwable error) {}

    @Override
    public void onTaskRetryScheduled(Task task, int retryCount) {}

    @Override
    public void onTaskMovedToDLQ(Task task) {}

    @Override
    public void onTaskStolen(String thiefWorkerId, String victimWorkerId, Task task) {}

    @Override
    public void onWorkerStateChange(String workerId, WorkerState newState) {}
}