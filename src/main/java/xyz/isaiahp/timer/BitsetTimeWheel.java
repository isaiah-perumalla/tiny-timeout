package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public class BitsetTimeWheel implements TimeOut {
    private static final long EMPTY_SET = 0L;
    public static byte ERR_OUT_OF_RANGE = -1;
    private final TimeUnit timeUnit;

    private long startTime;

    private final int resolutionBits;
    private final long maxTimeoutDuration;

    private final long[] timerWheel;
    private int timersPerBucket = 64;

    private long currentBucket;
    private int ERR_EXPIRED = -2;



    public BitsetTimeWheel(TimeUnit timeUnit, long startTime, long resolution, long maxTimeInterval, int timersPerBucket) {

        checkPowerOf2(resolution, "resolution");
        checkPowerOf2(maxTimeInterval, "maxDuration");
        this.timeUnit = timeUnit;
        this.startTime = startTime;
        this.currentBucket = 0;
        this.resolutionBits = Long.numberOfTrailingZeros(resolution);
        this.maxTimeoutDuration = maxTimeInterval;
        final int buckets = (int) (maxTimeInterval >> resolutionBits);
        this.timersPerBucket = timersPerBucket;
        final int longPerBucket = timersPerBucket/(Long.SIZE);
        timerWheel = new long[buckets * longPerBucket];
        checkPowerOf2(timerWheel.length, "wheel.length");
    }

    public static void checkPowerOf2(long value, String errorMsg) {
        if (!isPowerOf2(value) ) {
            throw new IllegalArgumentException(errorMsg +" not power of two" + value);
        }
    }

    /*
     0 1 2 3
     */
    private static boolean isPowerOf2(long value) {
        return value > 0 && ((value & (~value + 1)) == value);
    }

    @Override
    public int scheduleTimer(long deadline) {
        if (startTime == deadline) return ERR_EXPIRED; //already expired
        final int maxRange = (int) (maxTimeoutDuration >> resolutionBits);
        final long deadLineBucket = (deadline - startTime) >> resolutionBits;
        if (deadLineBucket < 0) { //in past or expired
            return ERR_EXPIRED;
        }
        if ((deadLineBucket - currentBucket) >= maxRange) {
            return ERR_OUT_OF_RANGE;
        }

        final int bucketIndex = (int) (deadLineBucket & bucketMask());
        final int startIndex = bucketIndex * longPerBucket();


        for (int i = 0; i < longPerBucket(); i++) {

            final int index = startIndex + i;
            long timeBitSet = timerWheel[index];
            short bitIndex = freeBitIndex(timerWheel, index);
            if (bitIndex < 0) {
                continue;
            }
            timerWheel[index] = timeBitSet | (1L << bitIndex);
            final int timerId = getTimerId(index, bitIndex);
            return timerId;
        }
        return -1;
    }

    private int getTimerId(int index, short bitIndex) {
        return index * Long.SIZE + bitIndex;
    }


    private int longPerBucket() {
        return timersPerBucket / Long.SIZE;
    }

    private int bucketMask() {
        int numberOfBuckets = (int) (maxTimeoutDuration >> resolutionBits);
        return numberOfBuckets -1;
    }

    private int getMaxBuckets() {
        return (int) (maxTimeoutDuration >> resolutionBits);
    }

    private static short freeBitIndex(long[] timerWheel, int i) {
        long bitSet = timerWheel[i];
        if (bitSet == 0) return 0;
        //full
        if (bitSet == -1) return -1;
        for (short j = 0; j < Long.SIZE; j++) {
            if (0 == (bitSet & (1 << j))) {
                return j;
            }
        }
        return -1;
    }

    public int pollTimeouts(final long now, final TimeOut.Handler handler)
    {
        assert now >= startTime;
        final long nowBucketId = (now - startTime) >> resolutionBits;
        if (nowBucketId < 0 || nowBucketId < currentBucket) return 0;
        final int resolution = 1 << resolutionBits;
        int expiredCount = 0;
        for (long i = currentBucket; i < nowBucketId ; i++) {
            final int index = (int) (i & bucketMask());
            expiredCount += expireTimersAt(index, handler, now);
            currentBucket += 1;
        }

        return expiredCount;
    }

    private int expireTimersAt(int bucketIndex, Handler handler, long now) {
        int count = 0;
        for (int i = 0; i < longPerBucket(); i++) {
            final int index = bucketIndex + i;
            long bitSet = timerWheel[index];
            if (bitSet == EMPTY_SET) continue;
            for (short j = 0; bitSet != EMPTY_SET && j < Long.SIZE; j++) {
                int bitMask = (1 << j);
                if ((bitSet & bitMask) != 0L) {
                    timerWheel[index] &= ~bitMask; //clear the bit to cancel timeout
                    bitSet &= ~bitMask;
                    long timerId = getTimerId(index, j);
                    handler.onTimeout(timeUnit, now, timerId);
                    count++;
                }
            }
        }
        return count;
    }

    public void advanceCurrentTick(long now) {

    }


    @Override
    public boolean cancelTimer(int timeoutId) {
        clearBit(timeoutId, timerWheel);
        return true;
    }

    private void clearBit(int timeoutId, long[] timerWheel) {
        int bitSlot = timeoutId / Long.SIZE;
        int bitMask = timeoutId & (Long.SIZE-1);
        long set = timerWheel[bitSlot];
        assert (set & (1 << bitMask)) != 0 : "timeoutId not set";
        timerWheel[bitSlot] = set & ~(1 << bitMask);
    }


    public long getCurrentTime() {
        return startTime + (currentBucket << resolutionBits);
    }
}
