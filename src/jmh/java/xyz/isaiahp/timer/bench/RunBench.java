package xyz.isaiahp.timer.bench;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class RunBench {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(VolatileBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
