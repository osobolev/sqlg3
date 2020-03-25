package sqlg3.remote.common;

public final class WatcherThread implements Runnable {

    /**
     * Client activity check interval.
     * If client does not call any method between checks its session is closed.
     */
    public static final long ACTIVITY_CHECK_INTERVAL = 60 * 1000L;

    private final Object waitLock = new Object();
    private boolean running = true;

    private final int divider;
    private final Runnable check;

    public WatcherThread(int divider, Runnable check) {
        this.divider = divider;
        this.check = check;
    }

    public void run() {
        while (true) {
            synchronized (waitLock) {
                try {
                    waitLock.wait(ACTIVITY_CHECK_INTERVAL / divider);
                } catch (InterruptedException ie) {
                    // ignore
                }
                if (!running)
                    break;
            }
            check.run();
        }
    }

    public void shutdown() {
        synchronized (waitLock) {
            running = false;
            waitLock.notifyAll();
        }
    }

    public void runThread() {
        Thread thread = new Thread(this, "Watcher");
        thread.setDaemon(true);
        thread.start();
    }
}
