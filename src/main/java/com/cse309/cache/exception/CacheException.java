package com.cse309.cache.exception;

/**
 * Root checked exception for the cache application.
 * Distinguishes between internal technical messages and human-friendly user messages.
 */
public class CacheException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String userMessage;

    public CacheException(String technicalMessage, String userMessage, Throwable cause) {
        super(technicalMessage, cause);
        this.userMessage = userMessage != null ? userMessage : technicalMessage;
    }

    public CacheException(String technicalMessage, String userMessage) {
        this(technicalMessage, userMessage, null);
    }

    public CacheException(String message, Throwable cause) {
        this(message, message, cause);
    }

    public CacheException(String message) {
        this(message, message, null);
    }

    public String getUserMessage() {
        return userMessage;
    }
}
