package xyz.isaiahp.timer;

public class BinaryHeapTimer implements TimeOut {
    private final long[] deadlines;
    private final int[] timerIds;
    private int nextTimeId = 1;
    
    public BinaryHeapTimer(long startTime, int initialCapacity) {
        deadlines = new long[initialCapacity];
        timerIds = new int[initialCapacity];
    }

    public int scheduleTimeout(long deadline) {
        return 0;
    }

    @Override
    public boolean cancelTimer(int timeoutId) {
        return false;
    }

    @Override
    public int pollTimeouts(long now, Handler handler) {
        return 0;
    }

}
