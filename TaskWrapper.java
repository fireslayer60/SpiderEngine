package SpiderEngine;

class TaskWrapper implements Comparable<TaskWrapper>{
    Task task;
    int attempts;
    long nextRunTime;
    public TaskWrapper(Task task, int i, long timeMillis) {
        this.task = task;
        this.attempts = i;
        this.nextRunTime = timeMillis;
    }
    @Override
    public int compareTo(TaskWrapper other) {
        return Long.compare(this.nextRunTime, other.nextRunTime);
    }
    
}