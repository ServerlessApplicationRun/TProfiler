/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.test;

import org.junit.Before;
import org.junit.Test;

import com.taobao.profile.Manager;
import com.taobao.profile.Profiler;

public class ProfilerBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 10000;
    private static final int BENCHMARK_ITERATIONS = 1000000;

    @Before
    public void setUp() {
        Manager.instance().setProfileFlag(true);
    }

    @Test
    public void benchmarkStartEnd() {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Profiler.Start(1);
            Profiler.End(1);
        }

        Profiler.clearData();

        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Profiler.Start(1);
            Profiler.End(1);
        }
        long endTime = System.nanoTime();

        double totalMs = (endTime - startTime) / 1000000.0;
        double avgNs = (endTime - startTime) / (double) BENCHMARK_ITERATIONS;

        System.out.println("=== Profiler.Start/End Benchmark ===");
        System.out.println("Iterations: " + BENCHMARK_ITERATIONS);
        System.out.println("Total time: " + String.format("%.2f", totalMs) + " ms");
        System.out.println("Avg per call: " + String.format("%.2f", avgNs) + " ns");
        System.out.println("Throughput: " + String.format("%.0f", BENCHMARK_ITERATIONS / (totalMs / 1000.0)) + " ops/sec");
    }

    @Test
    public void benchmarkStartEndWithProfileDisabled() {
        Manager.instance().setProfileFlag(false);

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Profiler.Start(1);
            Profiler.End(1);
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Profiler.Start(1);
            Profiler.End(1);
        }
        long endTime = System.nanoTime();

        double totalMs = (endTime - startTime) / 1000000.0;
        double avgNs = (endTime - startTime) / (double) BENCHMARK_ITERATIONS;

        System.out.println("=== Profiler Disabled Benchmark ===");
        System.out.println("Iterations: " + BENCHMARK_ITERATIONS);
        System.out.println("Total time: " + String.format("%.2f", totalMs) + " ms");
        System.out.println("Avg per call: " + String.format("%.2f", avgNs) + " ns");
        System.out.println("Throughput: " + String.format("%.0f", BENCHMARK_ITERATIONS / (totalMs / 1000.0)) + " ops/sec");
    }

    @Test
    public void benchmarkMultiThreaded() throws InterruptedException {
        final int threadCount = 4;
        final int iterationsPerThread = BENCHMARK_ITERATIONS / threadCount;

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Profiler.Start(1);
            Profiler.End(1);
        }

        Profiler.clearData();

        Thread[] threads = new Thread[threadCount];
        final long[] threadTimes = new long[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            threads[t] = new Thread(new Runnable() {
                public void run() {
                    long start = System.nanoTime();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        Profiler.Start(threadIndex);
                        Profiler.End(threadIndex);
                    }
                    threadTimes[threadIndex] = System.nanoTime() - start;
                }
            });
        }

        long startTime = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long endTime = System.nanoTime();

        double totalMs = (endTime - startTime) / 1000000.0;
        double avgNs = (endTime - startTime) / (double) BENCHMARK_ITERATIONS;

        System.out.println("=== Multi-threaded Benchmark (" + threadCount + " threads) ===");
        System.out.println("Total iterations: " + BENCHMARK_ITERATIONS);
        System.out.println("Total wall time: " + String.format("%.2f", totalMs) + " ms");
        System.out.println("Avg per call: " + String.format("%.2f", avgNs) + " ns");
        System.out.println("Throughput: " + String.format("%.0f", BENCHMARK_ITERATIONS / (totalMs / 1000.0)) + " ops/sec");
    }
}