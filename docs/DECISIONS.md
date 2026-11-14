# Architectural and Design Decisions: CSE 309 Multithreaded In-Memory Caching System

This document logs all architectural and design decisions, rationales, and any minor clarifications made during implementation, conforming to the master specification.

---

## 1. Primary Decisions (from Master Specification Section 2)

### 1.1 In-Memory RAM vs. Persistence Boundary
- **Decision**: RAM is the only cache. `get`, `put`, `update`, and `remove` touch strictly in-memory data structures.
- **Rationale**: In-memory caching requires sub-microsecond latency. SQLite is used purely as an out-of-band durable store for snapshots, operation audit trails, and benchmark run persistence. SQLite operations are never on the hot path and never executed while holding the cache lock.

### 1.2 Separation of Persistence Mechanisms
- **Decision**: SQLite handles queryable historical records and structured snapshots; Java I/O streams handle standalone portability via custom binary `.jcs` files, CSV exports, and `config.properties`.
- **Rationale**: Keeps responsibilities segregated and single-purpose without redundant database complexity for basic configuration.

### 1.3 Concurrency Control: Single Private Lock
- **Decision**: A single private `final Object lock = new Object();` guards all internal engine collections, indices, and tick counters using Java's built-in `synchronized (lock)`.
- **Rationale**: Cache operations (including `get`) modify eviction metadata (LRU recency, LFU access frequency). `ReentrantReadWriteLock` would require an exclusive write lock on every `get`, giving no throughput advantage over `synchronized` while adding overhead and complexity. `ConcurrentHashMap` cannot atomically coordinate map modifications with eviction policy data structures without external locking. Leaf-lock discipline prevents deadlocks.

### 1.4 Syllabus Concurrency Primitives Only
- **Decision**: We hand-code `Thread` subclasses, `Runnable` implementations, `wait()`/`notifyAll()`, `join()`, `volatile`, and custom worker thread pools. `AtomicLong` and `AtomicInteger` are permitted for performance counters. `ExecutorService`, `BlockingQueue`, and `ReentrantReadWriteLock` are not used in production cache engine code.
- **Rationale**: The project is designed and evaluated on core syllabus concurrency mastery.

### 1.5 Time and Clock Abstraction
- **Decision**: TTL and expiration timestamps use wall-clock epoch milliseconds obtained from an injected `java.time.Clock`. Latency calculations use `System.nanoTime()`.
- **Rationale**: Epoch milliseconds are durable and persistable across process restarts. Clock injection enables completely deterministic, instant unit testing using a `MutableClock` without requiring `Thread.sleep`.

### 1.6 Custom Binary Snapshot Format (`.jcs`)
- **Decision**: Snapshots are serialized using `DataOutputStream` / `DataInputStream` with a fixed magic number `0x4A435331`, version, capacity, policy, entry count, structured field writes, and an end-of-file CRC32 checksum.
- **Rationale**: Avoids the severe security and compatibility pitfalls of standard Java Object Serialization (`ObjectInputStream` / `ObjectOutputStream`).

### 1.7 Out-of-Process AWT Event Console
- **Decision**: The AWT Event Console runs in an isolated JVM process invoked via `ProcessBuilder`.
- **Rationale**: Running AWT and JavaFX event dispatch threads concurrently inside the same JVM instance causes native UI toolkit conflicts on multiple operating systems (specifically macOS and specific Wayland/X11 Linux configurations).

### 1.8 Asynchronous Event Dispatch (No Callbacks Under Lock)
- **Decision**: Events (evictions, expirations, puts, updates, removes) and `EvictionRecord` objects generated during a mutation are queued locally inside the critical section and delivered to registered listeners only after the engine lock is completely released.
- **Rationale**: Guarantees that slow or misbehaved listeners cannot stall the cache or introduce lock inversions/deadlocks.

### 1.9 Logical Ticks for Ordering
- **Decision**: LRU and FIFO recency and tie-breaking use monotonically increasing `long` sequence counters (`lastAccessTick`, `insertionSequence`).
- **Rationale**: Multiple operations within the same millisecond have identical wall-clock timestamps; logical ticks guarantee absolute, deterministic ordering.

---

## 2. Additional Design Clarifications

### 2.1 Default Uncaught Exception Handlers
- **Decision**: Dedicated `UncaughtExceptionHandler` installed on worker threads, the JavaFX thread, and `Thread.setDefaultUncaughtExceptionHandler`.
- **Rationale**: Ensures unexpected runtime exceptions in background tasks do not crash the application silently or terminate the UI process.

### 2.2 UI Refresh Throttling
- **Decision**: JavaFX UI utilizes a central 250ms `Timeline` to poll immutable metric snapshots and drain a bounded `UiEventBuffer`.
- **Rationale**: Avoids saturating the JavaFX Application Thread with thousands of individual `Platform.runLater` invocations during high-throughput simulation runs.
