package com.cse309.cache.lab;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionsLabTest {

    @Test
    void testCollectionsComparisonRuns() {
        List<CollectionsLab.BenchmarkEntry> entries = CollectionsLab.runComparisons(500);
        assertFalse(entries.isEmpty());
        assertTrue(entries.size() >= 8); // 3 Lists, 4 Maps, 2 Sets
    }

    @Test
    void testFailFastCmeDemonstration() {
        assertTrue(CollectionsLab.demonstrateFailFastCme(),
                "Direct mutation during foreach iteration must throw ConcurrentModificationException");
    }

    @Test
    void testSafeIteratorRemoveDemonstration() {
        assertTrue(CollectionsLab.demonstrateSafeIteratorRemove(),
                "Structural modification via Iterator.remove() must succeed cleanly");
    }
}
