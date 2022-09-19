package xyz.isaiahp.timer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class TimeOutTests {

    @Test
    public void testEndRanges() {
        long startTime = System.currentTimeMillis();
        long resolution = 512;
        long maxTimeInterval = 2048;
        BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, resolution, maxTimeInterval,
                64);
        int timerId = timeout.scheduleTimer(startTime + 2048);
        Assertions.assertEquals(BitsetTimeWheel.ERR_OUT_OF_RANGE, timerId);

    }
    @Test
    public void testWrap() {
        long startTime = System.currentTimeMillis();
        long resolution = 512;
        long maxTimeInterval = 2048;
        BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, resolution, maxTimeInterval, 64);
        int expired = timeout.pollTimeouts(startTime + 4096, (timeUnit, now, timerId) -> {
            return true;
        });
        Assertions.assertEquals(0, expired);
        Assertions.assertEquals(startTime + 4096, timeout.getCurrentTime());

        int LIMIT = 4096;
        long currentTime = timeout.getCurrentTime();
        for (int i = 1; i < LIMIT; i++) {
            int timerId = timeout.scheduleTimer(currentTime + ((maxTimeInterval) * i) - resolution);
            long timeNow = currentTime + maxTimeInterval * i;
            int count = timeout.pollTimeouts(timeNow, ((timeUnit, now, timerId1) -> {
                Assertions.assertEquals(timerId, timerId1);
                return true;
            }));
            Assertions.assertEquals(1, count);
        }
    }

    @Test
    public void testScheduleCancel() {
        long startTime = System.currentTimeMillis();
        long resolution = 32;
        long maxTimeInterval = 2048;
        BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, resolution, maxTimeInterval, 64);
        int timeoutId = timeout.scheduleTimer(startTime + 1 );
        Assertions.assertTrue( timeoutId >= 0);
        timeout.cancelTimer(timeoutId);
        Assertions.assertEquals(0, timeout.pollTimeouts(startTime + 32, (timeUnit, now, timerId) -> {
            throw new IllegalStateException("time should be cancelled");
                }
        ));

    }
        @Test
    public void testScheduleATimeout() {
        long startTime = System.currentTimeMillis();
        long resolution = 32;
        long maxTimeInterval = 2048;
        BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, resolution, maxTimeInterval, 64);
        int timeoutId = timeout.scheduleTimer(startTime + 1);

        Assertions.assertEquals(0, timeout.pollTimeouts(startTime + 30, (timeUnit, now, timerId) -> true
        ));

        
        Assertions.assertEquals(1, timeout.pollTimeouts(startTime + 32, (timeUnit, now, timerId) -> {
            Assertions.assertEquals(timeoutId, timerId);
            return true;
                }
        ));

    }
}
