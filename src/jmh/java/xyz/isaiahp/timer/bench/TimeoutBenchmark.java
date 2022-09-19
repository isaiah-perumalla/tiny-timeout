package xyz.isaiahp.timer.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import xyz.isaiahp.timer.BinaryHeapTimer;
import xyz.isaiahp.timer.BitsetTimeWheel;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)

public class TimeoutBenchmark {

    @Benchmark
    public long bitsetTimerScheduleAndCancel(TestCase testCase, Blackhole bh) {

        long deadline = testCase.nextDeadline();
        BitsetTimeWheel timer = testCase.bitsetTimeWheel;
        int timeoutId = timer.scheduleTimeout(deadline);
        Blackhole.consumeCPU(10);
        timer.cancelTimer(timeoutId);
        return timeoutId;
    }

    @Benchmark
    public long binHeapTimerScheduleAndCancel(TestCase testCase, Blackhole bh) {

        long deadline = testCase.nextDeadline();
        BinaryHeapTimer timer = testCase.binaryHeapTimer;
        int timeoutId = timer.scheduleTimeout(deadline);
        Blackhole.consumeCPU(10);
        timer.cancelTimer(timeoutId);
        return timeoutId;
    }

    @State(Scope.Benchmark)
    private static class TestCase {
        @Param({"10000", "100000", "200000"})
        private int size;

        @Param({"1024", "4096"})
        private long maxTimeoutMillis;

        private BitsetTimeWheel bitsetTimeWheel;
        private BinaryHeapTimer binaryHeapTimer;
        private long resolutionMillis = 16;
        private final Random random = new Random();
        private long[] timeouts;
        private int index;

        @Setup(Level.Iteration)
        public void setup() {
            long startTime = System.currentTimeMillis();
            bitsetTimeWheel = new BitsetTimeWheel(TimeUnit.MILLISECONDS, startTime, resolutionMillis, maxTimeoutMillis, 64);
            binaryHeapTimer = new BinaryHeapTimer(startTime,32);
            timeouts = new long[size];
            index = 0;
            for (int i = 0; i < size; i++) {
                int timeoutMillis = (int) (resolutionMillis + random.nextInt((int) maxTimeoutMillis));
                timeouts[i] = timeoutMillis;
            }
        }

        public long nextDeadline() {
            return System.currentTimeMillis() + timeouts[index++];
        }
    }
}
