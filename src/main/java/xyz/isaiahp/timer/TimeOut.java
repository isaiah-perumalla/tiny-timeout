package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public interface TimeOut {

    int scheduleTimeout(long deadline);
    boolean cancelTimer(int timeoutId);

    int pollTimeouts(final long now, final TimeOut.Handler handler);


    @FunctionalInterface
    interface Handler
    {
        void onTimeout(TimeUnit timeUnit, long now, int timerId);
    }
}
