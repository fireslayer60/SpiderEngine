package com.engine.observability.stats;

import java.util.Map;

public class ExecutorStats {

    private final int totalWorkers;
    private final int activeWorkers;
    private final int idleWorkers;

    private final int delayQueueSize;
    private final int retryQueueSize;
    private final int dlqSize;

    private final Map<String, Integer> workerQueueSizes;

    public ExecutorStats(
            int totalWorkers,
            int activeWorkers,
            int idleWorkers,
            int delayQueueSize,
            int retryQueueSize,
            int dlqSize,
            Map<String, Integer> workerQueueSizes
    ) {
        this.totalWorkers = totalWorkers;
        this.activeWorkers = activeWorkers;
        this.idleWorkers = idleWorkers;
        this.delayQueueSize = delayQueueSize;
        this.retryQueueSize = retryQueueSize;
        this.dlqSize = dlqSize;
        this.workerQueueSizes = workerQueueSizes;
    }

    public int getTotalWorkers() { return totalWorkers; }

    public int getActiveWorkers() { return activeWorkers; }

    public int getIdleWorkers() { return idleWorkers; }

    public int getDelayQueueSize() { return delayQueueSize; }

    public int getRetryQueueSize() { return retryQueueSize; }

    public int getDlqSize() { return dlqSize; }

    public Map<String, Integer> getWorkerQueueSizes() { return workerQueueSizes; }
}