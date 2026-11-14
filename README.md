# Multithreaded Java-Based In-Memory Caching System with Concurrent Access Management

**Course**: CSE 309 Advanced Java Programming  
**Author**: Shivangi Sharma  
**Platform**: Java 17+ (LTS), OpenJFX 21, SQLite JDBC, JUnit 5, Apache Maven  

---

## 1. Title, Introduction, Problem Statement, Objectives, and Proposed Solution

### 1.1 Project Title
**Multithreaded Java-Based In-Memory Caching System with Concurrent Access Management**

### 1.2 Introduction & Problem Statement
In modern computing architectures, database queries and secondary storage operations are orders of magnitude slower than CPU execution speeds. High-throughput distributed applications, web servers, and transactional financial systems face severe performance bottlenecks if every read and write request touches relational databases or remote APIs. 

To bridge this latency gap, **in-memory caching systems** store hot, frequently accessed key-value pairs directly in Random Access Memory (RAM). However, building an in-memory cache for multithreaded environments introduces complex engineering challenges:
1. **Thread Safety & Race Conditions**: Unsynchronized concurrent mutations result in corrupted data structures, lost updates, and memory leaks.
2. **Deadlocks**: Complex locking strategies involving callbacks or nested locks can stall application threads permanently.
3. **Cache Eviction**: Finite memory limits require deterministic, computationally efficient (\(O(1)\) or \(O(\log n)\)) eviction policies (LRU, FIFO, LFU) to prune cold entries when capacity is reached.
4. **Time-To-Live (TTL) Expiration**: Stale entries must expire accurately without wasting CPU cycles on full cache table scans.
5. **Durable Persistence**: Cache state must survive system restarts without placing slow disk I/O on the latency-sensitive critical path.

### 1.3 Objectives
- Strictly demonstrate and satisfy all concepts across **Units 1 through 5 of the CSE 309 Advanced Java Programming syllabus**.
- Deliver a production-grade, thread-safe in-memory cache engine with sub-microsecond lookup latency.
- Implement three independent, pluggable eviction policies: **LRU (\(O(1)\))**, **FIFO (\(O(1)\))**, and **LFU (\(O(\log n)\))**.
- Provide deterministic wall-clock TTL expiration using an active background daemon thread and lazy eviction on access.
- Guarantee deadlock freedom through strict leaf-lock discipline and non-blocking asynchronous event delivery.
- Implement dual persistence: durable SQLite WAL transactions and portable custom binary `.jcs` file serialization with CRC32 integrity checks.
- Expose a desktop GUI built with **JavaFX** featuring 9 dedicated functional tabs, responsive background task scheduling, and real-time chart polling.
- Provide an isolated out-of-process **AWT Event Console** demonstrating the Delegation Event Model, and an interactive **Concepts Lab** for educational concurrency and collections exploration.

