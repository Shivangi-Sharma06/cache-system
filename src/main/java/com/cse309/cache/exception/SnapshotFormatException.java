package com.cse309.cache.exception;

/**
 * Thrown when a snapshot file cannot be loaded due to corruption,
 * bad magic number, unsupported version, or checksum mismatch.
 */
public class SnapshotFormatException extends CachePersistenceException {

    private static final long serialVersionUID = 1L;

    public SnapshotFormatException(String technicalMessage, String userMessage, Throwable cause) {
        super(technicalMessage, userMessage, cause);
    }

    public SnapshotFormatException(String message, Throwable cause) {
        super(message, "Invalid snapshot file: " + message, cause);
    }

    public SnapshotFormatException(String message) {
        super(message, null);
    }
}
