package com.cse309.cache.benchmark;

import com.cse309.cache.simulation.KeyDistribution;
import com.cse309.cache.simulation.OperationMix;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkRunnerTest {

    @Test
    void testPolicyComparisonWithSameSeedGivesIdenticalResults() {
        long seed = 12345L;
        int capacity = 20;
        int totalOps = 500;
        int keySpace = 50;

        // Run comparison twice with identical seed
        List<BenchmarkResult> run1 = BenchmarkRunner.runPolicyComparison(
                capacity, totalOps, keySpace, KeyDistribution.HOTSPOT_80_20, OperationMix.MIXED, seed
        );

        List<BenchmarkResult> run2 = BenchmarkRunner.runPolicyComparison(
                capacity, totalOps, keySpace, KeyDistribution.HOTSPOT_80_20, OperationMix.MIXED, seed
        );

        assertEquals(3, run1.size());
        assertEquals(3, run2.size());

        for (int i = 0; i < 3; i++) {
            BenchmarkResult r1 = run1.get(i);
            BenchmarkResult r2 = run2.get(i);

            assertEquals(r1.getEvictionPolicy(), r2.getEvictionPolicy());
            assertEquals(r1.getHitRatio(), r2.getHitRatio(), 0.0001,
                    "Identical seed must yield deterministic single-threaded hit ratio for " + r1.getEvictionPolicy());
            assertTrue(r1.getOpsPerSecond() > 0);
            assertNotNull(r1.getRunMatrix());
            assertEquals(3, r1.getRunMatrix().length);
        }
    }
}
