package com.engine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.engine.Tasks.Task;
import com.engine.Tasks.TaskWrapper;
import com.engine.Worker.Worker;
import com.engine.observability.listner.ExecutorEventListener;
import com.engine.observability.listner.NoOpExecutorEventListener;
import com.engine.observability.stats.ExecutorStats;

public class SimpleThreadPool {
    private final Worker[] workers;
    private final Thread[] workerThreads;
    private volatile boolean isShutdown = false;
    private volatile boolean isTerminated = false;
    
    private final RejectionPolicy rejectionPolicy;
    private final AtomicInteger roundRobin = new AtomicInteger(0);
    private final int maxRetries;
    private final long retryDelayMs;

    private final PriorityQueue<TaskWrapper> delayQueue = new PriorityQueue<>();
    private final Object delayLock = new Object();
    private final LinkedList<TaskWrapper> deadLetterQueue = new LinkedList<>();
    private ExecutorEventListener eventListener = new NoOpExecutorEventListener();

    public SimpleThreadPool(int numWorkers, int queueSize, RejectionPolicy policy, int retries, long delay, ExecutorEventListener eventListener) {
        this.rejectionPolicy = policy;
        this.maxRetries = retries;
        this.retryDelayMs = delay;
        this.workers = new Worker[numWorkers];
        this.workerThreads = new Thread[numWorkers];
        this.eventListener = eventListener;

        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new Worker(queueSize, i, this,eventListener);
            workerThreads[i] = new Thread(workers[i], "worker-" + i);
            workerThreads[i].start();
        }
        new Thread(new RetryScheduler(), "retry-scheduler").start();
        System.out.println("ThreadPool initialized");
    }
    

    public void submit(Task task) throws InterruptedException {
        if (isShutdown) throw new IllegalStateException("ThreadPool is shutdown");
        TaskWrapper wrapper = new TaskWrapper(task, 0, System.currentTimeMillis());
        getNextWorker().enqueue(wrapper);
        eventListener.onTaskSubmitted(task);
    }

    public void handleFailure(TaskWrapper taskWrapper, Exception e) {
        taskWrapper.addAttempts(1);
        if (taskWrapper.getAttempts() > maxRetries) {
            sendToDLQ(taskWrapper, e);
        } else {
            taskWrapper.setRuntime(System.currentTimeMillis() + retryDelayMs);
            synchronized (delayLock) {
                delayQueue.add(taskWrapper);
                delayLock.notifyAll();
            }
        }
    }

    private void sendToDLQ(TaskWrapper taskWrapper, Exception e) {
        synchronized (deadLetterQueue) {
            deadLetterQueue.add(taskWrapper);
        }
        eventListener.onTaskMovedToDLQ(taskWrapper.getTask());
        System.out.println("Task moved to DLQ after " + taskWrapper.getAttempts() + " attempts. Error: " + e.getMessage());
    }

    public void shutdown() {
        isShutdown = true;
        for (Worker worker : workers) {
            LinkedList<TaskWrapper> q = worker.getQueue();
            synchronized (q) { q.notifyAll(); }
        }
        synchronized (delayLock) { delayLock.notifyAll(); }
    }

    public LinkedList<Task> shutdownNow() {
        isShutdown = true;
        LinkedList<Task> pending = new LinkedList<>();
        for (Worker worker : workers) {
            LinkedList<TaskWrapper> q = worker.getQueue();
            synchronized (q) {
                for (TaskWrapper tw : q) { tw.addToQueue(pending); }
                q.clear();
            }
        }
        synchronized (delayLock) {
            for (TaskWrapper tw : delayQueue) { 
                tw.addToQueue(pending);}
            delayQueue.clear();
            delayLock.notifyAll();
        }
        for (Thread t : workerThreads) { t.interrupt(); }
        return pending;

    }

    public boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (Thread t : workerThreads) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) return false;
            t.join(remaining);
        }
        isTerminated = true;
        return true;
    }
    public Worker[] getWorkers(){
        return workers;
    }


    public Worker getNextWorker() {
        return workers[Math.abs(roundRobin.getAndIncrement() % workers.length)];
    }
    public boolean isShutdown() { return isShutdown; }
    public RejectionPolicy getRejectionPolicy() { return rejectionPolicy; }
    public int getDeadLetterSize() { synchronized (deadLetterQueue) { return deadLetterQueue.size(); } }

    private class RetryScheduler implements Runnable {
        @Override
        public void run() {
            
            while (!isShutdown) {
                try {
                    
                    TaskWrapper wrapper;
                    
                    synchronized (delayLock) {
                        while (delayQueue.isEmpty() && !isShutdown) delayLock.wait();
                        if (isShutdown) return;
                        wrapper = delayQueue.peek();
                        long waitTime = wrapper.getRuntime() - System.currentTimeMillis();
                        if (waitTime > 0) {
                            delayLock.wait(waitTime);
                            continue;
                        }
                        delayQueue.poll();
                    }
                    eventListener.onTaskRetryScheduled(wrapper.getTask(), wrapper.getAttempts());
                    getNextWorker().enqueue(wrapper);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

}