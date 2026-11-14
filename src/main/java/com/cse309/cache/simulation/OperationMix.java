package com.cse309.cache.simulation;

/**
 * Proportional breakdown of operations in a workload simulation.
 * Percentages must sum exactly to 100%.
 */
public final class OperationMix {

    public static final OperationMix READ_HEAVY = new OperationMix(85, 10, 3, 2, 0);
    public static final OperationMix MIXED = new OperationMix(50, 30, 10, 5, 5);
    public static final OperationMix WRITE_HEAVY = new OperationMix(20, 60, 10, 10, 0);

    private final int getPercent;
    private final int putPercent;
    private final int updatePercent;
    private final int deletePercent;
    private final int searchPercent;

    public OperationMix(int getPercent, int putPercent, int updatePercent, int deletePercent, int searchPercent) {
        int sum = getPercent + putPercent + updatePercent + deletePercent + searchPercent;
        if (sum != 100) {
            throw new IllegalArgumentException("Operation mix percentages must sum to 100, but was: " + sum);
        }
        if (getPercent < 0 || putPercent < 0 || updatePercent < 0 || deletePercent < 0 || searchPercent < 0) {
            throw new IllegalArgumentException("Percentages cannot be negative");
        }
        this.getPercent = getPercent;
        this.putPercent = putPercent;
        this.updatePercent = updatePercent;
        this.deletePercent = deletePercent;
        this.searchPercent = searchPercent;
    }

    public int getGetPercent() { return getPercent; }
    public int getPutPercent() { return putPercent; }
    public int getUpdatePercent() { return updatePercent; }
    public int getDeletePercent() { return deletePercent; }
    public int getSearchPercent() { return searchPercent; }

    @Override
    public String toString() {
        return String.format("Mix(get=%d%%, put=%d%%, update=%d%%, del=%d%%, search=%d%%)",
                getPercent, putPercent, updatePercent, deletePercent, searchPercent);
    }
}
