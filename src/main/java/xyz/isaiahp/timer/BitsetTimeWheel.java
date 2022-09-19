package xyz.isaiahp.timer;

import java.util.concurrent.TimeUnit;

public class BitsetTimeWheel implements TimeOut {
    private static final long EMPTY_BITSET = 0L;
    private static final long FULL_BITSET = -1L;
    public static final byte ERR_OUT_OF_RANGE = -2;
    public static final byte ERR_EXPIRED = -3;
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
        final int buckets = (int) (this.maxTimeoutDuration >> tickGranularityBits);
        this.timerPerTick = timerPerTick;
        final int longPerBucket = timerPerTick /(Long.SIZE);
        timerWheel = new long[buckets * longPerBucket];
        checkPowerOf2(timerWheel.length, "wheel.length");
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
        return timerPerTick / Long.SIZE;
    }

    private int bucketMask() {
        int numberOfBuckets = (int) (maxTimeoutDuration >> tickGranularityBits);
        return numberOfBuckets -1;
    }


    private static short freeBitIndex(long[] timerWheel, int i) {
        long bitSet = timerWheel[i];
        if (bitSet == 0) return 0;
        //full
        if (bitSet == FULL_BITSET) return -1;
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
        final long nowBucketId = (now - startTime) >> tickGranularityBits;
        if (nowBucketId < 0 || nowBucketId < currentTick) return 0;
        final int resolution = 1 << tickGranularityBits;
        int expiredCount = 0;
        for (long i = currentTick; i < nowBucketId ; i++) {
            final int index = (int) (i & bucketMask());
            expiredCount += expireTimersAt(index, handler, now);
            currentTick += 1;
        }

        return expiredCount;
    }

    private int expireTimersAt(int bucketIndex, Handler handler, long now) {
        int count = 0;
        for (int i = 0; i < longPerBucket(); i++) {
            final int index = bucketIndex + i;
            long bitSet = timerWheel[index];
            if (bitSet == EMPTY_BITSET) continue;
            for (short j = 0; bitSet != EMPTY_BITSET && j < Long.SIZE; j++) {
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
        return startTime + (currentTick << tickGranularityBits);
    }
}