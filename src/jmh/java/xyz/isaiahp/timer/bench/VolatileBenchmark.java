package xyz.isaiahp.timer.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import xyz.isaiahp.timer.FixedTimeOut;
import xyz.isaiahp.timer.TimeOut;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)

public class VolatileBenchmark {
    int v;
    volatile int volatileInt;

    @Benchmark
    public int baseLine() {
        return 128;
    }

    @Benchmark
    public int intAdd() {
        return v++;
    }

    @Benchmark
    public int volatileIntAdd() {
        FixedTimeOut.checkPowerOf2(2);
        return volatileInt++;
    }

}
