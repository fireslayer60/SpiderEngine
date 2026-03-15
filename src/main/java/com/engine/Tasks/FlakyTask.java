package com.engine.Tasks;

public class FlakyTask implements Task {
    private final int id;
    private boolean hasFailed = false;

    public FlakyTask(int id) {
        this.id = id;
    }

    @Override
    public void execute() throws Exception {
        if (!hasFailed) {
            hasFailed = true;
            System.out.println("FlakyTask " + id + " failing first time");
            throw new RuntimeException("Intentional failure");
        }

        System.out.println("FlakyTask " + id +
                " succeeded on retry by " + Thread.currentThread().getName());
    }
}