package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public interface TimeOut {

    int scheduleTimer(long deadline);
    boolean cancelTimer(int timeoutId);

    int pollTimeouts(final long now, final TimeOut.Handler handler);


    @FunctionalInterface
    interface Handler
    {
        boolean onTimeout(TimeUnit timeUnit, long now, long timerId);
    }
}
