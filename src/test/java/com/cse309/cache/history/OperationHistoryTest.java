package com.cse309.cache.history;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationHistoryTest {

    @Test
    void testBoundedCapacityDropsOldest() {
        OperationHistory history = new OperationHistory(100); // minimum capacity is 100
        for (int i = 1; i <= 105; i++) {
            history.record(new OperationRecord(i, "thread-1", "PUT", "k" + i, "SUCCESS", 100, "details"));
        }

        assertEquals(100, history.size());
        List<OperationRecord> records = history.getRecentRecords();
        assertEquals("k6", records.get(0).getCacheKey()); // First 5 dropped
        assertEquals("k105", records.get(records.size() - 1).getCacheKey());
    }

    @Test
    void testFilterByOperation() {
        OperationHistory history = new OperationHistory(100);
        history.record(OperationRecord.create("PUT", "k1", "INSERTED", 100, ""));
        history.record(OperationRecord.create("GET", "k1", "HIT", 50, ""));
        history.record(OperationRecord.create("PUT", "k2", "INSERTED", 120, ""));

        List<OperationRecord> puts = history.getFilteredRecords("PUT");
        assertEquals(2, puts.size());

        List<OperationRecord> gets = history.getFilteredRecords("GET");
        assertEquals(1, gets.size());
    }

    @Test
    void testCsvExport() throws Exception {
        OperationHistory history = new OperationHistory(100);
        history.record(OperationRecord.create("GET", "sampleKey", "HIT", 42000, "len=5"));

        StringWriter writer = new StringWriter();
        history.exportCsv(writer);
        String csv = writer.toString();

        assertTrue(csv.contains("Timestamp,Thread,Operation,Key,Outcome,LatencyNanos,Details"));
        assertTrue(csv.contains("sampleKey"));
        assertTrue(csv.contains("HIT"));
    }
}
