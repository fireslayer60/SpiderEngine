package com.spider.observability.service;

import com.spider.observability.dto.TaskRequest;
import com.spider.observability.dto.BatchTaskRequest;

public interface ExecutorControlService {

    void startPool(int workers);

    void shutdown();

    void submitTask(TaskRequest request);

    void submitBatch(BatchTaskRequest request);
}