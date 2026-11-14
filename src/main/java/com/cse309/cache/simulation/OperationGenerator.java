package com.cse309.cache.simulation;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

/**
 * Deterministic operation generator that converts SimulationConfig specifications
 * into an orderly stream of PlannedOperations.
 */
public final class OperationGenerator {

    private final SimulationConfig config;
    private final Random random;
    private int sequentialIndex = 0;

    public OperationGenerator(SimulationConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.random = config.getSeed().isPresent()
                ? new Random(config.getSeed().get())
                : new Random();
    }

    public PlannedOperation nextOperation(int sequence) {
        PlannedOperation.Type type = selectType();
        int keyId = selectKeyId();
        String key = "item:" + keyId;
        String value = "val-" + keyId + "-seq-" + sequence;

        Duration ttl = null;
        if (type == PlannedOperation.Type.PUT && sequence % 10 == 0) {
            ttl = Duration.ofSeconds(3);
        }

        return new PlannedOperation(type, key, value, ttl, sequence);
    }

    private PlannedOperation.Type selectType() {
        int r = random.nextInt(100);
        OperationMix mix = config.getOperationMix();

        int threshold1 = mix.getGetPercent();
        int threshold2 = threshold1 + mix.getPutPercent();
        int threshold3 = threshold2 + mix.getUpdatePercent();
        int threshold4 = threshold3 + mix.getDeletePercent();

        if (r < threshold1) return PlannedOperation.Type.GET;
        if (r < threshold2) return PlannedOperation.Type.PUT;
        if (r < threshold3) return PlannedOperation.Type.UPDATE;
        if (r < threshold4) return PlannedOperation.Type.DELETE;
        return PlannedOperation.Type.SEARCH;
    }

    private int selectKeyId() {
        int keySpace = config.getKeySpaceSize();
        return switch (config.getKeyDistribution()) {
            case UNIFORM -> random.nextInt(keySpace);
            case HOTSPOT_80_20 -> {
                int hotspotCount = Math.max(1, (int) (keySpace * 0.2));
                if (random.nextInt(100) < 80) {
                    yield random.nextInt(hotspotCount);
                } else {
                    int remaining = keySpace - hotspotCount;
                    yield hotspotCount + (remaining > 0 ? random.nextInt(remaining) : 0);
                }
            }
            case SEQUENTIAL_SCAN -> {
                int id = sequentialIndex % keySpace;
                sequentialIndex++;
                yield id;
            }
        };
    }
}
