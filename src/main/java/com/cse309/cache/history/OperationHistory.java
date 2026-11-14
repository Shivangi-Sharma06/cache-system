package com.cse309.cache.history;

import com.cse309.cache.core.CacheDefaults;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Thread-safe bounded operation history log.
 * Backed by a LinkedList (Unit 4) with addLast/removeFirst under a private lock.
 */
public final class OperationHistory {

    private final Object lock = new Object();
    private final LinkedList<OperationRecord> records;
    private int capacity;

    public OperationHistory(int capacity) {
        if (capacity < CacheDefaults.MIN_HISTORY_CAPACITY || capacity > CacheDefaults.MAX_HISTORY_CAPACITY) {
            this.capacity = CacheDefaults.DEFAULT_HISTORY_CAPACITY;
        } else {
            this.capacity = capacity;
        }
        this.records = new LinkedList<>();
    }

    public OperationHistory() {
        this(CacheDefaults.DEFAULT_HISTORY_CAPACITY);
    }

    public void record(OperationRecord record) {
        if (record == null) return;
        synchronized (lock) {
            while (records.size() >= capacity) {
                records.removeFirst();
            }
            records.addLast(record);
        }
    }

    public List<OperationRecord> getRecentRecords() {
        synchronized (lock) {
            return new ArrayList<>(records);
        }
    }

    public List<OperationRecord> getFilteredRecords(String operationFilter) {
        synchronized (lock) {
            if (operationFilter == null || operationFilter.isBlank() || "ALL".equalsIgnoreCase(operationFilter)) {
                return new ArrayList<>(records);
            }
            List<OperationRecord> filtered = new ArrayList<>();
            for (OperationRecord record : records) {
                if (record.getOperation().equalsIgnoreCase(operationFilter)) {
                    filtered.add(record);
                }
            }
            return filtered;
        }
    }

    public void clear() {
        synchronized (lock) {
            records.clear();
        }
    }

    public int size() {
        synchronized (lock) {
            return records.size();
        }
    }

    public int getCapacity() {
        synchronized (lock) {
            return capacity;
        }
    }

    public void setCapacity(int newCapacity) {
        if (newCapacity < CacheDefaults.MIN_HISTORY_CAPACITY || newCapacity > CacheDefaults.MAX_HISTORY_CAPACITY) {
            return;
        }
        synchronized (lock) {
            this.capacity = newCapacity;
            while (records.size() > capacity) {
                records.removeFirst();
            }
        }
    }

    public void exportCsv(Writer out) throws IOException {
        BufferedWriter writer = out instanceof BufferedWriter ? (BufferedWriter) out : new BufferedWriter(out);
        writer.write("Timestamp,Thread,Operation,Key,Outcome,LatencyNanos,Details");
        writer.newLine();

        List<OperationRecord> snapshot = getRecentRecords();
        for (OperationRecord r : snapshot) {
            writer.write(String.format("%d,%s,%s,%s,%s,%d,%s",
                    r.getTimestamp(),
                    escapeCsv(r.getThreadName()),
                    escapeCsv(r.getOperation()),
                    escapeCsv(r.getCacheKey()),
                    escapeCsv(r.getOutcome()),
                    r.getLatencyNanos(),
                    escapeCsv(r.getDetails())));
            writer.newLine();
        }
        writer.flush();
    }

    private static String escapeCsv(String input) {
        if (input == null) return "";
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
}
