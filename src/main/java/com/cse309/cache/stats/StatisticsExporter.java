package com.cse309.cache.stats;

import com.cse309.cache.core.CacheEntrySnapshot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Utility for exporting cache statistics and entry records to CSV format.
 * Utilizes BufferedWriter and try-with-resources (Unit 3).
 */
public final class StatisticsExporter {

    private StatisticsExporter() {
        // Prevent instantiation
    }

    public static void exportStatisticsCsv(StatisticsSnapshot stats, Writer out) throws IOException {
        BufferedWriter writer = out instanceof BufferedWriter ? (BufferedWriter) out : new BufferedWriter(out);

        writer.write("Metric,Value");
        writer.newLine();

        writeRow(writer, "Timestamp", String.valueOf(stats.getTimestampMillis()));
        writeRow(writer, "ElapsedSeconds", String.format("%.3f", stats.getElapsedSeconds()));
        writeRow(writer, "CurrentSize", String.valueOf(stats.getCurrentSize()));
        writeRow(writer, "CurrentCapacity", String.valueOf(stats.getCurrentCapacity()));
        writeRow(writer, "UtilizationPercent", String.format("%.2f", stats.getUtilization()));
        writeRow(writer, "TotalRequests", String.valueOf(stats.getTotalRequests()));
        writeRow(writer, "GetRequests", String.valueOf(stats.getGetRequests()));
        writeRow(writer, "PutRequests", String.valueOf(stats.getPutRequests()));
        writeRow(writer, "UpdateRequests", String.valueOf(stats.getUpdateRequests()));
        writeRow(writer, "RemoveRequests", String.valueOf(stats.getRemoveRequests()));
        writeRow(writer, "SearchRequests", String.valueOf(stats.getSearchRequests()));
        writeRow(writer, "Hits", String.valueOf(stats.getHits()));
        writeRow(writer, "Misses", String.valueOf(stats.getMisses()));
        writeRow(writer, "HitRatioPercent", String.format("%.2f", stats.getHitRatio()));
        writeRow(writer, "MissRatioPercent", String.format("%.2f", stats.getMissRatio()));
        writeRow(writer, "Evictions", String.valueOf(stats.getEvictions()));
        writeRow(writer, "Expirations", String.valueOf(stats.getExpirations()));
        writeRow(writer, "SuccessfulOperations", String.valueOf(stats.getSuccessfulOperations()));
        writeRow(writer, "FailedOperations", String.valueOf(stats.getFailedOperations()));
        writeRow(writer, "ThroughputOpsPerSec", String.format("%.2f", stats.getThroughputOpsPerSec()));

        long[] getLat = stats.getGetLatency();
        writeRow(writer, "GetAvgLatencyMicros", String.format("%.3f", stats.getAverageLatencyMicros(getLat)));
        writeRow(writer, "GetMaxLatencyMicros", String.format("%.3f", stats.getMaxLatencyMicros(getLat)));

        long[] putLat = stats.getPutLatency();
        writeRow(writer, "PutAvgLatencyMicros", String.format("%.3f", stats.getAverageLatencyMicros(putLat)));
        writeRow(writer, "PutMaxLatencyMicros", String.format("%.3f", stats.getMaxLatencyMicros(putLat)));

        long[] hist = stats.getHistogramCounts();
        for (int i = 0; i < hist.length && i < LatencyHistogram.BUCKET_LABELS.length; i++) {
            writeRow(writer, "Histogram_" + LatencyHistogram.BUCKET_LABELS[i], String.valueOf(hist[i]));
        }

        writer.flush();
    }

    public static void exportEntriesCsv(List<? extends CacheEntrySnapshot<?, ?>> entries, Writer out) throws IOException {
        BufferedWriter writer = out instanceof BufferedWriter ? (BufferedWriter) out : new BufferedWriter(out);

        writer.write("Key,Value,CreatedAt,LastAccessAt,ExpiresAt,AccessCount,InsertionSequence");
        writer.newLine();

        if (entries != null) {
            for (CacheEntrySnapshot<?, ?> entry : entries) {
                writer.write(String.format("%s,%s,%d,%d,%d,%d,%d",
                        escapeCsv(String.valueOf(entry.getKey())),
                        escapeCsv(String.valueOf(entry.getValue())),
                        entry.getCreatedAt(),
                        entry.getLastAccessAt(),
                        entry.getExpiresAt(),
                        entry.getAccessCount(),
                        entry.getInsertionSequence()));
                writer.newLine();
            }
        }
        writer.flush();
    }

    private static void writeRow(BufferedWriter writer, String metric, String value) throws IOException {
        writer.write(escapeCsv(metric));
        writer.write(",");
        writer.write(escapeCsv(value));
        writer.newLine();
    }

    private static String escapeCsv(String input) {
        if (input == null) return "";
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
}
