package com.cse309.cache.persistence;

import com.cse309.cache.core.CacheDefaults;
import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CachePersistenceException;
import com.cse309.cache.exception.SnapshotFormatException;
import com.cse309.cache.logging.AppLogging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * Handles custom binary .jcs file I/O for cache snapshots.
 * Uses DataOutputStream, DataInputStream, CRC32 checksums, and atomic file replacement (Unit 3).
 * Demonstrates multiple catch blocks, multi-catch, and nested try (Unit 2).
 */
public final class SnapshotFileManager {

    private static final Logger LOGGER = AppLogging.getLogger(SnapshotFileManager.class);

    public static final int MAGIC_NUMBER = 0x4A435331; // "JCS1"
    public static final short CURRENT_VERSION = 1;

    public record SnapshotFileContent(
            int capacity,
            EvictionPolicyType policyType,
            List<CacheEntrySnapshot<String, String>> entries
    ) {}

    private SnapshotFileManager() {
        // Utility class
    }

    /**
     * Saves snapshot entries to a binary .jcs file using safe atomic writes.
     */
    public static void save(Path targetFile,
                            int capacity,
                            EvictionPolicyType policyType,
                            List<CacheEntrySnapshot<String, String>> entries)
            throws CachePersistenceException {
        Objects.requireNonNull(targetFile, "targetFile cannot be null");
        Objects.requireNonNull(policyType, "policyType cannot be null");

        Path parentDir = targetFile.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new CachePersistenceException("Failed to create snapshot directory: " + e.getMessage(), e);
            }
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(parentDir != null ? parentDir : Path.of("."), "snapshot-", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile());
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 CheckedOutputStream cos = new CheckedOutputStream(bos, new CRC32());
                 DataOutputStream dos = new DataOutputStream(cos)) {

                // Header
                dos.writeInt(MAGIC_NUMBER);
                dos.writeShort(CURRENT_VERSION);
                dos.writeInt(capacity);
                dos.writeUTF(policyType.name());
                int count = entries != null ? entries.size() : 0;
                dos.writeInt(count);

                // Entries
                if (entries != null) {
                    for (CacheEntrySnapshot<String, String> entry : entries) {
                        dos.writeUTF(entry.getKey());
                        dos.writeUTF(entry.getValue());
                        dos.writeLong(entry.getCreatedAt());
                        dos.writeLong(entry.getLastAccessAt());
                        dos.writeLong(entry.getExpiresAt());
                        dos.writeInt((int) entry.getAccessCount());
                        dos.writeLong(entry.getInsertionSequence());
                    }
                }

                dos.flush();
                long checksum = cos.getChecksum().getValue();

                // Append checksum (written directly to fos/dos without modifying checksum)
                dos.writeLong(checksum);
                dos.flush();

                try {
                    fos.getFD().sync();
                } catch (SyncFailedException ignored) {
                    // Sync may not be supported on all virtualized filesystems
                }
            }

            // Atomic file replacement
            try {
                Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info("Successfully wrote snapshot to " + targetFile + " (" + (entries != null ? entries.size() : 0) + " entries)");

        } catch (IOException e) {
            throw new CachePersistenceException("Failed to write snapshot file " + targetFile + ": " + e.getMessage(), e);
        } finally {
            if (tempFile != null && Files.exists(tempFile)) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupEx) {
                    LOGGER.log(Level.FINE, "Could not delete temporary snapshot file " + tempFile + ": " + cleanupEx.getMessage(), cleanupEx);
                }
            }
        }
    }

    /**
     * Reads and strictly validates a binary .jcs snapshot file.
     * Demonstrates multiple catch blocks, multi-catch (A | B e), and nested try (Unit 2).
     */
    public static SnapshotFileContent load(Path sourceFile) throws CachePersistenceException {
        Objects.requireNonNull(sourceFile, "sourceFile cannot be null");
        File file = sourceFile.toFile();

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             CheckedInputStream cis = new CheckedInputStream(bis, new CRC32());
             DataInputStream dis = new DataInputStream(cis)) {

            // Read and validate magic number
            int magic = dis.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new SnapshotFormatException("Invalid snapshot file: bad magic number 0x" +
                        Integer.toHexString(magic).toUpperCase() + " (expected 0x" +
                        Integer.toHexString(MAGIC_NUMBER).toUpperCase() + ")");
            }

            // Read and validate version
            short version = dis.readShort();
            if (version > CURRENT_VERSION) {
                throw new SnapshotFormatException("Unsupported snapshot version " + version +
                        " (maximum supported is " + CURRENT_VERSION + ")");
            }

            // Read capacity
            int capacity = dis.readInt();
            if (capacity < CacheDefaults.MIN_CAPACITY || capacity > CacheDefaults.MAX_CAPACITY) {
                throw new SnapshotFormatException("Snapshot contains invalid capacity: " + capacity);
            }

            // Read policy
            String policyName = dis.readUTF();
            EvictionPolicyType policyType;
            try {
                policyType = EvictionPolicyType.valueOf(policyName);
            } catch (IllegalArgumentException e) {
                throw new SnapshotFormatException("Snapshot contains unknown eviction policy: " + policyName, e);
            }

            // Read entry count
            int entryCount = dis.readInt();
            if (entryCount < 0 || entryCount > CacheDefaults.MAX_CAPACITY) {
                throw new SnapshotFormatException("Snapshot contains invalid entry count: " + entryCount);
            }

            List<CacheEntrySnapshot<String, String>> entries = new ArrayList<>(entryCount);
            Set<String> seenKeys = new HashSet<>(entryCount);

            for (int i = 0; i < entryCount; i++) {
                try {
                    String key = dis.readUTF();
                    String value = dis.readUTF();
                    long createdAt = dis.readLong();
                    long lastAccessAt = dis.readLong();
                    long expiresAt = dis.readLong();
                    int accessCount = dis.readInt();
                    long insertionSeq = dis.readLong();

                    if (accessCount < 0) {
                        throw new SnapshotFormatException("Negative access count for key: " + key);
                    }
                    if (!seenKeys.add(key)) {
                        throw new SnapshotFormatException("Duplicate key in snapshot file: " + key);
                    }

                    entries.add(new CacheEntrySnapshot<>(
                            key, value, createdAt, lastAccessAt, expiresAt, accessCount, insertionSeq
                    ));
                } catch (EOFException eof) {
                    throw new SnapshotFormatException("Snapshot file truncated prematurely at entry #" + (i + 1), eof);
                }
            }

            // Checksum verification
            long computedChecksum = cis.getChecksum().getValue();
            long storedChecksum = dis.readLong();

            if (computedChecksum != storedChecksum) {
                throw new SnapshotFormatException(String.format(
                        "CRC32 checksum mismatch: file stored 0x%X but computed 0x%X (file corrupted)",
                        storedChecksum, computedChecksum));
            }

            LOGGER.info("Successfully loaded snapshot from " + sourceFile + " (" + entries.size() + " entries)");
            return new SnapshotFileContent(capacity, policyType, entries);

        } catch (FileNotFoundException e) {
            // First specific catch
            throw new CachePersistenceException("Snapshot file not found: " + sourceFile, e);
        } catch (EOFException e) {
            // Second specific catch
            throw new SnapshotFormatException("Snapshot file is truncated or incomplete: " + sourceFile, e);
        } catch (java.io.UTFDataFormatException e) {
            throw new SnapshotFormatException("Snapshot file contains corrupt string data: " + e.getMessage(), e);
        } catch (SnapshotFormatException e) {
            // Re-throw SnapshotFormatException directly
            throw e;
        } catch (IOException | SecurityException e) {
            // Multi-catch demonstrating language feature (Unit 2)
            throw new CachePersistenceException("I/O error reading snapshot file: " + e.getMessage(), e);
        }
    }
}
