package xyz.isaiahp.timer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class TimeOutTests {

    @Test
    public void testScheduleATimeout() {
        long startTime = 1661971123000L;
        long resolution = 32;
        long maxTimeInterval = 2048;
        FixedTimeOut timeout = new FixedTimeOut(TimeUnit.MILLISECONDS, startTime, resolution, maxTimeInterval);
        int timeoutId = timeout.schedule(startTime + 32);

        timeout.advanceCurrentTick(startTime + 30);
//        Assertions.assertEquals(0, timeout.pollTimeouts(startTime + 30, (timeUnit, now, timerId) -> true,
//                10));

        timeout.advanceCurrentTick(startTime + 32);
//        Assertions.assertEquals(1, timeout.pollTimeouts(startTime + 32, (timeUnit, now, timerId) -> true,
//                10));


    }
}
