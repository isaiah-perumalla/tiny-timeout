package xyz.isaiahp.timer.bench;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@Fork(value = 3, jvmArgsPrepend = "-disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class TimeoutBenchmark {
}