### 1.4 Proposed Solution Architecture
Our solution implements a layered architecture where **RAM is the exclusive primary cache** and persistence mechanisms run out-of-band. The engine is encapsulated behind [`Cache<K,V>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/Cache.java) and implemented by [`ThreadSafeCache<K,V>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/ThreadSafeCache.java) using a single private monitor lock. Workloads are simulated using a custom producer-consumer pipeline built with [`BoundedBuffer<T>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/BoundedBuffer.java), [`PauseGate`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/PauseGate.java), and [`WorkerThread`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/WorkerThread.java) instances. The desktop interface communicates solely through the composition root [`CacheManager`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/app/CacheManager.java) and immutable snapshot records.

---

## 2. Features and Technology Stack

### 2.1 Core Feature Highlights
- **O(1) Least Recently Used (LRU)** eviction via custom doubly-linked `LinkedHashSet`.
- **O(1) First-In-First-Out (FIFO)** eviction tracking chronological insertion sequence.
- **O(log n) Least Frequently Used (LFU)** eviction via sorted `TreeSet` of `(frequency, sequence)` pairs with `HashMap` index.
- **Wall-Clock TTL & Expiry Daemon**: Background daemon thread [`CacheExpirationService`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/expiry/CacheExpirationService.java) purging expired entries from an ordered `TreeSet` index.
- **Atomic Dynamic Reconfiguration**: Online eviction policy switching with full metadata rebuilding and capacity reduction with instant excess eviction.
- **Robust Workload Simulator**: Configurable producer-consumer system supporting 1 to 64 threads, uniform/hotspot/sequential key distributions, customizable read/write mixes, and thread priority experiments.
- **Dual Persistence Subsystem**: Out-of-band SQLite repository (WAL mode, transactions, rollback) and portable binary `.jcs` file format with magic headers and CRC32 verification.
- **JavaFX Desktop GUI**: 9 interactive tabs with visual effects (`DropShadow`, `Glow`, `Scale`, `Rotate`), procedural 16x16 vector icons via Canvas, and 250ms throttled metric polling.
- **AWT Event Console**: Standalone utility demonstrating `ActionListener`, `KeyListener`, `KeyAdapter`, and `WindowAdapter`.
- **Educational Concepts Lab**: Visual demonstrations of race conditions (`UnsafeCounterBox` vs `SafeCounterBox`), collections benchmarks, fail-fast iterators, and generic multi-type caches.

### 2.2 Technology Stack
- **Language**: Java 17+ (compiled with `--release 17` and `-Xlint:all`).
- **Build System**: Apache Maven 3.9.9.
- **Runtime Dependencies**:
  - `org.openjfx:javafx-controls` (v21.0.5) — Scene graph, UI controls, and charts.
  - `org.xerial:sqlite-jdbc` (v3.47.1.0) — SQLite embedded database driver.
- **Testing Framework**:
  - `org.junit.jupiter:junit-jupiter` (v5.11.3) — Modern unit testing.

### 2.3 Labelled Beyond-Syllabus Enrichments
In strict adherence to the project specification, the following modern Java features are utilized beyond the standard CSE 309 syllabus and explicitly labelled as **enrichment**:
1. `java.util.concurrent.atomic.AtomicLong` & `AtomicInteger` — High-performance lock-free performance counters.
2. `java.time` (`Clock`, `Instant`, `Duration`) — Wall-clock epoch time representation and testable clock injection.
3. `java.util.logging` — Enterprise structured application logging with rotating file handlers.
4. `java.nio.file.Files` & `Path` — Atomic filesystem operations (`ATOMIC_MOVE`).
5. `java.util.zip.CRC32` — Tamper-evident checksumming for snapshot persistence.
6. JavaFX `Task`, `Service`, `Timeline` & Charts — Background asynchronous execution and real-time visualization.

---

## 3. Architecture & Package Structure

### 3.1 Architectural Package Layout
```
com.cse309.cache
├── app          CacheManager (facade & composition root), AppPaths
├── awt          AwtEventConsole (standalone process, Delegation Event Model)
├── benchmark    BenchmarkRunner, BenchmarkResult
├── config       CacheConfiguration (+ nested Builder), ConfigurationValidator, ConfigurationStore
├── core         Cache<K,V>, AbstractCache, ThreadSafeCache, StringCache, CacheEntry,
│                CacheEntrySnapshot, CacheLookupResult, CacheWriteResult, CacheFactory, CacheDefaults
├── event        CacheEvent, CacheEventListener, CacheEventAdapter
├── eviction     EvictionPolicy<K>, AbstractEvictionPolicy<K>, LeastRecentlyUsedPolicy,
│                FirstInFirstOutPolicy, LeastFrequentlyUsedPolicy, EvictionPolicyType, EvictionRecord
├── exception    CacheException (root checked), InvalidCacheOperationException,
│                InvalidCacheCapacityException, CacheKeyNotFoundException, CacheConfigurationException,
│                CachePersistenceException, SnapshotFormatException
├── expiry       CacheExpirationService, ExpiryIndex
├── history      OperationRecord, OperationHistory
├── lab          RaceConditionLab, CollectionsLab, GenericCacheLab, Student
├── logging      AppLogging, SingleLineFormatter, InMemoryLogHandler
├── persistence  DatabaseManager, Repository<T,ID>, SnapshotRepository, OperationHistoryRepository,
│                BenchmarkRepository, SnapshotFileManager, ValueCodec<V>, StringValueCodec,
│                PersistenceCoordinator, PersistenceStatus, PersistenceMode
├── simulation   SimulationConfig, KeyDistribution, OperationMix, OperationGenerator,
│                OperationProducerTask, SimulationController, SimulationProgress
├── stats        CacheStatistics, StatisticsSnapshot, LatencyHistogram, StatisticsExporter
└── ui           Launcher, CacheApplication, IconFactory, InputValidator, UiEventBuffer,
                 DashboardTab, OperationsTab, ExplorerTab, ConfigurationTab, SimulatorTab,
                 StatisticsTab, PersistenceTab, HistoryTab, ConceptsLabTab
```

### 3.2 Layering & Dependency Rules
1. **Unidirectional Dependencies**: UI components depend exclusively on [`CacheManager`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/app/CacheManager.java) and immutable data transfer snapshots. Core engine packages never import `javafx.*`, `java.awt.*`, or `java.sql.*`.
2. **Immutability Boundary**: Any data handed from worker threads or the engine to the UI is an immutable value object (`CacheEntrySnapshot`, `StatisticsSnapshot`, `SimulationProgress`).
3. **Leaf-Lock Isolation**: The cache engine holds no external locks and invokes no callbacks while synchronized.

### 3.3 Major Class Catalog

| Class / Interface | Package | Responsibility |
|---|---|---|
| [`Cache<K,V>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/Cache.java) | `core` | Generic public interface defining complete cache operations. |
| [`AbstractCache<K,V>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/AbstractCache.java) | `core` | Skeletal implementation holding common invariants and listener mechanics. |
| [`ThreadSafeCache<K,V>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/ThreadSafeCache.java) | `core` | Production thread-safe engine governed by a single private monitor lock. |
| [`StringCache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/StringCache.java) | `core` | Non-generic specialization enforcing key (250 char) and value (8192 char) limits. |
| [`LeastRecentlyUsedPolicy<K>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/eviction/LeastRecentlyUsedPolicy.java) | `eviction` | \(O(1)\) LRU eviction tracking access order via `LinkedHashSet`. |
| [`FirstInFirstOutPolicy<K>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/eviction/FirstInFirstOutPolicy.java) | `eviction` | \(O(1)\) FIFO eviction tracking insertion sequence. |
| [`LeastFrequentlyUsedPolicy<K>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/eviction/LeastFrequentlyUsedPolicy.java) | `eviction` | \(O(\log n)\) LFU eviction utilizing a frequency-ordered `TreeSet` with node index. |
| [`CacheExpirationService`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/expiry/CacheExpirationService.java) | `expiry` | Named daemon thread performing timed periodic sweeps of expired keys. |
| [`BoundedBuffer<T>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/BoundedBuffer.java) | `threading` | Generic circular queue with monitor `wait()`/`notifyAll()` synchronization. |
| [`PauseGate`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/PauseGate.java) | `threading` | Synchronization barrier coordinating inter-thread simulation pause and resume. |
| [`WorkerThread`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/WorkerThread.java) | `threading` | Custom `Thread` subclass consuming operations from buffer and running on cache. |
| [`SimulationController`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/simulation/SimulationController.java) | `simulation` | Orchestrates workload generation, thread pool lifecycle, and progress metrics. |
| [`DatabaseManager`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/persistence/DatabaseManager.java) | `persistence` | Manages SQLite connection pooling, WAL mode, foreign keys, and DDL schema. |
| [`SnapshotFileManager`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/persistence/SnapshotFileManager.java) | `persistence` | Reads and writes custom binary `.jcs` format with CRC32 verification. |
| [`CacheManager`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/app/CacheManager.java) | `app` | Central composition root wiring engine, persistence, expiry, and history. |
| [`AwtEventConsole`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/awt/AwtEventConsole.java) | `awt` | Isolated AWT process demonstrating Delegation Event Model with inspector. |

---

## 4. Why the Cache Lives in RAM and What SQLite Is For

### 4.1 In-Memory RAM vs Disk Boundary
An in-memory cache is built to achieve sub-microsecond access times (\(<1\,\mu\text{s}\)) for active data sets. Accessing secondary storage—even modern NVMe SSDs running high-performance embedded SQLite—incurs operating system kernel transitions, filesystem locking, page cache lookups, and serialization overheads measuring between 100 microseconds and several milliseconds.

| Storage Layer | Medium | Typical Read Latency | Role in Our Architecture |
|---|---|---|---|
| **Primary Cache** | Dynamic RAM | **< 1 microsecond** | Active data store. All `get`, `put`, `update`, `remove`, and search calls touch RAM only. |
| **Secondary Persistence** | SQLite (WAL disk) | **200 – 2,000 microseconds** | Out-of-band durable snapshot backup, operation history logging, and benchmark runs. |

### 4.2 SQLite Design Rules
1. **Never on the Hot Path**: No cache operation (`get`, `put`, `update`, `remove`) ever invokes SQLite.
2. **Never Under the Lock**: Database operations are strictly forbidden while holding the engine monitor lock.
3. **Atomic Copy-Then-Write**: When taking a database snapshot, cache entries are copied into an immutable list inside the lock (a microsecond memory copy), the lock is released immediately, and the snapshot is written to SQLite in a single transaction on a background thread.
4. **Resilience to Database Failure**: If SQLite is locked, uninstalled, or corrupted, the cache engine continues operating normally in RAM without interruption.

---

## 5. Algorithms and Worked Examples

### 5.1 Least Recently Used (LRU)
- **Data Structure**: `LinkedHashSet<K>` maintaining access order.
- **Victim Selection**: The head element of the set represents the least recently accessed key (\(O(1)\)).
- **Access Rule**: On every `get()` or `put()`, the key is removed and re-added to the tail of the set (\(O(1)\)).
- **Worked Example**:
  - Capacity: 3.
  - Operations: `put(A)`, `put(B)`, `put(C)`. Order: `[A, B, C]`.
  - Read: `get(A)`. Key `A` moves to most recent. Order: `[B, C, A]`.
  - Insert: `put(D)`. Cache is full; victim at head is `B`. `B` is evicted. Final entries: `[C, A, D]`.

### 5.2 First-In-First-Out (FIFO)
- **Data Structure**: `LinkedHashSet<K>` maintaining strict insertion order.
- **Victim Selection**: The earliest inserted entry at the head of the set (\(O(1)\)).
- **Access Rule**: Reading an entry (`get()`) has zero effect on order. Updating an existing entry keeps its original insertion sequence.
- **Worked Example**:
  - Capacity: 3.
  - Operations: `put(A)`, `put(B)`, `put(C)`. Order: `[A, B, C]`.
  - Read: `get(A)`. Order remains strictly `[A, B, C]`.
  - Insert: `put(D)`. Cache is full; victim at head is `A`. `A` is evicted. Final entries: `[B, C, D]`.
  - Overwrite: `put(B, "newVal")`. `B` updates value but preserves its head position in FIFO order.

### 5.3 Least Frequently Used (LFU)
- **Data Structure**: Sorted `TreeSet` of `LfuNode` objects ordered by `(frequency, insertionSequence)` paired with a `HashMap<K, LfuNode>` index.
- **Victim Selection**: Minimum element from `treeSet.first()` (\(O(1)\) read, \(O(\log n)\) removal).
- **Access Rule**: On access, node is removed from the `TreeSet`, frequency is incremented, and re-inserted into `TreeSet` (\(O(\log n)\)).
- **Tie-Breaking Rule**: When two keys have identical frequency, the one with the smaller `insertionSequence` (earliest inserted) is evicted.
- **Worked Examples**:
  - **Frequency Victory**: Capacity 3. `put(A)`, `put(B)`, `put(C)`. `get(A)` twice (freq=3). `get(B)` once (freq=2). `put(D)`. Victim is `C` (freq=1 vs 3 and 2). `C` is evicted.
  - **Deterministic Tie-Break**: Capacity 3. `put(A)`, `put(B)`, `put(C)` with no reads (all freq=1). `put(D)`. All have frequency 1; tie-breaker selects earliest inserted (`A`). `A` is evicted.

### 5.4 TTL Expiration & Expiry Index
- **Wall-Clock Storage**: Each entry stores `expiresAt = now + ttlMillis` using the injected `Clock`.
- **Expiry Index**: Backed by a `TreeSet` sorted by `(expiresAt, insertionSequence)`.
- **Purge Cost**: \(O(k)\) where \(k\) is the number of expired entries, rather than \(O(n)\) table scans.
- **Lazy + Active Sweep**: Active sweep runs on a daemon thread every \(N\) seconds; lazy check runs on `get()`, `containsKey()`, and `size()`, guaranteeing expired entries are never returned.

---

## 6. Concurrency Design

### 6.1 Single Private Lock Architecture
All mutable engine state (entry map, expiry index, eviction policy, logical tick counters) is guarded by a single private monitor:
```java
private final Object lock = new Object();
```
Every public method that mutates or reads state synchronizes on `lock`.

### 6.2 Why Not `ReentrantReadWriteLock`?
A common misconception is that read-heavy workloads always benefit from read-write locks. In a caching engine implementing LRU or LFU, **`get()` is inherently a write operation** because it modifies eviction metadata (updating recency tick in LRU, incrementing access frequency in LFU). Therefore, `readLock()` cannot be used for `get()`. Every `get()` would require `writeLock()`, eliminating all multi-reader concurrency while adding the substantial CPU overhead of `ReentrantReadWriteLock` state transitions. A single private `synchronized` lock provides maximum throughput and minimal overhead.

### 6.3 Deadlock Freedom Argument (Leaf-Lock Discipline)
Deadlock mathematically requires circular wait. Our engine enforces **leaf-lock discipline**:
1. While holding `lock`, the engine performs strictly in-memory state updates.
2. No secondary locks are ever acquired.
3. No I/O, database queries, or thread sleeps occur.
4. No event listeners or callbacks are invoked under `lock`.
Because a thread holding `lock` never waits for another lock or resource, a circular wait graph cannot form, guaranteeing that deadlocks involving the cache engine are impossible.

### 6.4 `BoundedBuffer<T>`: Inter-Thread Communication
[`BoundedBuffer<T>`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/BoundedBuffer.java) is a generic circular buffer implementing producer-consumer synchronization via Java's monitor primitives:
```java
public synchronized void put(T item) throws InterruptedException {
    while (count == capacity) {
        if (closed) throw new IllegalStateException("Buffer closed");
        wait();
    }
    items[tail] = item;
    tail = (tail + 1) % capacity;
    count++;
    notifyAll();
}
```
Waits are placed strictly inside `while` loops to protect against spurious wakeups and state theft. `notifyAll()` prevents missed signals between producer and consumer thread sets.

### 6.5 Inter-Thread Pause, Resume, and Cooperative Stop
- **`PauseGate`**: Implements an await barrier: `while (paused) wait();`. Workers check the gate only between atomic operations, never mid-transaction.
- **Cooperative Stop**: `SimulationController.stop()` closes the buffer, wakes the gate, interrupts workers, and calls `join(2000)` on each worker thread. Because cache operations execute atomically under the engine lock, cancellation leaves the cache 100% consistent.

---

## 7. Persistence Architecture: SQLite and Custom Binary Format

### 7.1 SQLite Schema & WAL Mode
SQLite database connections are initialized with Write-Ahead Logging (`PRAGMA journal_mode=WAL`), foreign key enforcement (`PRAGMA foreign_keys=ON`), and a 5000ms busy timeout.

```sql
CREATE TABLE IF NOT EXISTS snapshots (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  eviction_policy TEXT NOT NULL,
  capacity INTEGER NOT NULL,
  entry_count INTEGER NOT NULL);

CREATE TABLE IF NOT EXISTS snapshot_entries (
  snapshot_id INTEGER NOT NULL REFERENCES snapshots(id) ON DELETE CASCADE,
  cache_key TEXT NOT NULL,
  cache_value TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  last_access_at INTEGER NOT NULL,
  expires_at INTEGER,
  access_count INTEGER NOT NULL,
  insertion_seq INTEGER NOT NULL,
  PRIMARY KEY (snapshot_id, cache_key));

CREATE TABLE IF NOT EXISTS operation_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ts INTEGER NOT NULL,
  thread_name TEXT,
  operation TEXT NOT NULL,
  cache_key TEXT,
  outcome TEXT NOT NULL,
  latency_nanos INTEGER,
  details TEXT);

