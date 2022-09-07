package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public class BitsetTimeWheel implements TimeOut {
    private final TimeUnit timeUnit;
    private long startTime;
    private final long resolution;
    private final long maxTimeInterval;

    public BitsetTimeWheel(TimeUnit timeUnit, long startTime, long resolution, long maxTimeInterval) {

        checkPowerOf2(resolution);
        checkPowerOf2(maxTimeInterval);
        this.timeUnit = timeUnit;
        this.startTime = startTime;
        this.resolution = resolution;
        this.maxTimeInterval = maxTimeInterval;
    }

    public static void checkPowerOf2(long value) {
        if (!isPowerOf2(value) ) {
            throw new IllegalArgumentException("not a power of 2 " + value);
        }
    }

    private static boolean isPowerOf2(long value) {
        return value > 0 && ((value & (~value + 1)) == value);
    }

    public int schedule(long deadline) {
        return 0;
    }

    public int pollTimeouts(final long now, final TimeOut.Handler handler, final int limit)
    {
        return 0;
    }

    public void advanceCurrentTick(long now) {

    }

    @Override
    public int scheduleTimer(long deadline) {
        return 0;
    }

    @Override
    public boolean cancelTimer(int timeoutId) {
        return false;
    }
}
