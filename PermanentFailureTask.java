package SpiderEngine;

public class PermanentFailureTask implements Task {
    private final int id;

    public PermanentFailureTask(int id) {
        this.id = id;
    }

    @Override
    public void execute() throws Exception {
        System.out.println("PermanentFailureTask " + id + " failing");
        throw new RuntimeException("Permanent failure");
    }
}