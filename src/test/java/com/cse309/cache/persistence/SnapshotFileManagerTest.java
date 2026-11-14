package com.cse309.cache.persistence;

import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CachePersistenceException;
import com.cse309.cache.exception.SnapshotFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotFileManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testRoundTripBinarySnapshot() throws Exception {
        Path file = tempDir.resolve("test-snap.jcs");

        List<CacheEntrySnapshot<String, String>> entries = List.of(
                new CacheEntrySnapshot<>("user:1", "Alice", 1000L, 1050L, 2000L, 5L, 1L),
                new CacheEntrySnapshot<>("user:2", "Bob", 1100L, 1150L, -1L, 2L, 2L)
        );

        SnapshotFileManager.save(file, 100, EvictionPolicyType.LRU, entries);
        assertTrue(Files.exists(file));

        SnapshotFileManager.SnapshotFileContent loaded = SnapshotFileManager.load(file);
        assertEquals(100, loaded.capacity());
        assertEquals(EvictionPolicyType.LRU, loaded.policyType());
        assertEquals(2, loaded.entries().size());
        assertEquals("user:1", loaded.entries().get(0).getKey());
        assertEquals("Alice", loaded.entries().get(0).getValue());
        assertEquals("user:2", loaded.entries().get(1).getKey());
        assertEquals("Bob", loaded.entries().get(1).getValue());
    }

    @Test
    void testBadMagicThrowsSnapshotFormatException() throws Exception {
        Path file = tempDir.resolve("bad-magic.jcs");
        Files.write(file, new byte[]{0x00, 0x01, 0x02, 0x03, 0x04});

        assertThrows(SnapshotFormatException.class, () -> SnapshotFileManager.load(file));
    }

    @Test
    void testTruncatedFileThrowsSnapshotFormatException() throws Exception {
        Path file = tempDir.resolve("truncated.jcs");

        List<CacheEntrySnapshot<String, String>> entries = List.of(
                new CacheEntrySnapshot<>("k1", "v1", 100L, 100L, -1L, 1L, 1L)
        );
        SnapshotFileManager.save(file, 50, EvictionPolicyType.FIFO, entries);

        // Truncate file by 10 bytes
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.setLength(raf.length() - 10);
        }

        assertThrows(SnapshotFormatException.class, () -> SnapshotFileManager.load(file));
    }

    @Test
    void testCorruptChecksumThrowsSnapshotFormatException() throws Exception {
        Path file = tempDir.resolve("corrupt-checksum.jcs");

        List<CacheEntrySnapshot<String, String>> entries = List.of(
                new CacheEntrySnapshot<>("k1", "v1", 100L, 100L, -1L, 1L, 1L)
        );
        SnapshotFileManager.save(file, 50, EvictionPolicyType.FIFO, entries);

        // Tamper with the checksum byte at the end of the file
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 0xFF); // Flip bits in CRC32 checksum
        Files.write(file, bytes);

        assertThrows(SnapshotFormatException.class, () -> SnapshotFileManager.load(file));
    }

    @Test
    void testFailedSavePreservesOriginalFile() throws Exception {
        Path file = tempDir.resolve("safe-overwrite.jcs");

        List<CacheEntrySnapshot<String, String>> originalEntries = List.of(
                new CacheEntrySnapshot<>("orig", "data", 100L, 100L, -1L, 1L, 1L)
        );
        SnapshotFileManager.save(file, 10, EvictionPolicyType.LRU, originalEntries);
        long originalSize = Files.size(file);

        // Attempt to save to an invalid target path (directory instead of file)
        Path invalidTarget = tempDir.resolve("a-dir");
        Files.createDirectories(invalidTarget);

        assertThrows(CachePersistenceException.class, () ->
                SnapshotFileManager.save(invalidTarget, 10, EvictionPolicyType.LRU, originalEntries));

        // Original file should remain untouched
        assertEquals(originalSize, Files.size(file));
    }
}
