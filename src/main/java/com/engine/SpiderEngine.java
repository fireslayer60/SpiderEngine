package com.engine;

import com.engine.Tasks.FlakyTask;
import com.engine.Tasks.PermanentFailureTask;
import com.engine.Tasks.SuccessTask;
import com.engine.observability.listner.EventBusExecutorListener;
import com.engine.observability.listner.ExecutorEventBus;
import com.engine.observability.listner.NoOpExecutorEventListener;

public class SpiderEngine {

    public static void main(String[] args) throws Exception {
        ExecutorEventBus eventBus = new ExecutorEventBus();
        eventBus.subscribe(event -> {
            System.out.printf(
                "%s | worker=%s | task=%s | meta=%s%n",
                event.getType(),
                event.getWorkerId(),
                event.getTaskId(),
                event.getMetadata()
            );
        });

        SimpleThreadPool pool =
                new SimpleThreadPool(
                        4,
                        5,
                        RejectionPolicy.BLOCK,
                        2,
                        100, new NoOpExecutorEventListener()
                );
        pool.setEventListener(new EventBusExecutorListener(eventBus));

        // success
        for (int i = 1; i <= 3; i++) {
            pool.submit(new SuccessTask(i));
        }

        // retry
        for (int i = 1; i <= 50; i++) {
            pool.submit(new FlakyTask(i));
        }

        
        for (int i = 1; i <= 3; i++) {
            pool.submit(new PermanentFailureTask(i));
        }
        /*//LT
         for (int i = 1; i <= 8; i++) {
            pool.submit(new LongTask(i));
        }*/

        Thread.sleep(2000);

        System.out.println("\n--- exiting gracefully ---");

       pool.shutdown();

        System.out.println("DLQ size: " + pool.getDeadLetterSize());
        System.out.println("Shutdown complete");
    }
}