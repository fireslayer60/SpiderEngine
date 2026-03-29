package com.spider.observability.dto;

import java.util.List;

public class BatchTaskRequest {
    private List<TaskRequest> tasks;

    public List<TaskRequest> getTasks() { return tasks; }
    public void setTasks(List<TaskRequest> tasks) { this.tasks = tasks; }
}