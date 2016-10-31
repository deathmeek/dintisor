package upb.com.smarttooth;

public class ToothWatchDog {
    private final long timeout;
    private Thread thread;

    public ToothWatchDog(long timeout) {
        this.timeout = timeout;
    }

    public void reset() {
        try {
            thread.interrupt();
        } catch (Exception e){
            //all is good
        }
    }

    public void start() {
        reset();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeout);
                    Tooth.getInstance().resetBluetooth();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }
}