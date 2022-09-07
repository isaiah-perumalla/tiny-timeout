package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public interface TimeOut {

    int scheduleTimer(long deadline);
    boolean cancelTimer(int timeoutId);

    @FunctionalInterface
    interface Handler
    {
        boolean onTimeout(TimeUnit timeUnit, long now, long timerId);
    }
}
