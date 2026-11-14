package com.cse309.cache.benchmark;

import com.cse309.cache.core.ThreadSafeCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.simulation.KeyDistribution;
import com.cse309.cache.simulation.OperationGenerator;
import com.cse309.cache.simulation.OperationMix;
import com.cse309.cache.simulation.PlannedOperation;
import com.cse309.cache.simulation.SimulationConfig;
import com.cse309.cache.logging.AppLogging;
import com.cse309.cache.stats.StatisticsSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deterministic benchmark runner executing repeatable workload runs against isolated caches.
 * Uses 2-D double[][] matrix to store repetition metrics (Unit 1).
 */
public final class BenchmarkRunner {

    private static final Logger LOGGER = AppLogging.getLogger(BenchmarkRunner.class);

    private BenchmarkRunner() {
        // Utility class
    }

    /**
     * Executes a policy benchmark comparing LRU, FIFO, and LFU using an identical
     * generated workload sequence.
     */
    public static List<BenchmarkResult> runPolicyComparison(int capacity,
                                                            int totalOps,
                                                            int keySpace,
                                                            KeyDistribution distribution,
                                                            OperationMix mix,
                                                            long seed) {
        List<BenchmarkResult> results = new ArrayList<>();
        EvictionPolicyType[] policies = {EvictionPolicyType.LRU, EvictionPolicyType.FIFO, EvictionPolicyType.LFU};

        // 1. Generate planned operations once using seed
        SimulationConfig simConfig = new SimulationConfig(1, totalOps, mix, distribution, keySpace, 256, seed, false, false);
        OperationGenerator generator = new OperationGenerator(simConfig);
        List<PlannedOperation> plan = new ArrayList<>(totalOps);
        for (int i = 1; i <= totalOps; i++) {
            plan.add(generator.nextOperation(i));
        }

        for (EvictionPolicyType policy : policies) {
            results.add(executeSingleThreadedRun(policy, capacity, plan, distribution.name(), mix.toString()));
        }

        return results;
    }

    private static BenchmarkResult executeSingleThreadedRun(EvictionPolicyType policy,
                                                            int capacity,
                                                            List<PlannedOperation> plan,
                                                            String distributionName,
                                                            String mixName) {
        // Warmup pass (discarded)
        executePlanSingleThreaded(policy, capacity, plan);

        // 3 measured repetitions
        final int repetitions = 3;
        double[][] runMatrix = new double[repetitions][3];
        double[] opsPerSecRuns = new double[repetitions];
        double finalHitRatio = 0.0;
        int finalEvictions = 0;

        for (int rep = 0; rep < repetitions; rep++) {
            ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(capacity, policy);
            long startNanos = System.nanoTime();

            executePlan(cache, plan);

            long durationNanos = Math.max(1L, System.nanoTime() - startNanos);
            StatisticsSnapshot stats = cache.getStatistics().snapshot(cache.size(), cache.getCapacity());

            double opsPerSec = (double) plan.size() / (durationNanos / 1_000_000_000.0);
            runMatrix[rep][0] = opsPerSec;
            runMatrix[rep][1] = stats.getHitRatio();
            runMatrix[rep][2] = durationNanos;
            opsPerSecRuns[rep] = opsPerSec;

            finalHitRatio = stats.getHitRatio();
            finalEvictions = (int) stats.getEvictions();
        }

        Arrays.sort(opsPerSecRuns);
        double medianOpsPerSec = opsPerSecRuns[1]; // Median of 3
        long medianDuration = (long) runMatrix[1][2];

        return new BenchmarkResult(
                null,
                System.currentTimeMillis(),
                policy.name(),
                capacity,
                1,
                distributionName,
                mixName,
                plan.size(),
                medianDuration,
                medianOpsPerSec,
                finalHitRatio,
                finalEvictions,
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                Runtime.getRuntime().availableProcessors(),
                runMatrix
        );
    }

    public static BenchmarkResult runMultiThreadedThroughput(EvictionPolicyType policy,
                                                             int capacity,
                                                             int threads,
                                                             int totalOps,
                                                             int keySpace,
                                                             KeyDistribution distribution,
                                                             OperationMix mix,
                                                             Long seed) {
        // Multi-threaded throughput measurement
        final int repetitions = 3;
        double[][] runMatrix = new double[repetitions][3];
        double[] throughputRuns = new double[repetitions];
        double lastHitRatio = 0.0;
        int lastEvictions = 0;

        for (int rep = 0; rep < repetitions; rep++) {
            ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(capacity, policy);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threads);
            int opsPerThread = totalOps / threads;

            long startNanos = System.nanoTime();

            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        long threadSeed = seed != null ? seed + threadId * 31L : System.nanoTime() + threadId;
                        SimulationConfig cfg = new SimulationConfig(1, opsPerThread, mix, distribution, keySpace, 128, threadSeed, false, false);
                        OperationGenerator gen = new OperationGenerator(cfg);

                        for (int i = 1; i <= opsPerThread; i++) {
                            PlannedOperation op = gen.nextOperation(i);
                            executeSingleOp(cache, op);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            try {
                finishLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long durationNanos = Math.max(1L, System.nanoTime() - startNanos);
            StatisticsSnapshot stats = cache.getStatistics().snapshot(cache.size(), cache.getCapacity());

            double opsPerSec = (double) totalOps / (durationNanos / 1_000_000_000.0);
            runMatrix[rep][0] = opsPerSec;
            runMatrix[rep][1] = stats.getHitRatio();
            runMatrix[rep][2] = durationNanos;
            throughputRuns[rep] = opsPerSec;

            lastHitRatio = stats.getHitRatio();
            lastEvictions = (int) stats.getEvictions();
        }

        Arrays.sort(throughputRuns);
        double medianOpsPerSec = throughputRuns[1];
        long medianDuration = (long) runMatrix[1][2];

        return new BenchmarkResult(
                null,
                System.currentTimeMillis(),
                policy.name(),
                capacity,
                threads,
                distribution.name(),
                mix.toString(),
                totalOps,
                medianDuration,
                medianOpsPerSec,
                lastHitRatio,
                lastEvictions,
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                Runtime.getRuntime().availableProcessors(),
                runMatrix
        );
    }

    private static void executePlanSingleThreaded(EvictionPolicyType policy, int capacity, List<PlannedOperation> plan) {
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(capacity, policy);
        executePlan(cache, plan);
    }

    private static void executePlan(ThreadSafeCache<String, String> cache, List<PlannedOperation> plan) {
        for (PlannedOperation op : plan) {
            executeSingleOp(cache, op);
        }
    }

    private static void executeSingleOp(ThreadSafeCache<String, String> cache, PlannedOperation op) {
        try {
            switch (op.getType()) {
                case GET -> cache.get(op.getKey());
                case PUT -> {
                    if (op.getTtl() != null) cache.put(op.getKey(), op.getValue(), op.getTtl());
                    else cache.put(op.getKey(), op.getValue());
                }
                case UPDATE -> {
                    try {
                        cache.update(op.getKey(), op.getValue());
                    } catch (Exception e) {
                        LOGGER.log(Level.FINEST, "Benchmark update key missed: " + op.getKey());
                    }
                }
                case DELETE -> cache.remove(op.getKey());
                case SEARCH -> cache.search(k -> k.equals(op.getKey()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINEST, "Benchmark operation expected failure: " + e.getMessage());
        }
    }
}
