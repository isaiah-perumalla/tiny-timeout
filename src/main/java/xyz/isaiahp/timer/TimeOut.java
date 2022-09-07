package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public interface TimeOut {

    @FunctionalInterface
    interface Handler
    {
        boolean onTimeout(TimeUnit timeUnit, long now, long timerId);
    }
}
