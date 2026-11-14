package com.cse309.cache.exception;

/**
 * Thrown when an illegal cache operation is requested (e.g. null key/value, negative TTL,
 * starting a simulation while one is already active).
 */
public class InvalidCacheOperationException extends CacheException {

    private static final long serialVersionUID = 1L;

    public InvalidCacheOperationException(String technicalMessage, String userMessage, Throwable cause) {
        super(technicalMessage, userMessage, cause);
    }

    public InvalidCacheOperationException(String technicalMessage, String userMessage) {
        super(technicalMessage, userMessage);
    }

    public InvalidCacheOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCacheOperationException(String message) {
        super(message);
    }
}
