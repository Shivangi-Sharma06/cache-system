package com.cse309.cache.exception;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheExceptionTest {

    @Test
    void testRootCacheException() {
        Throwable cause = new RuntimeException("Underlying root");
        CacheException ex = new CacheException("Tech detail", "User friendly message", cause);
        assertEquals("Tech detail", ex.getMessage());
        assertEquals("User friendly message", ex.getUserMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void testKeyNotFoundException() {
        CacheKeyNotFoundException ex = new CacheKeyNotFoundException("key123");
        assertEquals("key123", ex.getKey());
        assertTrue(ex.getUserMessage().contains("key123"));
    }

    @Test
    void testPersistenceExceptionChaining() {
        IOException ioEx = new IOException("Disk failure");
        SnapshotFormatException sfe = new SnapshotFormatException("Corrupt magic", ioEx);
        assertTrue(sfe instanceof CachePersistenceException);
        assertTrue(sfe instanceof CacheException);
        assertEquals(ioEx, sfe.getCause());
    }

    @Test
    void testConfigurationExceptionErrors() {
        List<String> errors = List.of("Err 1", "Err 2");
        CacheConfigurationException ex = new CacheConfigurationException(errors);
        assertEquals(2, ex.getValidationErrors().size());
        assertTrue(ex.getUserMessage().contains("Err 1"));
        assertTrue(ex.getUserMessage().contains("Err 2"));
    }
}
