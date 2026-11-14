package com.cse309.cache.core;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Generic collection utilities showcasing generic methods with bounded type parameters.
 * Fulfills CSE 309 Unit 5 requirements.
 */
public final class CollectionTools {

    private CollectionTools() {
        // Prevent instantiation
    }

    /**
     * Bounded generic method to find the minimum element in a collection.
     * Demonstrates: {@code <T extends Comparable<? super T>>}
     *
     * @param collection collection of elements
     * @param <T> comparable element type
     * @return optional containing the minimum element, or empty if collection is null/empty
     */
    public static <T extends Comparable<? super T>> Optional<T> findMin(Collection<? extends T> collection) {
        if (collection == null || collection.isEmpty()) {
            return Optional.empty();
        }

        T min = null;
        for (T element : collection) {
            if (element == null) continue;
            if (min == null || element.compareTo(min) < 0) {
                min = element;
            }
        }
        return Optional.ofNullable(min);
    }

    /**
     * Bounded generic method to find the maximum element in a collection.
     * Demonstrates: {@code <T extends Comparable<? super T>>}
     *
     * @param collection collection of elements
     * @param <T> comparable element type
     * @return optional containing the maximum element, or empty if collection is null/empty
     */
    public static <T extends Comparable<? super T>> Optional<T> findMax(Collection<? extends T> collection) {
        if (collection == null || collection.isEmpty()) {
            return Optional.empty();
        }

        T max = null;
        for (T element : collection) {
            if (element == null) continue;
            if (max == null || element.compareTo(max) > 0) {
                max = element;
            }
        }
        return Optional.ofNullable(max);
    }
}
