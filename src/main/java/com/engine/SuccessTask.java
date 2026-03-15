package com.engine;


public class SuccessTask implements Task {
    private final int id;

    public SuccessTask(int id) {
        this.id = id;
    }

    @Override
    public void execute() {
        System.out.println("SuccessTask " + id +
                " executed by " + Thread.currentThread().getName());
    }
}