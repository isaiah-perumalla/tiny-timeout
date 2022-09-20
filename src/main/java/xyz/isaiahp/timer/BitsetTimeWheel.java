package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public class BitsetTimeWheel implements TimeOut {
    private static final long EMPTY_BITSET = 0L;
    private static final long FULL_BITSET = -1L;
    public static final byte ERR_OUT_OF_RANGE = -2;
    public static final byte ERR_EXPIRED = -3;
    public static final byte ERR_CAPACITY_EXCEEDED = -4;

    private final TimeUnit timeUnit;

    /**
     * reference start time in @timeUnit
     * @currentTick is relative to this startTime
     */
    private final long startTime;

    /**
     * granularity of each tick on wheel
     * must be power of 2
     */
    private final int tickGranularityBits;
    private final long maxTimeoutDuration;
    private int activeTimers;


    private int timerPerTick = 64;
    private long[] timerWheel;
    /**
     * monotonically increasing
     * approx currentTime = startTime + (currentTick * granularity)
     */
    private long currentTick;



    public BitsetTimeWheel(TimeUnit timeUnit, long startTime, long tickGranularity, long requestedMaxTimeoutDuration, int timerPerTick) {

        checkPowerOf2(tickGranularity, "tickGranularity");
        this.timeUnit = timeUnit;
        this.startTime = startTime;
        this.currentTick = 0;
        this.tickGranularityBits = Long.numberOfTrailingZeros(tickGranularity);
        this.maxTimeoutDuration = getRequiredMaxDuration(requestedMaxTimeoutDuration, (int) tickGranularity);
        checkPowerOf2(this.maxTimeoutDuration, "requestedMaxTimeoutDuration");
        final int ticks = (int) (this.maxTimeoutDuration >> tickGranularityBits);
        checkPowerOf2(ticks, "ticks");
        this.timerPerTick = timerPerTick;
        final int longPerBucket = timerPerTick /(Long.SIZE);
        timerWheel = new long[ticks * longPerBucket];
        this.activeTimers = 0;

    }

    private static long getRequiredMaxDuration(long requestedDuration, int granularity) {
        int required = (int) (requestedDuration + 1 + granularity);
        return nextPowerOf2(required);
    }

    private static int nextPowerOf2(int value) {
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
    }

    public static void checkPowerOf2(long value, String errorMsg) {
        if (!isPowerOf2(value) ) {
            throw new IllegalArgumentException(errorMsg +" not power of two" + value);
        }
    }


    private static boolean isPowerOf2(long value) {
        return value > 0 && ((value & (~value + 1)) == value);
    }

    @Override
    public int scheduleTimeout(long deadline) {
        if (startTime == deadline) return ERR_EXPIRED; //already expired
        final int maxRange = (int) (maxTimeoutDuration >> tickGranularityBits) - 1;
        final long deadLineBucket = (deadline - startTime) >> tickGranularityBits;
        if (deadLineBucket < 0) { //in past or expired
            return ERR_EXPIRED;
        }
        if ((deadLineBucket - currentTick) >= maxRange) {
            return ERR_OUT_OF_RANGE;
        }

        final int bucketIndex = (int) (deadLineBucket & bucketMask());
        final int longPerBucket = longPerBucket(timerPerTick);
        final int startIndex = bucketIndex * longPerBucket;

        for (int i = 0; i < longPerBucket; i++) {
            final int index = startIndex + i;
            long timeBitSet = timerWheel[index];
            short bitIndex = freeBitIndex(timeBitSet);
            if (bitIndex < 0) {
                continue;
            }
            timerWheel[index] = timeBitSet | (1L << bitIndex);
            final int timerId = getTimerId(index, bitIndex);
            activeTimers++;
            return timerId;
        }
        return ERR_CAPACITY_EXCEEDED;
    }

    private int getTimerId(int index, short bitIndex) {
        return index * Long.SIZE + bitIndex;
    }


    private static int longPerBucket(int timerPerTick) {
        return timerPerTick >> 6;
    }

    private int bucketMask() {
        int numberOfBuckets = (int) (maxTimeoutDuration >> tickGranularityBits);
        return numberOfBuckets -1;
    }


    private static short freeBitIndex(long bitSet) {
        if (bitSet == EMPTY_BITSET) return 0;
        if (bitSet == FULL_BITSET) return -1;
        for (short j = 0; j < Long.SIZE; j++) {
            if (0 == (bitSet & (1L << j))) {
                return j;
            }
        }
        return -1;
    }

    public int pollTimeouts(final long now, final TimeOut.Handler handler)
    {
        assert now >= startTime;
        final long nowBucketId = (now - startTime) >> tickGranularityBits;
        if (nowBucketId < 0 || nowBucketId < currentTick) return 0;
        int expiredCount = 0;
        if (activeTimers == 0) {
            currentTick = nowBucketId;
            return 0;
        }
        final int longPerBucket = longPerBucket(timerPerTick);
        for (long i = currentTick; i < nowBucketId ; i++) {

            final int index = (int) (i & bucketMask()) * longPerBucket;
            expiredCount += expireTimersAt(index, handler, now);
            currentTick += 1;
        }

        return expiredCount;
    }

    private int expireTimersAt(int bucketIndex, Handler handler, long now) {
        int count = 0;
        int longPerBucket = longPerBucket(timerPerTick);
        for (int i = 0; i < longPerBucket; i++) {
            final int index = bucketIndex + i;
            long bitSet = timerWheel[index];
            if (bitSet == EMPTY_BITSET) continue;
            for (short j = 0; bitSet != EMPTY_BITSET && j < Long.SIZE; j++) {
                long bitMask = (1L << j);
                if ((bitSet & bitMask) != 0L) {
                    timerWheel[index] &= ~bitMask; //clear the bit to cancel timeout
                    bitSet &= ~bitMask;
                    int timerId = getTimerId(index, j);
                    activeTimers--;
                    handler.onTimeout(timeUnit, now, timerId);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public boolean cancelTimer(int timeoutId) {
        if (timeoutId < 0) {
            return false;
        }
        if (clearBit(timeoutId, timerWheel)) {
            activeTimers--;
            return true;
        }
        return false;
    }

    private static boolean clearBit(int timeoutId, long[] timerWheel) {
        int bitSlot = timeoutId / Long.SIZE;
        int bitMask = timeoutId & (Long.SIZE-1);
        long set = timerWheel[bitSlot];
        boolean isSet = (set & (1 << bitMask)) != 0;
        assert isSet : "timeoutId not set";
        if (isSet) {
            timerWheel[bitSlot] = set & ~(1L << bitMask);
            return true;
        }
        return false;
    }


    public long getCurrentTime() {
        return startTime + (currentTick << tickGranularityBits);
    }

    public int count() {
        return activeTimers;
    }
}
