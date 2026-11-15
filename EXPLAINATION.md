# Technical Architecture & System Explanation

**Project**: Multithreaded Java-Based In-Memory Caching System with Concurrent Access Management  
**Course**: CSE 309 Advanced Java Programming  
**Author**: Shivangi Sharma  
**Target File**: `EXPLANATION.md` (Deliverable for Task v & Oral Viva Evaluation)

---

## 1. Project Overview & Core Purpose

This project implements a production-grade, thread-safe, in-memory key-value caching system written in pure Java (JDK 17 LTS) with a desktop JavaFX user interface. 

### What the System Does
- **Primary In-Memory Cache**: Stores key-value entries (`StringCache` mapping `String` to `String`) directly in RAM to achieve sub-microsecond lookup and mutation latencies ($< 1\,\mu\text{s}$).
- **Dynamic Eviction Algorithms**: Enforces strict capacity limits via three pluggable eviction algorithms:
  - **LRU (Least Recently Used)**: Evicts the least recently accessed key ($O(1)$).
  - **FIFO (First-In First-Out)**: Evicts the oldest inserted key ($O(1)$).
  - **LFU (Least Frequently Used)**: Evicts the lowest-frequency accessed key with deterministic tie-breaking ($O(\log n)$).
- **Wall-Clock Expiration (TTL)**: Supports optional, configurable Time-To-Live expiration with both lazy on-access expiration and a proactive background daemon thread sweep.
- **Concurrent Access & Workload Simulator**: Employs a hand-crafted producer-consumer multi-threaded simulation engine (1 to 64 worker threads) that executes concurrent reads and writes against the live cache engine without corruption or deadlock.
- **Dual Out-of-Band Persistence**:
  - **SQLite Embedded Database**: Point-in-time snapshot persistence for durable backups, warm starts, and recovery.
  - **Binary Snapshot (`.jcs`)**: High-speed, tamper-evident binary file serialization via byte streams with CRC32 integrity verification.

---

## 2. Tech Stack & Architectural Decisions

| Technology | Role in System | Rationale for Selection |
|---|---|---|
| **Java 17 LTS** | Core Language & Platform | Leverages modern language features (records, switch expressions, `java.time`, sealed/final class design) while maintaining compatibility with standard lab environments (`--release 17`). |
| **OpenJFX 21 (JavaFX)** | Desktop Graphical Interface | Pure Java GUI implementation (no FXML or external CSS) providing real-time data binding, charts, and visual transforms without native toolkit conflicts. |
| **SQLite JDBC (3.47.1.0)** | Embedded Relational Storage | Serverless, zero-configuration local database (`data/cache.db`). Runs in WAL (Write-Ahead Logging) mode; requires zero network overhead, credentials, or `.env` configuration. |
| **Apache Maven 3.9+** | Build & Lifecycle Management | Standardized dependency management, clean lifecycle phases (`clean`, `compile`, `test`, `package`), and JavaFX runtime plugin execution. |
| **JUnit 5** | Test Verification Suite | Plain Java unit and concurrency stress testing; ensures zero regressions across 79 tests. |

### Why Third-Party Caches & Libraries Were Forbidden
External caching frameworks (Redis, Caffeine, Ehcache, Guava) and utility libraries (Lombok, Spring, Jackson) were intentionally excluded. The academic purpose of CSE 309 is mastering core Java primitives: `synchronized` monitors, `wait()`/`notifyAll()`, `Thread` lifecycles, JDBC transactions, and the Collections Framework implemented from first principles.

---

## 3. End-to-End Architectural Workflow

### 3.1. Hot Path Cache Access (RAM First)
1. **Client / GUI Request**: An operation (`put`, `get`, `update`, `remove`) is initiated via [`OperationsTab`](src/main/java/com/cse309/cache/ui/OperationsTab.java) or a simulator [`WorkerThread`](src/main/java/com/cse309/cache/threading/WorkerThread.java).
2. **Lock Acquisition**: The call enters [`ThreadSafeCache`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java) and acquires the private monitor lock `synchronized (lock)`.
3. **Lazy TTL Check**: If the target key exists but has expired (`now >= expiresAt`), it is immediately purged, recording an expiration.
4. **Operation Execution**:
   - `get()` updates policy recency (LRU) or increments access count (LFU) and returns a [`CacheLookupResult`](src/main/java/com/cse309/cache/core/CacheLookupResult.java).
   - `put()` checks available capacity. If full, `purgeExpired()` runs first. If still full, `policy.selectVictim()` selects an entry for eviction before inserting the new entry.
5. **Lock Release & Event Dispatch**: The lock is released. Buffered events ([`CacheEvent`](src/main/java/com/cse309/cache/event/CacheEvent.java)) are dispatched to UI listeners off the critical path, guaranteeing zero lock contention for listeners.

