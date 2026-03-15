

public class SpiderEngine {

    public static void main(String[] args) throws Exception {

        SimpleThreadPool pool =
                new SimpleThreadPool(
                        4,
                        5,
                        RejectionPolicy.BLOCK,
                        2,
                        100
                );

        // success
        for (int i = 1; i <= 3; i++) {
            pool.submit(new SuccessTask(i));
        }

        // retry
        for (int i = 1; i <= 50; i++) {
            pool.submit(new FlakyTask(i));
        }

        /*// DLQ
        for (int i = 1; i <= 3; i++) {
            pool.submit(new PermanentFailureTask(i));
        }
        //LT
         for (int i = 1; i <= 8; i++) {
            pool.submit(new LongTask(i));
        }*/

        Thread.sleep(2000);

        System.out.println("\n--- exiting gracefully ---");

       pool.shutdown();

        System.out.println("DLQ size: " + pool.getDeadLetterSize());
        System.out.println("Shutdown complete");
    }
}