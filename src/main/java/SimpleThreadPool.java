

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.RuntimeErrorException;
public class SimpleThreadPool {
    private Worker[] workers;
    private volatile boolean isShutdown = false;
    private volatile boolean isTerminated = false;
    private Thread[] workerThreads;
    private  RejectionPolicy rejectionPolicy;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private int MAX_RETRIES;
    private long RETRY_DELAY_MS;
    private final PriorityQueue<TaskWrapper> delayQueue = new PriorityQueue<>();
    private final Object delayLock = new Object();

    private final LinkedList<TaskWrapper> deadLetterQueue = new LinkedList<>();

    public SimpleThreadPool(int numWorkers, int MAX_QUEUE_SIZE, RejectionPolicy rejectionPolicy,int MAX_RETRIES, long RETRY_DELAY_MS) {
        new Thread(new RetryScheduler(), "retry-scheduler").start();
        this.rejectionPolicy = rejectionPolicy;
        this.MAX_RETRIES = MAX_RETRIES;
        this.RETRY_DELAY_MS = RETRY_DELAY_MS;
        this.workers = new Worker[numWorkers];
        this.workerThreads = new Thread[numWorkers];
        for(int i=0;i<numWorkers;i++){
            workers[i] = new Worker(MAX_QUEUE_SIZE,i);
            workerThreads[i] = new Thread(workers[i], "worker-" + i);
            workerThreads[i].start();
        }
        System.out.println("ThreadPool initialized");

    }

    public void submit(Task task) throws InterruptedException{
        
            if (isShutdown) {
                throw new IllegalStateException("ThreadPool is shutdown");
            }
            TaskWrapper wrapper = new TaskWrapper(task, 0, System.currentTimeMillis());
            int index = Math.abs(roundRobin.getAndIncrement() % workers.length);
            Worker worker = workers[index];
            worker.enqueue(wrapper);
            
    }

    public void shutdown(){
        isShutdown = true;
        for(Worker worker : workers){
            synchronized(worker.queue){
                worker.queue.notifyAll();
            }}
        synchronized(delayLock){
            delayLock.notifyAll();
        }
        }
    public boolean awaitTermination(long timeoutMillis) throws InterruptedException {

        long deadline = System.currentTimeMillis() + timeoutMillis;

        for(Thread t : workerThreads){

            long remaining = deadline - System.currentTimeMillis();

            if(remaining <= 0){
                return false;
            }

            t.join(remaining);
        }

        isTerminated = true;
        return true;
    }
    public LinkedList<Task> shutdownNow(){

        isShutdown = true;

        LinkedList<Task> pending = new LinkedList<>();

        for(Worker worker : workers){

            synchronized(worker.queue){
                for(TaskWrapper tw : worker.queue){
                    pending.add(tw.task);
                }
                worker.queue.clear();
            }
        }

        synchronized(delayLock){
            delayQueue.clear();
            delayLock.notifyAll();
        }

        for(Thread t : workerThreads){
            t.interrupt();
        }

        return pending;
    }
    private void sendToDLQ(TaskWrapper taskWrapper, Exception e){
        synchronized(deadLetterQueue){
            deadLetterQueue.add(taskWrapper);
        }
        System.out.println("Task moved to DLQ after "
                + taskWrapper.attempts + " attempts. Error: " + e.getMessage());

    }
     public int getDeadLetterSize() {
        synchronized (deadLetterQueue) {
            return deadLetterQueue.size();
        }
    }
    private class Worker implements Runnable {
        private final LinkedList<TaskWrapper> queue = new LinkedList<>();
        private final int MAX_QUEUE_SIZE;
        private final int id;
        public Worker(int MAX_QUEUE_SIZE, int id){
            this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
            this.id = id;
        }

        public void enqueue(TaskWrapper taskWrapper) throws InterruptedException{
            synchronized(queue){
                while(queue.size()>= MAX_QUEUE_SIZE){
                    switch(rejectionPolicy){
                        case BLOCK:
                            queue.wait();
                            break;
                        case REJECT:
                            throw new RuntimeException("Queue full loool");
                        case CALLER_RUNS:
                            try {
                            taskWrapper.task.execute();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return;
                    }
                }
                queue.addFirst(taskWrapper);
                queue.notifyAll();
            }
        }
        private TaskWrapper popLocal(){
            synchronized(queue){
                if(!queue.isEmpty()){
                    TaskWrapper wrapper = queue.removeFirst();
                    queue.notifyAll();  
                    return wrapper;
                }
                return null;

            }
        }

        private TaskWrapper steal() {
            for (int i = 0; i < workers.length; i++) {
                if (i == id) continue;

                Worker victim = workers[i];

                synchronized (victim.queue) {
                    TaskWrapper stolen = victim.queue.pollLast();

                    if (stolen != null) {
                        System.out.println(Thread.currentThread().getName()
                                + " stole task from worker-" + i);
                        victim.queue.notifyAll();
                        return stolen;
                    }
                }
            }
            return null;
        }
        private void handleFailure(TaskWrapper taskWrapper,Exception e){
            taskWrapper.attempts++;
            if(taskWrapper.attempts>MAX_RETRIES){
                sendToDLQ(taskWrapper, e);
                return;
            }
            taskWrapper.nextRunTime = System.currentTimeMillis() + RETRY_DELAY_MS;
            
                synchronized(delayLock) {
                delayQueue.add(taskWrapper);
                delayLock.notifyAll();
            }
            
        }
        

        @Override
        public void run() {
            System.out.println("Worker started: " + Thread.currentThread().getName());

            while (true) {

                if (isShutdown) {
                    System.out.println(Thread.currentThread().getName() + " exiting");
                    return;
                }

                TaskWrapper taskWrapper = popLocal();

                if (taskWrapper == null) {
                    taskWrapper = steal();
                }

                if (taskWrapper != null) {

                    long now = System.currentTimeMillis();


                    try {
                        taskWrapper.task.execute();
                    } catch (Exception e) {
                        handleFailure(taskWrapper, e);
                    }

                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    
    }
    private class RetryScheduler implements Runnable {

    @Override
    public void run() {
        while (!isShutdown) {

            try {

                TaskWrapper wrapper;

                synchronized (delayLock) {

                    while (delayQueue.isEmpty()) {
                        delayLock.wait();
                    }

                    wrapper = delayQueue.peek();

                    long now = System.currentTimeMillis();
                    long waitTime = wrapper.nextRunTime - now;

                    if (waitTime > 0) {
                        delayLock.wait(waitTime);
                        continue;
                    }

                    delayQueue.poll();
                }

                Worker worker = workers[Math.abs(roundRobin.getAndIncrement() % workers.length)];
                worker.enqueue(wrapper);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
}

}