CREATE TABLE IF NOT EXISTS benchmark_runs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ran_at INTEGER NOT NULL,
  eviction_policy TEXT NOT NULL,
  capacity INTEGER NOT NULL,
  threads INTEGER NOT NULL,
  key_distribution TEXT NOT NULL,
  operation_mix TEXT NOT NULL,
  total_ops INTEGER NOT NULL,
  duration_nanos INTEGER NOT NULL,
  ops_per_second REAL NOT NULL,
  hit_ratio REAL NOT NULL,
  evictions INTEGER NOT NULL);
```

### 7.2 Custom Binary Snapshot Layout (`.jcs`)
Snapshots are written via `FileOutputStream`, `BufferedOutputStream`, and `DataOutputStream` wrapped in a `CheckedOutputStream` (CRC32):

| Offset / Field | Data Type | Description |
|---|---|---|
| Header Magic | `int` (4 bytes) | Fixed signature: `0x4A435331` (ASCII "JCS1") |
| Version | `short` (2 bytes) | Format version (currently `1`) |
| Capacity | `int` (4 bytes) | Cache capacity setting |
| Eviction Policy | `UTF` (variable) | Name of policy (`LRU`, `FIFO`, `LFU`) |
| Entry Count (\(N\)) | `int` (4 bytes) | Total number of serialized entries |
| **Entries Array (\(N\) records)**: | | |
| - `key` | `UTF` (variable) | Cache key |
| - `value` | `UTF` (variable) | Cache value |
| - `createdAt` | `long` (8 bytes) | Creation epoch timestamp in milliseconds |
| - `lastAccessAt` | `long` (8 bytes) | Last access epoch timestamp in milliseconds |
| - `expiresAt` | `long` (8 bytes) | Expiration epoch timestamp (-1 if no expiry) |
| - `accessCount` | `int` (4 bytes) | LFU total access count |
| - `insertionSeq` | `long` (8 bytes) | Logical insertion sequence tick |
| Trailer Checksum | `long` (8 bytes) | CRC32 checksum computed across all preceding bytes |

### 7.3 Safe Atomic Writes & Reads
- **Atomic Writes**: Data is written to a temporary file (`.tmp`), flushed, synced to disk via `getFD().sync()`, and moved over the target file using `Files.move(temp, target, ATOMIC_MOVE, REPLACE_EXISTING)`. If an error occurs, the temporary file is deleted in a `finally` block, leaving the existing file undamaged.
- **Validated Reads**: The complete file is parsed and validated into memory first (magic number check, version check, entry count validation, CRC32 checksum verification). Only after full validation does `replaceAll()` swap cache contents atomically.

---

## 8. Exception Strategy and Logging

### 8.1 Custom Checked Exception Hierarchy
```
java.lang.Exception
  └── CacheException (Root checked; getMessage() = technical, getUserMessage() = user-friendly)
        ├── InvalidCacheOperationException (Bad parameters, negative TTL, duplicate runs)
        ├── InvalidCacheCapacityException (Capacity outside 1 to 1,000,000)
        ├── CacheKeyNotFoundException (Key absent on update)
        ├── CacheConfigurationException (Aggregated list of validation errors)
        └── CachePersistenceException (Database & filesystem I/O failures)
              └── SnapshotFormatException (Corrupt, truncated, or bad magic/CRC32 file)
