package com.cse309.cache.exception;

/**
 * Thrown when database or filesystem persistence operations fail.
 * Always wraps the underlying cause.
 */
public class CachePersistenceException extends CacheException {

    private static final long serialVersionUID = 1L;

    public CachePersistenceException(String technicalMessage, String userMessage, Throwable cause) {
        super(technicalMessage, userMessage, cause);
    }

    public CachePersistenceException(String message, Throwable cause) {
        super(message, "Persistence failure: " + (cause != null ? cause.getMessage() : message), cause);
    }
}
