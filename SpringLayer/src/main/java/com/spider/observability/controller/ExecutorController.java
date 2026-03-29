package com.spider.observability.controller;

import com.spider.observability.dto.*;
import com.spider.observability.service.ExecutorControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/executor")
public class ExecutorController {

    private final ExecutorControlService service;

    public ExecutorController(ExecutorControlService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestParam int workers) {
        service.startPool(workers);
        return ResponseEntity.ok("Executor started");
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submit(@RequestBody TaskRequest request) {
        service.submitTask(request);
        return ResponseEntity.ok("Tasks submitted");
    }

    @PostMapping("/submit-batch")
    public ResponseEntity<String> submitBatch(@RequestBody BatchTaskRequest request) {
        service.submitBatch(request);
        return ResponseEntity.ok("Batch submitted");
    }

    @PostMapping("/shutdown")
    public ResponseEntity<String> shutdown() {
        service.shutdown();
        return ResponseEntity.ok("Executor shutdown");
    }
}