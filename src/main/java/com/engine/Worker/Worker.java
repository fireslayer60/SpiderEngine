package com.engine.Worker;

import java.util.EventListener;
import java.util.LinkedList;

import com.engine.SimpleThreadPool;
import com.engine.Tasks.TaskWrapper;
import com.engine.observability.enums.WorkerState;
import com.engine.observability.listner.ExecutorEventListener;

public class Worker implements Runnable {
    private final LinkedList<TaskWrapper> queue = new LinkedList<>();
    private final int MAX_QUEUE_SIZE;
    private final int id;
    private final SimpleThreadPool pool;
    private final ExecutorEventListener eventListener;

    public Worker(int MAX_QUEUE_SIZE, int id, SimpleThreadPool pool, ExecutorEventListener eventListener) {
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.id = id;
        this.pool = pool;
        this.eventListener = eventListener;
    }

    public void enqueue(TaskWrapper taskWrapper) throws InterruptedException {
        synchronized (queue) {
            while (queue.size() >= MAX_QUEUE_SIZE) {
                switch (pool.getRejectionPolicy()) {
                    case BLOCK: queue.wait(); break;
                    case REJECT: throw new RuntimeException("Queue full");
                    case CALLER_RUNS:
                        try { taskWrapper.executeTask(); } 
                        catch (Exception e) { e.printStackTrace(); }
                        return;
                }
            }
            queue.addFirst(taskWrapper);
            queue.notifyAll();
        }
    }
    public LinkedList<TaskWrapper> getQueue(){
        return queue;
    }

    // YOUR ORIGINAL POPLOCAL IS BACK
    private TaskWrapper popLocal() {
        synchronized (queue) {
            if (!queue.isEmpty()) {
                TaskWrapper wrapper = queue.removeFirst();
                queue.notifyAll();
                return wrapper;
            }
            return null;
        }
    }

    // YOUR ORIGINAL STEAL IS BACK
    private TaskWrapper steal() {
        Worker[] allWorkers = pool.getWorkers(); // Gets the array from the main pool
        for (int i = 0; i < allWorkers.length; i++) {
            if (i == id) continue;

            Worker victim = allWorkers[i];

            synchronized (victim.queue) {
                TaskWrapper stolen = victim.queue.pollLast();

                if (stolen != null) {
                    System.out.println(Thread.currentThread().getName() + " stole task from worker-" + i);
                    eventListener.onTaskStolen(Thread.currentThread().getName(), "worker-"+i, stolen.getTask());
                    victim.queue.notifyAll();
                    return stolen;
                }
            }
        }
        return null;
    }

    @Override
    public void run() {
        System.out.println("Worker started: " + Thread.currentThread().getName());
        eventListener.onWorkerStateChange(Thread.currentThread().getName(), WorkerState.RUNNING);

        while (!pool.isShutdown()) {
            
            TaskWrapper taskWrapper = popLocal();

            if (taskWrapper == null) {
                taskWrapper = steal();
            }

            if (taskWrapper != null) {
                try {
                    eventListener.onTaskStarted(Thread.currentThread().getName(), taskWrapper.getTask());
                    taskWrapper.executeTask();
                    eventListener.onTaskCompleted(Thread.currentThread().getName(), taskWrapper.getTask());
                } catch (Exception e) {
                    eventListener.onTaskFailed(Thread.currentThread().getName(), taskWrapper.getTask(), e);
                    pool.handleFailure(taskWrapper, e);
                    continue;
                }
            } else {
                try { 
                    Thread.sleep(50); 
                } catch (InterruptedException e) { 
                    Thread.currentThread().interrupt(); 
                    return; 
                }
            }
            
        }
        
        System.out.println(Thread.currentThread().getName() + " exiting");
    }
}