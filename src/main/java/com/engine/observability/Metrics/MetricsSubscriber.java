package com.engine.observability.Metrics;

import io.micrometer.core.instrument.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.engine.observability.listner.ExecutorEventSubscriber;
import com.engine.observability.stats.ExecutorEvent;

public class MetricsSubscriber implements ExecutorEventSubscriber {

    private final MeterRegistry registry;

    // Counters
    private final Counter tasksSubmitted;
    private final Counter tasksCompleted;
    private final Counter tasksFailed;
    private final Counter tasksRetried;
    private final Counter tasksDLQ;
    private final Counter tasksStolen;

    // Gauges (stateful)
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicInteger idleWorkers = new AtomicInteger(0);
    private final Map<String, String> workerStates = new ConcurrentHashMap<>();

    public MetricsSubscriber(MeterRegistry registry) {
        this.registry = registry;

        this.tasksSubmitted = registry.counter("executor.tasks.submitted");
        this.tasksCompleted = registry.counter("executor.tasks.completed");
        this.tasksFailed = registry.counter("executor.tasks.failed");
        this.tasksRetried = registry.counter("executor.tasks.retried");
        this.tasksDLQ = registry.counter("executor.tasks.dlq");
        this.tasksStolen = registry.counter("executor.tasks.stolen");

        // Register gauges
        registry.gauge("executor.workers.active", activeWorkers);
        registry.gauge("executor.workers.idle", idleWorkers);
    }

    @Override
    public void onEvent(ExecutorEvent event) {

        switch (event.getType()) {

            case TASK_SUBMITTED -> tasksSubmitted.increment();

            case TASK_COMPLETED -> tasksCompleted.increment();

            case TASK_FAILED -> tasksFailed.increment();

            case TASK_RETRY_SCHEDULED -> tasksRetried.increment();

            case TASK_MOVED_TO_DLQ -> tasksDLQ.increment();

            case TASK_STOLEN -> tasksStolen.increment();

            case WORKER_STATE_CHANGE -> handleWorkerState(event);

            default -> {}
        }
    }

    private void handleWorkerState(ExecutorEvent event) {

        String workerId = event.getWorkerId();
        Object stateObj = event.getMetadata() != null 
                ? event.getMetadata().get("state") 
                : null;

        if (workerId == null || stateObj == null) return;

        String newState = stateObj.toString();

        // update latest state
        workerStates.put(workerId, newState);

        // recompute counts
        int active = 0;
        int idle = 0;

        for (String state : workerStates.values()) {
            if (state.equals("RUNNING")) active++;
            else if (state.equals("IDLE")) idle++;
        }

        activeWorkers.set(active);
        idleWorkers.set(idle);
    }
}