### 3.2. Background Expiration Workflow
1. [`CacheExpirationService`](src/main/java/com/cse309/cache/expiry/CacheExpirationService.java) runs as a daemon thread named `cache-expiry-service`.
2. It waits on a private monitor using `monitor.wait(intervalMillis)`.
3. Upon waking, it calls [`ThreadSafeCache.purgeExpired()`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java#L230).
4. Rather than scanning the entire map, it queries [`ExpiryIndex`](src/main/java/com/cse309/cache/expiry/ExpiryIndex.java), which maintains a `TreeSet` sorted by `expiresAt`. It polls expired entries from the head of the tree in $O(k)$ time (where $k$ is the number of expired entries).

### 3.3. Producer-Consumer Simulation Workflow
1. The user configures workload parameters (threads, operation mix, key distribution) in [`SimulatorTab`](src/main/java/com/cse309/cache/ui/SimulatorTab.java) and clicks "Start Simulation".
2. [`SimulationController`](src/main/java/com/cse309/cache/simulation/SimulationController.java) spawns an [`OperationProducerTask`](src/main/java/com/cse309/cache/simulation/OperationProducerTask.java) on a dedicated producer thread.
3. The producer generates [`PlannedOperation`](src/main/java/com/cse309/cache/simulation/PlannedOperation.java) objects and places them into a custom [`BoundedBuffer<PlannedOperation>`](src/main/java/com/cse309/cache/threading/BoundedBuffer.java).
4. If the buffer is full, the producer blocks on `wait()`.
5. A pool of [`WorkerThread`](src/main/java/com/cse309/cache/threading/WorkerThread.java) instances (`cache-worker-1` to `cache-worker-N`) continuously calls `buffer.take()`, waking producers via `notifyAll()`.
6. Each worker checks [`PauseGate.await()`](src/main/java/com/cse309/cache/threading/PauseGate.java) (for interactive pause/resume), executes the operation against the shared cache engine, measures latency using `System.nanoTime()`, and updates atomic counters in [`CacheStatistics`](src/main/java/com/cse309/cache/stats/CacheStatistics.java).

### 3.4. Out-of-Band Snapshot Persistence Workflow
1. The user clicks "Save Current State to Database" in [`PersistenceTab`](src/main/java/com/cse309/cache/ui/PersistenceTab.java).
2. Inside [`ThreadSafeCache.getAllEntries()`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java#L190), a fast copy of immutable [`CacheEntrySnapshot`](src/main/java/com/cse309/cache/core/CacheEntrySnapshot.java) records is made under the lock and the lock is instantly released.
3. A background JavaFX `Task<DbSnapshot>` runs outside the UI thread, delegating to [`SnapshotRepository.save()`](src/main/java/com/cse309/cache/persistence/SnapshotRepository.java#L45).
4. [`DatabaseManager`](src/main/java/com/cse309/cache/persistence/DatabaseManager.java) opens a dedicated SQLite connection with `PRAGMA journal_mode = WAL;`.
5. An atomic transaction begins (`conn.setAutoCommit(false)`), writes the snapshot metadata, executes batch inserts for all entries via `PreparedStatement.addBatch()`, and calls `conn.commit()`. If an error occurs, a nested catch executes `conn.rollback()`.

---

## 4. Technical Pros, Cons & Inherent Trade-offs

| Aspect | Architectural Choice | Advantages (Pros) | Limitations (Cons / Trade-offs) |
|---|---|---|---|
| **Primary Storage** | Exclusively in RAM (`HashMap` + index) | Sub-microsecond latency ($< 1\,\mu\text{s}$); zero disk/network I/O on hot path. | Volatile memory; data is lost if process terminates without taking a snapshot. |
| **Concurrency Locking** | Single private monitor lock (`synchronized`) | Mathematically deadlock-free (leaf lock); eliminates complexity; ensures atomic consistency between entries and eviction queues. | Limits throughput scalability under massive multi-core write loads compared to lock striping. |
| **Eviction Tracking** | Logical ticks (`long lastAccessTick`) | Deterministic ordering; eliminates tie-break ambiguities caused by multiple ops in the same millisecond. | Requires incrementing a sequence counter on every write/read operation. |
| **Durable Persistence** | Out-of-band SQLite WAL mode | Zero hot-path latency penalty; durable point-in-time recovery; standard SQL queryability; zero credentials. | State changes made after a snapshot was saved are not captured (point-in-time snapshot, not write-through). |
| **Binary File Format** | Custom `.jcs` via `DataOutputStream` | Ultra-fast I/O; compact binary layout; tamper-evident CRC32 checksum; immune to Java deserialization security exploits. | Proprietary binary format readable only by this application. |
| **UI Updates** | 250ms polling `Timeline` | Prevents JavaFX thread starvation during 100k+ ops/sec simulations; keeps CPU usage below 1%. | Metrics in the GUI refresh every 250ms rather than instantly per operation. |

---

## 5. Advanced Java Syllabus Mapping (CSE 309 Units 1–5)

Every topic below is implemented in real, reachable code:

| Unit & Focus Topic | Core Java Feature | Exact File & Code Location |
|---|---|---|
| **Unit 1: Classes & OOP** | Constructor chaining via `this(...)` | [`CacheConfiguration.java`](src/main/java/com/cse309/cache/config/CacheConfiguration.java#L35-L42) |
| **Unit 1: Immutability** | `final` class and `final` fields | [`CacheEntrySnapshot.java`](src/main/java/com/cse309/cache/core/CacheEntrySnapshot.java#L15-L25) |
| **Unit 1: Skeletal Architecture** | Generic interface and abstract class | [`Cache.java`](src/main/java/com/cse309/cache/core/Cache.java), [`AbstractCache.java`](src/main/java/com/cse309/cache/core/AbstractCache.java) |
| **Unit 1: Encapsulation** | All 4 access specifiers used intentionally | `public` API in [`Cache.java`](src/main/java/com/cse309/cache/core/Cache.java), `protected` hooks in [`AbstractCache.java`](src/main/java/com/cse309/cache/core/AbstractCache.java), package-private [`CacheEntry.java`](src/main/java/com/cse309/cache/core/CacheEntry.java), `private` lock in [`ThreadSafeCache.java`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java) |
| **Unit 2: Polymorphism** | Compile-time overloading | `put(K, V)` and `put(K, V, Duration)` in [`ThreadSafeCache.java`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java#L58-L69) |
| **Unit 2: Dynamic Dispatch** | Runtime polymorphism across policies | Abstract reference [`EvictionPolicy<K>`](src/main/java/com/cse309/cache/eviction/EvictionPolicy.java) dynamically invoking LRU, FIFO, or LFU |
| **Unit 2: Custom Exceptions** | Exception hierarchy rooted at checked base | [`CacheException.java`](src/main/java/com/cse309/cache/exception/CacheException.java) providing both `getMessage()` and user-friendly `getUserMessage()` |
| **Unit 2: Exception Handling** | Multi-catch & nested try/rollback | Multi-catch in [`SnapshotFileManager.java`](src/main/java/com/cse309/cache/persistence/SnapshotFileManager.java#L130), nested try rollback in [`SnapshotRepository.java`](src/main/java/com/cse309/cache/persistence/SnapshotRepository.java#L85-L95) |
| **Unit 2: Generics & Wildcards** | Bounded wildcards (PECS) & bounded generic methods | `putAll(Map<? extends K, ? extends V>)` in [`Cache.java`](src/main/java/com/cse309/cache/core/Cache.java), `<T extends Comparable<? super T>>` in [`CollectionTools.java`](src/main/java/com/cse309/cache/core/CollectionTools.java#L18) |
| **Unit 3: Multithreading** | `Thread` subclass & `Runnable` task | [`WorkerThread.java`](src/main/java/com/cse309/cache/threading/WorkerThread.java#L16), [`OperationProducerTask.java`](src/main/java/com/cse309/cache/simulation/OperationProducerTask.java#L14) |
| **Unit 3: Inter-Thread Comms** | Monitor synchronization with `wait()` in `while` loop & `notifyAll()` | [`BoundedBuffer.java`](src/main/java/com/cse309/cache/threading/BoundedBuffer.java#L30-L55), [`PauseGate.java`](src/main/java/com/cse309/cache/threading/PauseGate.java#L20-L40) |
| **Unit 3: Thread States & Joins** | Thread lifecycle inspection & cooperative termination | `Thread.getState()` in [`ThreadStateMonitor.java`](src/main/java/com/cse309/cache/threading/ThreadStateMonitor.java#L25), `worker.join(timeout)` in [`WorkerPool.java`](src/main/java/com/cse309/cache/threading/WorkerPool.java#L65) |
| **Unit 3: Atomic Variables** | Hardware CAS counters outside monitor lock | `AtomicLong` counters for requests, hits, misses, evictions in [`CacheStatistics.java`](src/main/java/com/cse309/cache/stats/CacheStatistics.java#L18-L32) |
| **Unit 3: Byte Streams I/O** | Chained binary byte streams with CRC32 | `FileOutputStream` $\to$ `BufferedOutputStream` $\to$ `DataOutputStream` with `java.util.zip.CRC32` in [`SnapshotFileManager.java`](src/main/java/com/cse309/cache/persistence/SnapshotFileManager.java#L40-L75) |
| **Unit 4: JDBC & Persistence** | PreparedStatements, batching, WAL mode, transactions | [`DatabaseManager.java`](src/main/java/com/cse309/cache/persistence/DatabaseManager.java#L42-L48), [`SnapshotRepository.java`](src/main/java/com/cse309/cache/persistence/SnapshotRepository.java#L60-L90) |
| **Unit 4: Collections Framework** | `LinkedHashSet`, `TreeSet`, `HashMap`, `LinkedList`, `ArrayList` | LRU/FIFO in [`LeastRecentlyUsedPolicy.java`](src/main/java/com/cse309/cache/eviction/LeastRecentlyUsedPolicy.java#L16), LFU/TTL in [`LeastFrequentlyUsedPolicy.java`](src/main/java/com/cse309/cache/eviction/LeastFrequentlyUsedPolicy.java#L21) and [`ExpiryIndex.java`](src/main/java/com/cse309/cache/expiry/ExpiryIndex.java#L18) |
| **Unit 5: JavaFX Desktop Architecture** | Scene graph, asynchronous `Task`, throttled `Timeline` | [`CacheApplication.java`](src/main/java/com/cse309/cache/ui/CacheApplication.java#L100-L150), 250ms polling `Timeline` at [`CacheApplication.java`](src/main/java/com/cse309/cache/ui/CacheApplication.java#L320-L350) |
| **Unit 5: Visual Transforms & Effects** | `DropShadow`, `Glow`, `Rotate`, `Scale` | Applied to dashboard metric cards and activity spinners in [`DashboardTab.java`](src/main/java/com/cse309/cache/ui/DashboardTab.java#L75-L90) |

---

## 6. Complete File-by-File Walkthrough (All 77 Production Files)

### Package: `com.cse309.cache.app`
- [`AppPaths.java`](src/main/java/com/cse309/cache/app/AppPaths.java): Resolves standardized absolute filesystem paths for database files (`data/cache.db`), snapshots, and configuration files.
- [`CacheManager.java`](src/main/java/com/cse309/cache/app/CacheManager.java): Composition root and facade coordinating cache engine, expiration daemon, persistence coordinator, and simulation controller with graceful shutdown.

### Package: `com.cse309.cache.config`
- [`CacheConfiguration.java`](src/main/java/com/cse309/cache/config/CacheConfiguration.java): Immutable configuration record with nested `Builder` and constructor chaining via `this(...)`.
- [`ConfigurationStore.java`](src/main/java/com/cse309/cache/config/ConfigurationStore.java): Persists and loads `config.properties` using standard Java `Properties` I/O streams with default fallback.
- [`ConfigurationValidator.java`](src/main/java/com/cse309/cache/config/ConfigurationValidator.java): Validates all system configuration parameters and collects violations into a single exception.

### Package: `com.cse309.cache.core`
- [`AbstractCache.java`](src/main/java/com/cse309/cache/core/AbstractCache.java): Skeletal base class implementing default convenience methods (`isEmpty`, `putWithoutExpiry`) and listener management.
- [`Cache.java`](src/main/java/com/cse309/cache/core/Cache.java): Core generic cache interface declaring the full API contract (`get`, `put`, `update`, `remove`, `search`, `clear`).
- [`CacheDefaults.java`](src/main/java/com/cse309/cache/core/CacheDefaults.java): Central repository of static constant defaults (capacity, TTL, intervals, buffer sizes).
- [`CacheEntry.java`](src/main/java/com/cse309/cache/core/CacheEntry.java): Package-private mutable internal container storing value payload, creation time, expiration, access count, and sequence ticks.
- [`CacheEntrySnapshot.java`](src/main/java/com/cse309/cache/core/CacheEntrySnapshot.java): Immutable value object exposing entry metadata safely to UI components and persistence repositories.
- [`CacheFactory.java`](src/main/java/com/cse309/cache/core/CacheFactory.java): Static factory class creating thread-safe cache instances and specialized `StringCache` engines.
- [`CacheLookupResult.java`](src/main/java/com/cse309/cache/core/CacheLookupResult.java): Rich return object for lookup operations indicating status (`HIT`, `MISS`, `EXPIRED`) and entry snapshot.
- [`CacheWriteResult.java`](src/main/java/com/cse309/cache/core/CacheWriteResult.java): Return object for write operations indicating status (`INSERTED`, `REPLACED`) and a list of triggered eviction records.
- [`CollectionTools.java`](src/main/java/com/cse309/cache/core/CollectionTools.java): Utility class containing bounded generic methods (`<T extends Comparable<? super T>>`) for collection analysis.
- [`StringCache.java`](src/main/java/com/cse309/cache/core/StringCache.java): String-specialized concrete cache subclass enforcing string length limits on keys (250 chars) and values (8192 chars).
- [`ThreadSafeCache.java`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java): Primary engine implementation synchronizing all storage, eviction, and expiration operations under a single private lock.

### Package: `com.cse309.cache.event`
- [`CacheEvent.java`](src/main/java/com/cse309/cache/event/CacheEvent.java): Event object encapsulating cache lifecycle events (inserts, updates, lookups, evictions, expirations, clears).
- [`CacheEventAdapter.java`](src/main/java/com/cse309/cache/event/CacheEventAdapter.java): Adapter class providing default empty implementations of `CacheEventListener` to simplify subclassing.
- [`CacheEventListener.java`](src/main/java/com/cse309/cache/event/CacheEventListener.java): Observer listener interface defining event callback methods.

### Package: `com.cse309.cache.eviction`
- [`AbstractEvictionPolicy.java`](src/main/java/com/cse309/cache/eviction/AbstractEvictionPolicy.java): Base policy class implementing shared victim description formatting and policy naming.
- [`EvictionPolicy.java`](src/main/java/com/cse309/cache/eviction/EvictionPolicy.java): Generic algorithm interface defining `onInsert`, `onAccess`, `onRemove`, `selectVictim`, and `rebuild`.
- [`EvictionPolicyType.java`](src/main/java/com/cse309/cache/eviction/EvictionPolicyType.java): Enum declaring the three supported algorithms: `LRU`, `FIFO`, and `LFU`.
- [`EvictionRecord.java`](src/main/java/com/cse309/cache/eviction/EvictionRecord.java): Immutable record capturing eviction metadata (evicted key, active policy, timestamp, eviction reason).
- [`FirstInFirstOutPolicy.java`](src/main/java/com/cse309/cache/eviction/FirstInFirstOutPolicy.java): $O(1)$ FIFO eviction policy maintaining insertion order using a `LinkedHashSet`.
- [`LeastFrequentlyUsedPolicy.java`](src/main/java/com/cse309/cache/eviction/LeastFrequentlyUsedPolicy.java): $O(\log n)$ LFU eviction policy ordering nodes in a `TreeSet` by access frequency and insertion sequence with a `HashMap` index.
- [`LeastRecentlyUsedPolicy.java`](src/main/java/com/cse309/cache/eviction/LeastRecentlyUsedPolicy.java): $O(1)$ LRU eviction policy updating recency on access using a `LinkedHashSet`.

### Package: `com.cse309.cache.exception`
- [`CacheConfigurationException.java`](src/main/java/com/cse309/cache/exception/CacheConfigurationException.java): Checked exception thrown when configuration parameters fail validation rules.
- [`CacheException.java`](src/main/java/com/cse309/cache/exception/CacheException.java): Root checked exception providing technical diagnostic message and plain-English `getUserMessage()`.
- [`CacheKeyNotFoundException.java`](src/main/java/com/cse309/cache/exception/CacheKeyNotFoundException.java): Checked exception thrown when an `update()` operation targets a non-existent key.
- [`CachePersistenceException.java`](src/main/java/com/cse309/cache/exception/CachePersistenceException.java): Checked exception wrapping low-level JDBC or I/O stream failures with root causes attached.
- [`InvalidCacheCapacityException.java`](src/main/java/com/cse309/cache/exception/InvalidCacheCapacityException.java): Checked exception thrown when requested capacity is outside allowed bounds (1 to 1,000,000).
- [`InvalidCacheOperationException.java`](src/main/java/com/cse309/cache/exception/InvalidCacheOperationException.java): Checked exception thrown on null/blank arguments or invalid simulator states.
- [`SnapshotFormatException.java`](src/main/java/com/cse309/cache/exception/SnapshotFormatException.java): Checked exception thrown when a binary `.jcs` file has invalid magic bytes, version, or checksum.

### Package: `com.cse309.cache.expiry`
- [`CacheExpirationService.java`](src/main/java/com/cse309/cache/expiry/CacheExpirationService.java): Background daemon thread executing timed monitor waits and periodically invoking `purgeExpired()`.
- [`ExpiryIndex.java`](src/main/java/com/cse309/cache/expiry/ExpiryIndex.java): Sorted index maintaining expiring entries in a `TreeSet` ordered by expiration epoch timestamp.

### Package: `com.cse309.cache.history`
- [`OperationHistory.java`](src/main/java/com/cse309/cache/history/OperationHistory.java): Thread-safe bounded in-memory circular list using a `LinkedList` for auditing recent operations.
- [`OperationRecord.java`](src/main/java/com/cse309/cache/history/OperationRecord.java): Immutable record capturing operation type, thread name, timestamp, key, outcome, and latency.

### Package: `com.cse309.cache.logging`
- [`AppLogging.java`](src/main/java/com/cse309/cache/logging/AppLogging.java): Configures `java.util.logging` with console output, rotating file logs, and in-memory log buffer.
- [`InMemoryLogHandler.java`](src/main/java/com/cse309/cache/logging/InMemoryLogHandler.java): Custom log handler capturing the last 500 log records in memory for UI display.
- [`SingleLineFormatter.java`](src/main/java/com/cse309/cache/logging/SingleLineFormatter.java): Formats log output into a clean single-line string with timestamps, levels, and thread names.

### Package: `com.cse309.cache.persistence`
- [`DatabaseManager.java`](src/main/java/com/cse309/cache/persistence/DatabaseManager.java): Manages SQLite connections, enables WAL mode, sets busy timeouts, and executes idempotent schema migrations.
- [`DbSnapshot.java`](src/main/java/com/cse309/cache/persistence/DbSnapshot.java): Entity model representing a point-in-time snapshot row stored in SQLite.
- [`OperationHistoryRepository.java`](src/main/java/com/cse309/cache/persistence/OperationHistoryRepository.java): JDBC repository persisting historical operation records to SQLite with batch execution.
- [`PersistenceCoordinator.java`](src/main/java/com/cse309/cache/persistence/PersistenceCoordinator.java): Coordinates asynchronous write-behind queues and snapshot backup/restoration workflows.
- [`PersistenceMode.java`](src/main/java/com/cse309/cache/persistence/PersistenceMode.java): Enum declaring operational modes: `IN_MEMORY_ONLY` and `PERSISTENT`.
- [`PersistenceStatus.java`](src/main/java/com/cse309/cache/persistence/PersistenceStatus.java): Immutable status object reporting database connectivity, active mode, and last encountered error.
- [`Repository.java`](src/main/java/com/cse309/cache/persistence/Repository.java): Generic data access interface declaring standard CRUD operations (`save`, `findById`, `findAll`, `deleteById`).
- [`SnapshotFileManager.java`](src/main/java/com/cse309/cache/persistence/SnapshotFileManager.java): Serializes and deserializes binary `.jcs` snapshot files using `DataOutputStream` and CRC32 verification.
- [`SnapshotRepository.java`](src/main/java/com/cse309/cache/persistence/SnapshotRepository.java): JDBC repository managing snapshot metadata and entry records in SQLite with transactional rollback.
- [`StringValueCodec.java`](src/main/java/com/cse309/cache/persistence/StringValueCodec.java): Identity value codec encoding and decoding `String` values for database storage.
- [`ValueCodec.java`](src/main/java/com/cse309/cache/persistence/ValueCodec.java): Generic codec interface defining serialization contracts (`encode`, `decode`) for persistent values.

### Package: `com.cse309.cache.simulation`
- [`KeyDistribution.java`](src/main/java/com/cse309/cache/simulation/KeyDistribution.java): Enum defining key access patterns: `UNIFORM`, `HOTSPOT_80_20`, and `SEQUENTIAL_SCAN`.
- [`OperationGenerator.java`](src/main/java/com/cse309/cache/simulation/OperationGenerator.java): Generates planned operations based on selected mix percentages, distributions, and pseudo-random seeds.
- [`OperationMix.java`](src/main/java/com/cse309/cache/simulation/OperationMix.java): Model defining percentages for GET, PUT, UPDATE, DELETE, and SEARCH operations.
- [`OperationProducerTask.java`](src/main/java/com/cse309/cache/simulation/OperationProducerTask.java): `Runnable` producer task generating planned operations and inserting them into the bounded buffer.
- [`PlannedOperation.java`](src/main/java/com/cse309/cache/simulation/PlannedOperation.java): Immutable task specification containing operation type, key, payload, and optional TTL.
- [`SimulationConfig.java`](src/main/java/com/cse309/cache/simulation/SimulationConfig.java): Configuration model capturing worker counts, total operations, mixes, and random seeds.
- [`SimulationController.java`](src/main/java/com/cse309/cache/simulation/SimulationController.java): Manages simulation lifecycles (start, pause, resume, stop) and coordinates worker pool execution.
- [`SimulationProgress.java`](src/main/java/com/cse309/cache/simulation/SimulationProgress.java): Immutable telemetry snapshot exposing completed operations, throughput, elapsed time, and worker states.

### Package: `com.cse309.cache.stats`
- [`CacheStatistics.java`](src/main/java/com/cse309/cache/stats/CacheStatistics.java): Manages thread-safe performance counters using `AtomicLong` (requests, hits, misses, evictions, latencies).
- [`LatencyHistogram.java`](src/main/java/com/cse309/cache/stats/LatencyHistogram.java): Categorizes operation latencies into discrete logarithmic microsecond buckets (`<1µs`, `<10µs`, `<100µs`, etc.).
- [`StatisticsExporter.java`](src/main/java/com/cse309/cache/stats/StatisticsExporter.java): Exports snapshot statistics and latency distributions into CSV reports.
- [`StatisticsSnapshot.java`](src/main/java/com/cse309/cache/stats/StatisticsSnapshot.java): Immutable snapshot capturing point-in-time hit ratios, throughput, and utilization metrics.

### Package: `com.cse309.cache.threading`
- [`BoundedBuffer.java`](src/main/java/com/cse309/cache/threading/BoundedBuffer.java): Thread-safe circular buffer implementing producer-consumer synchronization with `wait()` and `notifyAll()`.
- [`PauseGate.java`](src/main/java/com/cse309/cache/threading/PauseGate.java): Concurrency synchronization barrier allowing external threads to pause and resume worker threads cleanly.
- [`ThreadStateMonitor.java`](src/main/java/com/cse309/cache/threading/ThreadStateMonitor.java): Inspects live execution states (`Thread.getState()`) of all simulation worker threads.
- [`WorkerPool.java`](src/main/java/com/cse309/cache/threading/WorkerPool.java): Spawns, names, assigns priorities to, and joins simulation worker threads.
- [`WorkerThread.java`](src/main/java/com/cse309/cache/threading/WorkerThread.java): Custom `Thread` subclass consuming tasks from `BoundedBuffer` and executing them against the cache engine.

### Package: `com.cse309.cache.ui`
- [`CacheApplication.java`](src/main/java/com/cse309/cache/ui/CacheApplication.java): Main JavaFX desktop application class assembling the 4-tab interface, menus, toolbar, and 250ms polling `Timeline`.
- [`DashboardTab.java`](src/main/java/com/cse309/cache/ui/DashboardTab.java): Tab 1 rendering real-time metric cards, hit-ratio trend line chart, live eviction log, and visual transforms.
- [`IconFactory.java`](src/main/java/com/cse309/cache/ui/IconFactory.java): Procedurally draws 16x16 pixel icons in Java code on a `Canvas` and renders them via `ImageView`.
- [`InputValidator.java`](src/main/java/com/cse309/cache/ui/InputValidator.java): Validates user inputs on UI forms and returns friendly error messages without raising uncaught exceptions.
- [`Launcher.java`](src/main/java/com/cse309/cache/ui/Launcher.java): Standard `main(String[] args)` bootstrap class invoking `Application.launch(CacheApplication.class)` without classpath issues.
- [`OperationsTab.java`](src/main/java/com/cse309/cache/ui/OperationsTab.java): Tab 2 providing interactive CRUD forms (Put, Get, Update, Delete, Clear), latency badges, and integrated Eviction Policy/Capacity tuning.
- [`PersistenceTab.java`](src/main/java/com/cse309/cache/ui/PersistenceTab.java): Tab 4 managing point-in-time SQLite snapshots (warm-start save/restore), `.jcs` binary file exports, and CSV dumps.
- [`SimulatorTab.java`](src/main/java/com/cse309/cache/ui/SimulatorTab.java): Tab 3 controlling multi-threaded concurrency workloads, displaying worker thread states, throughput, and progress.
- [`UiEventBuffer.java`](src/main/java/com/cse309/cache/ui/UiEventBuffer.java): Thread-safe circular event buffer decoupling the high-speed cache engine from the JavaFX UI rendering loop.

---

## 7. Viva Preparation: 10 Likely Examiner Questions & Direct Answers

### Q1: Why did you use a single private `synchronized` lock instead of a `ReentrantReadWriteLock` for the cache engine?
**Answer**: In an eviction-based cache, a `get(key)` operation is not a pure read. Under LRU, `get()` updates the entry's position in the recency queue; under LFU, `get()` increments the frequency counter and reorders the `TreeSet`. Because every read mutates internal eviction metadata, a `ReentrantReadWriteLock` would have to acquire the exclusive write lock on every single `get()` anyway, completely eliminating the benefit of read locks while adding synchronization overhead. Using a private monitor lock `synchronized (lock)` in [`ThreadSafeCache.java`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java) is simpler, faster, and provably deadlock-free.

### Q2: How do you prove that your cache engine is mathematically free from deadlocks?
**Answer**: Deadlock requires circular wait. Our cache engine follows strict **leaf-lock discipline**: while holding the private monitor lock in [`ThreadSafeCache.java`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java), the engine performs purely in-memory operations. It never invokes external listener callbacks, never performs filesystem or database I/O, never sleeps, and never attempts to acquire any secondary lock. Because a thread holding the engine lock never waits on any other resource or lock, circular wait is impossible.

### Q3: Why is `wait()` called inside a `while` loop rather than an `if` statement in `BoundedBuffer`?
**Answer**: In [`BoundedBuffer.java`](src/main/java/com/cse309/cache/threading/BoundedBuffer.java#L35), `wait()` is placed inside `while (count == buffer.length)` and `while (count == 0)`. This protects against **spurious wakeups** (where the operating system wakes a thread without a corresponding signal) and **state changes by competing threads**. In a multi-consumer setup, when `notifyAll()` wakes several threads, the first thread to acquire the monitor consumes the available item. If subsequent threads woke up from an `if` block, they would proceed with an empty buffer, causing corrupted state or array bounds exceptions. The `while` loop forces threads to re-verify the condition before executing.

### Q4: Why did you use `DataOutputStream` instead of standard Java Object Serialization (`ObjectOutputStream`) for `.jcs` snapshots?
**Answer**: Standard Java serialization (`ObjectOutputStream`/`ObjectInputStream`) is a well-documented security hazard vulnerable to arbitrary remote code execution via deserialization gadget chains, and it suffers from version fragility and payload bloat. In [`SnapshotFileManager.java`](src/main/java/com/cse309/cache/persistence/SnapshotFileManager.java#L45), we write explicit primitive byte streams with a known magic header (`0x4A435331`), format versioning, explicit UTF strings, and an end-of-file CRC32 checksum. This provides safe, tamper-evident, high-performance serialization.

### Q5: How does your background expiration daemon avoid scanning the entire cache map on every cleanup sweep?
**Answer**: Rather than iterating over all $N$ entries in the map ($O(N)$), the engine maintains a sorted [`ExpiryIndex`](src/main/java/com/cse309/cache/expiry/ExpiryIndex.java) backed by a `TreeSet` ordered by `(expiresAt, insertionSequence)`. In [`ThreadSafeCache.purgeExpired()`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java#L230), [`CacheExpirationService`](src/main/java/com/cse309/cache/expiry/CacheExpirationService.java) inspects only the earliest expiring items from the head of the tree. If the first item has not expired, no subsequent items can be expired, so the sweep terminates immediately in $O(1)$ time. If items are expired, they are removed in $O(k)$ time where $k$ is the number of expired items.

### Q6: What happens when the cache capacity is reduced while the cache contains more items than the new limit?
**Answer**: If capacity is reduced from $C_1$ to $C_2$ where current size $S > C_2$, the excess $S - C_2$ entries must be evicted immediately and atomically under the lock. In [`ThreadSafeCache.setCapacity()`](src/main/java/com/cse309/cache/core/ThreadSafeCache.java#L160), the active eviction policy selects victims, removes them, and records an [`EvictionRecord`](src/main/java/com/cse309/cache/eviction/EvictionRecord.java) with reason `CAPACITY_REDUCED`. In the GUI ([`OperationsTab.java`](src/main/java/com/cse309/cache/ui/OperationsTab.java#L315)), `cache.previewCapacityChange()` is called first to display a confirmation dialog showing the user the exact number of entries that will be evicted before applying the change.

### Q7: Why are statistics counters implemented using `AtomicLong` instead of synchronized variables?
**Answer**: Performance counters (total requests, hits, misses, evictions) in [`CacheStatistics.java`](src/main/java/com/cse309/cache/stats/CacheStatistics.java) are updated outside the engine lock. `AtomicLong` utilizes low-level CPU Compare-And-Swap (CAS) instructions to perform atomic increments without lock contention or thread context switches. This ensures that recording performance telemetry never becomes a bottleneck for the high-speed cache engine.

### Q8: How does the desktop UI remain responsive without freezing during a high-speed 100,000-operation simulation?
**Answer**: If worker threads called `Platform.runLater()` for each cache operation, hundreds of thousands of events would overwhelm the JavaFX event queue, freezing the UI. In [`CacheApplication.java`](src/main/java/com/cse309/cache/ui/CacheApplication.java#L320), responsiveness is guaranteed through three decoupling rules: 1) Simulation workers run on native background threads; 2) Events enter a bounded circular buffer ([`UiEventBuffer`](src/main/java/com/cse309/cache/ui/UiEventBuffer.java)); and 3) A single JavaFX `Timeline` ticks every 250 milliseconds to pull immutable [`StatisticsSnapshot`](src/main/java/com/cse309/cache/stats/StatisticsSnapshot.java) and [`SimulationProgress`](src/main/java/com/cse309/cache/simulation/SimulationProgress.java) data in batch, keeping UI CPU usage below 1%.

### Q9: What is the Delegation Event Model, and how is it reflected in your architecture?
**Answer**: The Delegation Event Model decouples event generation from event handling through three components: the Event Source, the Event Object, and the Event Listener. In our cache engine, this pattern is mirrored by [`AbstractCache`](src/main/java/com/cse309/cache/core/AbstractCache.java) (Event Source), [`CacheEvent`](src/main/java/com/cse309/cache/event/CacheEvent.java) (Event Object), and [`CacheEventListener`](src/main/java/com/cse309/cache/event/CacheEventListener.java) / [`CacheEventAdapter`](src/main/java/com/cse309/cache/event/CacheEventAdapter.java) (Event Listeners), keeping engine mutations completely decoupled from UI display updates.

### Q10: How are atomic database transactions implemented in SQLite without risking partial writes?
**Answer**: In [`SnapshotRepository.java`](src/main/java/com/cse309/cache/persistence/SnapshotRepository.java#L65-L95), auto-commit is disabled using `connection.setAutoCommit(false)`. The snapshot header is inserted, and all cache entry records are batched using `statement.addBatch()` and `statement.executeBatch()`. If all operations succeed, `connection.commit()` commits the transaction permanently. If any `SQLException` is thrown, a nested catch block invokes `connection.rollback()` to discard partial changes before wrapping the failure in a [`CachePersistenceException`](src/main/java/com/cse309/cache/exception/CachePersistenceException.java). Finally, `setAutoCommit(true)` is restored in a `finally` block.
