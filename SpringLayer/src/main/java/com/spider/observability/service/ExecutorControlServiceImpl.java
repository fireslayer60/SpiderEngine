package com.spider.observability.service;

import com.engine.Tasks.*;
import com.spider.observability.adapter.ExecutorAdapter;
import com.spider.observability.dto.*;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;
@Service
public class ExecutorControlServiceImpl implements ExecutorControlService {
    private final AtomicInteger taskIdGenerator = new AtomicInteger(0);

    private final ExecutorAdapter adapter;

    public ExecutorControlServiceImpl(ExecutorAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void startPool(int workers) {
        adapter.start(workers);
    }

    @Override
    public void shutdown() {
        adapter.shutdown();
    }

    @Override
    public void submitTask(TaskRequest request){
        validate(request);

        for (int i = 0; i < request.getCount(); i++) {
            try{
                 adapter.submit(mapToTask(request.getType()));
            }
            catch(Exception e){
        
            }
           
        }
    }

    @Override
    public void submitBatch(BatchTaskRequest request) {
        for (TaskRequest req : request.getTasks()) {
            submitTask(req);
        }
    }

    private void validate(TaskRequest request) {
        if (request.getCount() <= 0) {
            throw new IllegalArgumentException("Count must be > 0");
        }
    }

    private Task mapToTask(String type) {
        int id = taskIdGenerator.incrementAndGet();
        return switch (type.toUpperCase()) {
            case "SUCCESS" -> new SuccessTask(id);
            case "FLAKY" -> new FlakyTask(id);
            case "FAILURE" -> new PermanentFailureTask(id);
            case "LONG" -> new LongTask(id);
            default -> throw new IllegalArgumentException("Unknown task type");
        };
    }
}