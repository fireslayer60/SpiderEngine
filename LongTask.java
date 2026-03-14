package SpiderEngine;

class LongTask implements Task {

    private int id;

    public LongTask(int id) {
        this.id = id;
    }

    @Override
    public void execute() throws Exception {
        System.out.println("LongTask " + id + " started by " +
                Thread.currentThread().getName());

        Thread.sleep(3000);

        System.out.println("LongTask " + id + " finished");
    }
}