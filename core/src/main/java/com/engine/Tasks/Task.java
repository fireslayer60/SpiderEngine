package com.engine.Tasks;


public interface Task {
    void execute() throws Exception;
    default String getId() {
        return "task-" + System.identityHashCode(this);
    }
}

