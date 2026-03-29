package com.engine.Tasks;

import java.util.LinkedList;

public class TaskWrapper implements Comparable<TaskWrapper>{
    private Task task;
    private int attempts;
    private long nextRunTime;
    public TaskWrapper(Task task, int i, long timeMillis) {
        this.task = task;
        this.attempts = i;
        this.nextRunTime = timeMillis;
    }
    @Override
    public int compareTo(TaskWrapper other) {
        return Long.compare(this.nextRunTime, other.nextRunTime);
    }
    public void executeTask() throws Exception{
        task.execute();
    }
    public void addToQueue(LinkedList<Task> q){
        q.add(task);
    }
    public void addAttempts(int i){
        attempts+=i;
    }
    public int getAttempts(){
        return attempts;
    }
    public void setRuntime(long i){
        nextRunTime = i;
    }
    public long getRuntime(){
        return nextRunTime;
    }
    public Task getTask(){
        return task;
    }
    
}