package com.engine.observability.listner;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.engine.observability.stats.ExecutorEvent;

public class ExecutorEventBus {

    private final BlockingQueue<ExecutorEvent> queue = new LinkedBlockingQueue<>();
    private final List<ExecutorEventSubscriber> subscribers = new CopyOnWriteArrayList<>();

    private volatile boolean running = true;
    private final Thread dispatcherThread;

    public ExecutorEventBus() {
        dispatcherThread = new Thread(this::dispatchLoop, "event-bus-dispatcher");
        dispatcherThread.start();
    }

    public void publish(ExecutorEvent event) {
        if (!running) return;
        queue.offer(event);
    }

    public void subscribe(ExecutorEventSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    private void dispatchLoop() {
        while (running) {
            try {
                ExecutorEvent event = queue.take();

                for (ExecutorEventSubscriber sub : subscribers) {
                    try {
                        sub.onEvent(event);
                    } catch (Exception e) {
                        e.printStackTrace(); // isolate subscriber failure
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        running = false;
        dispatcherThread.interrupt();
    }
}