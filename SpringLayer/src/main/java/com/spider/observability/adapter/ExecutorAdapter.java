package com.spider.observability.adapter;
import com.engine.Tasks.Task;
import com.engine.SimpleThreadPool;
import com.engine.RejectionPolicy;
import com.engine.observability.listner.NoOpExecutorEventListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutorAdapter {

    private SimpleThreadPool executor;

    // 🔥 Start executor
    public synchronized void start(int workers) {
        if (executor != null) {
            throw new IllegalStateException("Executor already running");
        }

        executor = new SimpleThreadPool(
                        4,
                        5,
                        RejectionPolicy.BLOCK,
                        2,
                        100, new NoOpExecutorEventListener()
                );
    }

    // 🔥 Submit task
    public void submit(Task task) throws InterruptedException{
        ensureRunning();
        executor.submit(task);
    }

    // 🔥 Shutdown
    public synchronized void shutdown() {
        ensureRunning();
        executor.shutdown();
        executor = null;
    }

    private void ensureRunning() {
        if (executor == null) {
            throw new IllegalStateException("Executor not started");
        }
    }
}