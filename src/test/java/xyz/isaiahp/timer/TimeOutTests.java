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
        int timerId = timeout.scheduleTimeout(startTime + 4096);
        Assertions.assertEquals(BitsetTimeWheel.ERR_OUT_OF_RANGE, timerId);

        //should not allow (one slot reserved for buffering timers when scheduling during call back)
        timerId = timeout.scheduleTimeout(startTime + 4096 - resolution);
        Assertions.assertEquals(BitsetTimeWheel.ERR_OUT_OF_RANGE, timerId);

        timerId = timeout.scheduleTimeout(startTime + 4096 - 2*resolution);
        Assertions.assertTrue(timerId >= 0);
    }

    @Test
    public void testErrWhenCapacityExceeded() {

        long startTime = System.currentTimeMillis();
        long tickSize = 512;
        long maxTimeInterval = 2047 - tickSize;

        BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, tickSize, maxTimeInterval,
                64);
        int expired = timeout.pollTimeouts(startTime + 4096, (timeUnit, now, timerId) -> {

        });
        Assertions.assertEquals(0, expired);

        int LIMIT = 4096;
        long currentTime = timeout.getCurrentTime();
        for (int i = 1; i < LIMIT; i++) {
            final long deadline = currentTime + ((maxTimeInterval) * i) - tickSize;
            for (int j = 0; j < 65; j++) {
                final int timeOutId = timeout.scheduleTimeout(deadline);
                if (j == 64) {
                    //max timer threshold reached
                    Assertions.assertEquals(timeOutId, BitsetTimeWheel.ERR_CAPACITY_EXCEEDED);
                }
            }
            final long timeNow = deadline + tickSize;
            final int count = timeout.pollTimeouts(timeNow, ((timeUnit, now, timerId1) -> {
            }));
            Assertions.assertEquals(64, count);
        }

    }

    @Test
    public void testScheduleTimeoutDuringCallBack() {
        long startTime = System.currentTimeMillis();
        long tickSize = 512;
        long maxTimeInterval = 2047 - tickSize;
        BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, tickSize, maxTimeInterval,
                64);

        Assertions.assertEquals(BitsetTimeWheel.ERR_OUT_OF_RANGE, timeout.scheduleTimeout(startTime + 2048));
        Assertions.assertEquals(BitsetTimeWheel.ERR_OUT_OF_RANGE, timeout.scheduleTimeout(startTime + 2047));
        int timeoutId = timeout.scheduleTimeout(startTime + 2047 - tickSize);
        Assertions.assertTrue(0 <= timeoutId);

        timeout.pollTimeouts(startTime + 2048 - tickSize, (timeUnit, now, timerId) -> {
            Assertions.assertEquals(timeoutId, timeoutId);
            int newTimeout = timeout.scheduleTimeout(startTime + 2048);
            Assertions.assertTrue(newTimeout >= 0);

            int newTimeout2 = timeout.scheduleTimeout(startTime + 2048 + tickSize);
            Assertions.assertEquals(newTimeout2, BitsetTimeWheel.ERR_OUT_OF_RANGE);

        });

        int expired = timeout.pollTimeouts(startTime + 2048 + tickSize,  (timeUnit, now, timerId) -> {


        });
        Assertions.assertEquals(expired, 1);




    }

    @Test
    public void testManyTimeoutISingleTick() {
        long startTime = System.currentTimeMillis();
        long tickSize = 512;
        long maxTimeInterval = 2048;

        for (int maxTimerPerTick = 64; maxTimerPerTick < 512; maxTimerPerTick += 64) {

            BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, tickSize, maxTimeInterval,
                    maxTimerPerTick);
            int expired = timeout.pollTimeouts(startTime + 4096, (timeUnit, now, timerId) -> {

            });
            Assertions.assertEquals(0, expired);

            int LIMIT = 4096;
            long currentTime = timeout.getCurrentTime();
            for (int i = 1; i < LIMIT; i++) {
                final long deadline = currentTime + ((maxTimeInterval) * i) - tickSize;
                for (int j = 0; j < maxTimerPerTick; j++) {
                    timeout.scheduleTimeout(deadline);
                }
                final long timeNow = deadline + tickSize;
                final int count = timeout.pollTimeouts(timeNow, ((timeUnit, now, timerId1) -> {
                }));
                Assertions.assertEquals(maxTimerPerTick, count);
            }
        }
    }
        @Test
    public void testWrap() {
        long startTime = System.currentTimeMillis();
        long resolution = 512;
        long maxTimeInterval = 2048;
        BitsetTimeWheel timeout = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, resolution, maxTimeInterval,
                64);
        int expired = timeout.pollTimeouts(startTime + 4096, (timeUnit, now, timerId) -> {

        });
        Assertions.assertEquals(0, expired);
        Assertions.assertEquals(startTime + 4096, timeout.getCurrentTime());

        int LIMIT = 4096;
        long currentTime = timeout.getCurrentTime();
        for (int i = 1; i < LIMIT; i++) {
            int timerId = timeout.scheduleTimeout(currentTime + ((maxTimeInterval) * i) - resolution);
            long timeNow = currentTime + maxTimeInterval * i;
            int count = timeout.pollTimeouts(timeNow, ((timeUnit, now, timerId1) -> {
                Assertions.assertEquals(timerId, timerId1);
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
        int timeoutId = timeout.scheduleTimeout(startTime + 1 );
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
        int timeoutId = timeout.scheduleTimeout(startTime + 1);

        Assertions.assertEquals(0, timeout.pollTimeouts(startTime + 30, (timeUnit, now, timerId) -> {
                }
        ));

        Assertions.assertEquals(1, timeout.pollTimeouts(startTime + 32, (timeUnit, now, timerId) -> {
            Assertions.assertEquals(timeoutId, timerId);
        }));
    }
}
