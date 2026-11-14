package com.cse309.cache.lab;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Educational laboratory comparing Java collections performance and demonstrating
 * fail-fast ConcurrentModificationException vs safe Iterator.remove() (Unit 4).
 * Marks Vector and Hashtable as legacy collections.
 */
public final class CollectionsLab {

    public record BenchmarkEntry(
            String collectionType,
            String category,
            boolean legacy,
            long insertNanos,
            long lookupNanos,
            long iterateNanos,
            long removeNanos
    ) {}

    private CollectionsLab() {
        // Utility class
    }

    public static List<BenchmarkEntry> runComparisons(int elementCount) {
        List<BenchmarkEntry> results = new ArrayList<>();

        // Warmup
        warmup();

        // 1. Lists: ArrayList, LinkedList, Vector (legacy)
        results.add(benchmarkList("ArrayList", false, new ArrayList<>(), elementCount));
        results.add(benchmarkList("LinkedList", false, new LinkedList<>(), elementCount));
        results.add(benchmarkList("Vector", true, new Vector<>(), elementCount));

        // 2. Maps: HashMap, Hashtable (legacy), LinkedHashMap, TreeMap
        results.add(benchmarkMap("HashMap", false, new HashMap<>(), elementCount));
        results.add(benchmarkMap("Hashtable", true, new Hashtable<>(), elementCount));
        results.add(benchmarkMap("LinkedHashMap", false, new LinkedHashMap<>(), elementCount));
        results.add(benchmarkMap("TreeMap", false, new TreeMap<>(), elementCount));

        // 3. Sets: HashSet, TreeSet
        results.add(benchmarkSet("HashSet", false, new HashSet<>(), elementCount));
        results.add(benchmarkSet("TreeSet", false, new TreeSet<>(), elementCount));

        return results;
    }

    private static BenchmarkEntry benchmarkList(String name, boolean legacy, List<Integer> list, int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) list.add(i);
        long insertNanos = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        int target = n / 2;
        boolean found = list.contains(target);
        long lookupNanos = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        long sum = 0;
        for (Integer val : list) sum += val;
        long iterateNanos = System.nanoTime() - t2;

        long t3 = System.nanoTime();
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        long removeNanos = System.nanoTime() - t3;

        return new BenchmarkEntry(name, "List", legacy, insertNanos, lookupNanos, iterateNanos, removeNanos);
    }

    private static BenchmarkEntry benchmarkMap(String name, boolean legacy, Map<Integer, String> map, int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) map.put(i, "v" + i);
        long insertNanos = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        String val = map.get(n / 2);
        long lookupNanos = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        long keySum = 0;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            keySum += entry.getKey();
        }
        long iterateNanos = System.nanoTime() - t2;

        long t3 = System.nanoTime();
        Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        long removeNanos = System.nanoTime() - t3;

        return new BenchmarkEntry(name, "Map", legacy, insertNanos, lookupNanos, iterateNanos, removeNanos);
    }

    private static BenchmarkEntry benchmarkSet(String name, boolean legacy, Set<Integer> set, int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) set.add(i);
        long insertNanos = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        boolean found = set.contains(n / 2);
        long lookupNanos = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        long sum = 0;
        for (Integer val : set) sum += val;
        long iterateNanos = System.nanoTime() - t2;

        long t3 = System.nanoTime();
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        long removeNanos = System.nanoTime() - t3;

        return new BenchmarkEntry(name, "Set", legacy, insertNanos, lookupNanos, iterateNanos, removeNanos);
    }

    private static void warmup() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 5000; i++) list.add(i);
        for (Integer i : list) {}
        list.clear();
    }

    /**
     * Demonstrates fail-fast ConcurrentModificationException when mutating during iteration.
     *
     * @return true if CME was caught as expected
     */
    public static boolean demonstrateFailFastCme() {
        List<String> list = new ArrayList<>(List.of("A", "B", "C"));
        try {
            for (String item : list) {
                if ("B".equals(item)) {
                    list.add("D"); // Modifying collection directly during for-each iteration!
                }
            }
            return false;
        } catch (ConcurrentModificationException e) {
            return true;
        }
    }

    /**
     * Demonstrates safe structural modification using explicit Iterator.remove().
     *
     * @return true if all matching items were safely removed without error
     */
    public static boolean demonstrateSafeIteratorRemove() {
        List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String item = it.next();
            if ("B".equals(item) || "D".equals(item)) {
                it.remove(); // Safe removal via Iterator
            }
        }
        return list.size() == 2 && list.contains("A") && list.contains("C");
    }
}
