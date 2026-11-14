package com.cse309.cache.exception;

/**
 * Thrown when an update operation is attempted on a non-existent or expired key.
 */
public class CacheKeyNotFoundException extends CacheException {

    private static final long serialVersionUID = 1L;

    private final Object key;

    public CacheKeyNotFoundException(Object key, String technicalMessage, String userMessage) {
        super(technicalMessage, userMessage);
        this.key = key;
    }

    public CacheKeyNotFoundException(Object key) {
        this(key, "Cache key not found: " + key, "The specified key '" + key + "' does not exist in the cache.");
    }

    public Object getKey() {
        return key;
    }
}