```

### 8.2 Exception Handling Invariants
- **No Empty Catch Blocks**: Every caught exception is either logged and handled, rethrown, or wrapped.
- **No Stack Traces in Dialogs**: Desktop error dialogs show only `getUserMessage()`. Full technical traces are routed to diagnostic logs.
- **Uncaught Exception Handlers**: Default uncaught exception handlers are installed on worker threads, the JavaFX application thread, and `Thread.setDefaultUncaughtExceptionHandler()`.

### 8.3 Logging Subsystem (`java.util.logging`)
Configured in [`AppLogging`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/logging/AppLogging.java):
- Compact single-line formatter: `[Timestamp] [Level] [Thread] Logger: Message`.
- Rotating `FileHandler`: `logs/cache-%g.log` (1 MB size, 3 files, UTF-8).
- `InMemoryLogHandler`: 500-line circular memory buffer feeding the live UI log view.
- Privacy rule: Cache keys are logged (truncated to 80 chars), but value contents are never logged (only value length).

---

## 9. JavaFX Desktop Interface & AWT Delegation Event Model

### 9.1 Desktop Layout & 9 Real Tabs
The JavaFX interface is built entirely in Java code without FXML. The root is a `BorderPane` with a top `MenuBar` and `ToolBar`, a central `TabPane` with 9 dedicated functional tabs, and a bottom `StatusBar`.

1. **Dashboard Tab**: High-level metric cards (`DropShadow`), hit-ratio `LineChart`, and eviction log `ListView`.
2. **Operations Tab**: Interactive CRUD form (Put, Get, Update, Delete, Search, Clear) with microsecond latency badge and recent operations list.
3. **Explorer Tab**: Prefix-grouped `TreeView` (splitting keys on `:`) filtering a metadata `TableView` with live Remaining TTL countdowns.
4. **Configuration Tab**: Dynamic settings, `RadioButton` eviction policy selector, and capacity reduction warning dialog.
5. **Simulator Tab**: Workload generator controls, live progress metrics, `ListView` of thread states (`Thread.getState()`), and live log.
6. **Statistics Tab**: Per-operation latency tables, `LatencyHistogram` `BarChart`, throughput charts, and integrated benchmark comparison runner.
7. **Persistence Tab**: Snapshot file save/load (`FileChooser`), SQLite snapshot manager table, and CSV exporters.
8. **History & Logs Tab**: Bounded `LinkedList` operation history audit table with type filter, and live `InMemoryLogHandler` view with log level selector.
9. **Concepts Lab Tab**: Educational runners for Race Condition demonstrations, Collections comparison, and Generic Cache proof.

### 9.2 UI Responsiveness Rules
1. **Zero I/O on UI Thread**: Database calls, snapshot operations, and benchmarks run exclusively inside JavaFX `Task<T>` background threads.
2. **Throttled Polling**: A single 250ms `Timeline` polls immutable snapshots, preventing thread starvation during 100,000+ ops/sec simulation runs.
3. **Procedural Icons**: 16x16 vector icons drawn on a `Canvas` by [`IconFactory`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/IconFactory.java) and converted to `WritableImage` for menu items and toolbar buttons.

### 9.3 Out-of-Process AWT Event Console
To avoid native UI toolkit conflicts between AWT and JavaFX within a single JVM on Linux and macOS, [`AwtEventConsole`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/awt/AwtEventConsole.java) runs as an isolated process launched via `ProcessBuilder`.
- **Delegation Event Model**: Demonstrates Event Source (Buttons, TextField), Event Object (`ActionEvent`, `KeyEvent`), and Event Listeners.
- **All 4 Listener Styles**:
  1. `ActionListener` for button clicks and Enter key triggers.
  2. `KeyListener` implementing `keyPressed`, `keyReleased`, and `keyTyped` for command history recall.
  3. `KeyAdapter` subclass overriding `keyReleased` for a live character count badge.
  4. `WindowAdapter` subclass overriding `windowClosing` for clean disposal.

---

## 10. Statistics Formulas, Rules, and Invariants

### 10.1 Mathematical Formulas
- **Hit Ratio**:
  $$\text{Hit Ratio} = \frac{\text{hits}}{\text{hits} + \text{misses}} \times 100$$
- **Miss Ratio**:
  $$\text{Miss Ratio} = \frac{\text{misses}}{\text{hits} + \text{misses}} \times 100$$
- **Cache Utilization**:
  $$\text{Utilization} = \frac{\text{currentSize}}{\text{capacity}} \times 100$$
- **Throughput**:
  $$\text{Throughput} = \frac{\text{totalOperations}}{\text{elapsedSeconds}}$$

### 10.2 Conservation Invariant
Asserted across high-concurrency stress tests:
$$\text{Inserted} - \text{Evicted} - \text{Expired} - \text{Removed} - \text{Cleared} = \text{Current Size}$$

### 10.3 Caveats
Atomic reads across multiple distinct `AtomicLong` counters are not collectively atomic while a simulation is actively running. Invariants are strictly asserted in tests after all worker threads have completed and joined.

---

## 11. Advanced Java Syllabus Mapping Contract

Every syllabus topic from the Master Specification Section 4 appears in real, reachable, production code. Each entry below has been independently verified:

| Unit & Topic | Requirement | Exact Class & File Path |
|---|---|---|
| **U1** Data types, arrays, operators, control statements | `long[]` buckets, 2-D `double[][]` matrix | [`LatencyHistogram`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/stats/LatencyHistogram.java), [`BenchmarkRunner`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/benchmark/BenchmarkRunner.java) |
| **U1** Classes, objects, constructors, methods, `this`, `static`, `final` | `this(...)` chaining, static factory, immutable snapshot | [`CacheConfiguration`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/config/CacheConfiguration.java), [`CacheFactory`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/CacheFactory.java), [`CacheEntrySnapshot`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/CacheEntrySnapshot.java) |
| **U1** Inheritance, member access, abstract class, interface | `Cache<K,V>`, `AbstractCache`, protected hooks | [`Cache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/Cache.java), [`AbstractCache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/AbstractCache.java) |
| **U1** Multilevel hierarchy, `super`, `final` with inheritance | Hierarchy, exception chaining, `super(...)` | [`AbstractCache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/AbstractCache.java) $\to$ [`ThreadSafeCache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/ThreadSafeCache.java) $\to$ [`StringCache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/StringCache.java), [`CacheException`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/exception/CacheException.java) |
| **U1** Packages, access specifiers | Dedicated packages, `public`, `protected`, package-private, `private` | [`CacheEntry`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/CacheEntry.java) (package-private), [`ThreadSafeCache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/ThreadSafeCache.java) |
| **U2** Compile-time polymorphism: overloading | Overloaded methods and constructors | [`Cache.put`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/Cache.java), [`CacheConfiguration`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/config/CacheConfiguration.java), [`OperationRecord`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/history/OperationRecord.java) |
| **U2** Runtime polymorphism: overriding, dynamic dispatch | Dynamic policy switching, method overrides | [`EvictionPolicy`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/eviction/EvictionPolicy.java), [`LeastRecentlyUsedPolicy`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/eviction/LeastRecentlyUsedPolicy.java) |
| **U2** Exception fundamentals, multiple catch, uncaught exceptions | Multiple catch, multi-catch (`A \| B e`), uncaught handlers | [`SnapshotFileManager.load`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/persistence/SnapshotFileManager.java), [`WorkerThread`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/WorkerThread.java) |
| **U2** Object class, exception hierarchy, nested try, `finally` | Hierarchy, nested try/rollback, `equals`/`hashCode`/`compareTo` | [`SnapshotRepository`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/persistence/SnapshotRepository.java), [`Student`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/lab/Student.java), [`CacheException`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/exception/CacheException.java) |
| **U3** Thread model, life cycle, `Thread`, `Runnable` | `WorkerThread extends Thread`, `implements Runnable`, state inspect | [`WorkerThread`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/WorkerThread.java), [`OperationProducerTask`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/simulation/OperationProducerTask.java), [`ThreadStateMonitor`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/ThreadStateMonitor.java) |
| **U3** Priorities, synchronization, inter-thread comm | Thread priority experiment, `synchronized`, `wait()`/`notifyAll()` | [`WorkerPool`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/WorkerPool.java), [`BoundedBuffer`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/BoundedBuffer.java), [`PauseGate`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/threading/PauseGate.java) |
| **U3** I/O, byte streams, `InputStream`, `OutputStream` | Binary `.jcs` format, `FileInputStream`, `FileOutputStream` | [`SnapshotFileManager`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/persistence/SnapshotFileManager.java), [`ConfigurationStore`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/config/ConfigurationStore.java) |
| **U3** Delegation event model, event classes, `KeyEvent` | AWT console events, custom cache events | [`AwtEventConsole`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/awt/AwtEventConsole.java), [`CacheEvent`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/event/CacheEvent.java) |
| **U3** Listener interfaces, adapter classes | `ActionListener`, `KeyListener`, `KeyAdapter`, `WindowAdapter` | [`AwtEventConsole`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/awt/AwtEventConsole.java), [`CacheEventAdapter`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/event/CacheEventAdapter.java) |
| **U4** JDBC, SQLite, tables, CRUD | `DriverManager`, schema DDL, `PreparedStatement`, `ResultSet` | [`DatabaseManager`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/persistence/DatabaseManager.java), [`SnapshotRepository`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/persistence/SnapshotRepository.java) |
| **U4** Collections overview, `List`, `Set`, `Map`, `ArrayList`, `LinkedList` | `ArrayList` rows, `LinkedList` history, `HashMap` storage | [`OperationHistory`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/history/OperationHistory.java), [`ThreadSafeCache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/ThreadSafeCache.java) |
| **U4** `Vector`, `HashSet`, `TreeSet`, `Hashtable`, `HashMap`, `Iterator` | `TreeSet` LFU/expiry, `HashSet`, explicit `Iterator.remove()` | [`ExpiryIndex`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/expiry/ExpiryIndex.java), [`LeastFrequentlyUsedPolicy`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/eviction/LeastFrequentlyUsedPolicy.java), [`CollectionsLab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/lab/CollectionsLab.java) |
| **U5** Generics: 2 type params, wildcards, bounded methods | `Cache<K,V>`, `Repository<T,ID>`, `putAll(? extends)`, `<T extends Comparable>` | [`Cache`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/Cache.java), [`CollectionTools`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/core/CollectionTools.java), [`GenericCacheLab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/lab/GenericCacheLab.java) |
| **U5** JavaFX skeleton, `RadioButton` | `CacheApplication extends Application`, policy `RadioButton` group | [`CacheApplication`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/CacheApplication.java), [`ConfigurationTab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/ConfigurationTab.java) |
| **U5** `ListView`, `ComboBox`, `TextField`, `TreeView` | History list, mix combos, prefix key tree explorer | [`ExplorerTab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/ExplorerTab.java), [`OperationsTab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/OperationsTab.java), [`SimulatorTab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/SimulatorTab.java) |
| **U5** Effects and transforms | `DropShadow`, `Glow` flash, `Scale` pulse, `Rotate` spinner | [`DashboardTab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/DashboardTab.java), [`SimulatorTab`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/SimulatorTab.java) |
| **U5** `MenuBar`, `Menu`, `MenuItem`, `ImageView`, `ToolBar` | Menu bar, 16x16 canvas icons, toolbar buttons with tooltips | [`CacheApplication`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/CacheApplication.java), [`IconFactory`](file:///home/shivangi/projects/java-project/src/main/java/com/cse309/cache/ui/IconFactory.java) |

---

## 12. Build, Run, and Test Commands

### 12.1 Prerequisites
- JDK 17 or higher (tested on OpenJDK 27 with `--release 17`).
- Apache Maven 3.9+ installed and available on `PATH`.

### 12.2 Clean Build & Run All Tests
To compile the entire codebase with `-Xlint:all` and run all 91 automated unit tests:
```bash
mvn clean test
```
*Expected Result*: Build Success, 91 tests run, 0 failures, 0 errors, 0 skipped.

### 12.3 Launch JavaFX Desktop Application
To start the main desktop caching application:
```bash
mvn javafx:run
```
*Or via direct Java execution after packaging*:
```bash
mvn package -DskipTests
java -cp "target/multithreaded-in-memory-cache-1.0.0.jar:target/dependency/*" com.cse309.cache.ui.Launcher
```

### 12.4 Launch AWT Event Console (Standalone Process)
To run the isolated AWT Delegation Event Model console:
```bash
java -cp target/classes com.cse309.cache.awt.AwtEventConsole
```

---

## 13. Verification Status & Honesty Disclosure

In accordance with Master Specification Section 0, 17, and 18:

### 13.1 What Was Automatically Tested
- **Core Cache Semantics**: All table rows from Section 6 (`ThreadSafeCacheTest`).
- **Worked Examples**: Exact LRU, FIFO, and LFU worked examples from Section 7 (`EvictionPoliciesWorkedExamplesTest`).
- **Deterministic TTL Expiration**: Injected `MutableClock` verifying lazy expiry and active daemon sweep without `Thread.sleep` (`ExpirationTest`, `CacheExpirationServiceTest`).
- **Concurrency & Stress**: 16 concurrent threads x 20,000 operations (320,000 ops) and 500,000-operation simulation runs verifying conservation invariant, zero escaped exceptions, and capacity bounding (`ConcurrencyStressTest`).
- **Producer-Consumer & Pause Gate**: `BoundedBufferTest` and `SimulationControllerTest` (verified 20 consecutive runs with 100% pass rate).
- **Persistence & Integrity**: Round-trip binary `.jcs` file serialization, CRC32 tampering detection, atomic replacement, and SQLite rollback on error (`SnapshotFileManagerTest`, `RepositoryTest`).
- **Shutdown & Thread Hygiene**: `ShutdownTest` verifying that after `CacheManager.shutdown()`, zero non-daemon application threads (`cache-*`) remain alive.
- **Code Cleanliness Audit**: Verified zero `TODO` comments, zero `printStackTrace()` calls, zero empty `catch` blocks, and all 42 required syllabus keywords present in reachable code.

### 13.2 What Was Verified by Process Launch
- Successful initialization and execution of `mvn javafx:run` connecting to SQLite and creating all UI tabs.
- Successful launch and event capture of standalone `AwtEventConsole` on X11/Wayland display.

### 13.3 What Was Not Automatically Run (Requires Human Verification)
- Aesthetic visual validation of JavaFX render transforms (`DropShadow`, `Glow`, `Scale`, `Rotate`) on physical displays.
- Interactive manual mouse clicks across each tab (documented in `docs/MANUAL_TEST_CHECKLIST.md`).
- Screen capture of GUI panels for final report submission (listed in `docs/screenshots/README.md`).

---

## 14. Known Limitations

1. **Single JVM Deployment**: The cache is purely local to a single Java process; it does not provide distributed clustering or network RPC protocols (by prompt design non-goals).
2. **Text-Persisted Values**: Persistence through `ValueCodec<V>` currently supports text-representable forms (`StringValueCodec`). Arbitrary non-serializable binary graphs are not supported by the SQLite text codec.
3. **Entry-Count Capacity**: Capacity is enforced by total number of keys (1 to 1,000,000 entries) rather than dynamic byte-level heap memory accounting.
4. **Wall-Clock Skew Sensitivity**: TTL expiration relies on wall-clock epoch milliseconds; abrupt system clock adjustments (e.g. NTP backwards steps) can temporarily affect remaining TTL calculations.
5. **O(n) Snapshot Memory Copy**: Creating a snapshot copies entries into a list inside the lock. While fast (\(<2\,\text{ms}\) for 10,000 entries), taking a snapshot of 1,000,000 entries will hold the lock during the list allocation.
6. **No GUI Automation Tests**: In strict compliance with Section 3 ("JUnit 5 only; no TestFX"), GUI controls are validated via plain-Java model tests and the manual verification checklist.

---

## 15. Future Enhancements

1. **Lock Striping & Segmented Caching**: Partition the cache into 16 or 32 independent striped segments (similar to Java 7 `ConcurrentHashMap`), reducing lock contention on high-core servers.
2. **`ReentrantReadWriteLock` for Read-Heavy Read-Only Policies**: Provide an alternative eviction policy mode (such as Random Eviction or Clock algorithm) where `get()` does not mutate metadata, allowing multi-reader shared locks.
3. **Java Concurrency Utilities**: Transition worker management to `ExecutorService` and periodic expiration to `ScheduledExecutorService` as enterprise production alternatives to hand-written `Thread` loops.
4. **Byte-Sized Dynamic Capacity**: Implement an instrumentation agent (`java.lang.instrument.Instrumentation`) to measure object heap footprints and bound cache size by megabytes.
5. **Append-Only Write-Ahead Log (WAL)**: Introduce an asynchronous append-only disk log for zero-data-loss microsecond crash recovery.