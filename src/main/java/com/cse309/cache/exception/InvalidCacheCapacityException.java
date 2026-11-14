package com.cse309.cache.exception;

/**
 * Thrown when an invalid cache capacity is specified (outside 1 to 1,000,000).
 */
public class InvalidCacheCapacityException extends CacheException {

    private static final long serialVersionUID = 1L;

    public InvalidCacheCapacityException(String technicalMessage, String userMessage, Throwable cause) {
        super(technicalMessage, userMessage, cause);
    }

    public InvalidCacheCapacityException(String technicalMessage, String userMessage) {
        super(technicalMessage, userMessage);
    }

    public InvalidCacheCapacityException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCacheCapacityException(String message) {
        super(message);
    }
}
