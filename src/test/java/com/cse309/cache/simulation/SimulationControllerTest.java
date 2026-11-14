package com.cse309.cache.simulation;

import com.cse309.cache.core.StringCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.InvalidCacheOperationException;
import com.cse309.cache.history.OperationHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationControllerTest {

    @Test
    void testNormalSimulationRunCompletion() throws Exception {
        StringCache cache = new StringCache(50, EvictionPolicyType.LRU);
        OperationHistory history = new OperationHistory(100);
        SimulationController controller = new SimulationController(cache, history);

        SimulationConfig config = new SimulationConfig(
                4, 500, OperationMix.MIXED, KeyDistribution.HOTSPOT_80_20, 100, 64, 42L, false, true
        );

        controller.start(config);
        assertTrue(controller.isActive());

        // Wait for completion (up to 5s)
        long deadline = System.currentTimeMillis() + 5000;
        SimulationProgress progress = controller.getProgress();
        while (progress.getState() == SimulationProgress.State.RUNNING && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            progress = controller.getProgress();
        }

        assertEquals(SimulationProgress.State.COMPLETED, progress.getState());
        assertEquals(500, progress.getCompleted());
        assertEquals(500, progress.getGenerated());
        assertTrue(cache.size() <= 50, "Cache size must never exceed capacity");
    }

    @Test
    void testSecondStartThrows() throws Exception {
        StringCache cache = new StringCache(50, EvictionPolicyType.LRU);
        SimulationController controller = new SimulationController(cache, null);

        SimulationConfig config = new SimulationConfig(
                2, 10000, OperationMix.READ_HEAVY, KeyDistribution.UNIFORM, 100, 64, null, false, false
        );

        controller.start(config);
        assertTrue(controller.isActive());

        assertThrows(InvalidCacheOperationException.class, () -> controller.start(config));

        controller.stop();
        assertFalse(controller.isActive());
    }

    @Test
    void testPauseResumeAndStop() throws Exception {
        StringCache cache = new StringCache(50, EvictionPolicyType.LRU);
        SimulationController controller = new SimulationController(cache, null);

        SimulationConfig config = new SimulationConfig(
                4, 50000, OperationMix.MIXED, KeyDistribution.UNIFORM, 500, 128, null, false, false
        );

        controller.start(config);
        Thread.sleep(100);

        controller.pause();
        assertEquals(SimulationProgress.State.PAUSED, controller.getProgress().getState());
        int completedAtPause = controller.getProgress().getCompleted();

        Thread.sleep(100);
        // While paused, completed count should remain largely stationary
        int completedDuringPause = controller.getProgress().getCompleted();
        assertTrue(Math.abs(completedDuringPause - completedAtPause) <= 4, "Workers should be paused");

        controller.resume();
        assertEquals(SimulationProgress.State.RUNNING, controller.getProgress().getState());

        Thread.sleep(100);
        controller.stop();
        assertFalse(controller.isActive());

        // Cache invariants must remain intact
        assertTrue(cache.size() <= 50);
    }
}
