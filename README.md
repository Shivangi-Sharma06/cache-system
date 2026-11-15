# Multithreaded In-Memory Caching System

A high-performance, thread-safe in-memory key-value caching system developed in Java 17+ with a JavaFX desktop interface and dual persistence (SQLite WAL and binary `.jcs`).

Built as a capstone project for **CSE 309 (Advanced Java Programming)**, demonstrating core Java concurrency primitives, object-oriented design patterns, collections, I/O streams, JDBC, and desktop GUI architecture.

---

## Key Features

- **Eviction Policies**:
  - **LRU** ($O(1)$): Least Recently Used tracking via `LinkedHashSet`.
  - **FIFO** ($O(1)$): First-In-First-Out chronological sequencing via `LinkedHashSet`.
  - **LFU** ($O(\log n)$): Least Frequently Used via frequency-ordered `TreeSet` with `HashMap` node indexing and deterministic tie-breaking.
- **Wall-Clock TTL & Expiry Daemon**: Timed background purge daemon thread (`CacheExpirationService`) operating on an ordered `TreeSet` index, plus lazy expiration on access.
- **Thread-Safe Engine**: Single private monitor lock (`synchronized`) enforcing leaf-lock discipline (mathematically deadlock-free).
- **Concurrency & Simulation**: Multi-worker producer-consumer workload simulator (1–64 threads) using custom `BoundedBuffer<T>`, `PauseGate`, and customizable key distributions.
- **Dual Persistence Layer**:
  - **SQLite Database**: Out-of-band durable storage (WAL mode, transactions, rollback) for point-in-time snapshots and operation history. Zero credentials needed.
  - **Binary Snapshot (`.jcs`)**: Fast, portable serialization via `DataOutputStream` with magic header (`0x4A435331`) and CRC32 integrity checksum.
- **Streamlined JavaFX Desktop GUI (4 Core Tabs)**:
  - **Dashboard**: Real-time metrics cards, hit-ratio trend line chart, and live eviction log with visual effects (`DropShadow`, `Glow`, `Scale`, `Rotate`).
  - **Operations**: Interactive CRUD form (Put, Get, Update, Delete, Search, Clear) with live latency measurement and integrated Eviction Policy & Capacity tuning.
  - **Simulator**: Multi-threaded concurrency workload generator (1–64 worker threads) with live throughput and worker thread states.
  - **Persistence**: Point-in-time SQLite database snapshots (warm-start & recovery) and portable binary `.jcs` file exports.

---

## Tech Stack

| Component | Technology | Details |
|---|---|---|
| **Language** | Java 17+ (LTS) | Compiled with `--release 17` and `-Xlint:all` |
| **Desktop UI** | OpenJFX 21.0.5 | Scene graph, charts, controls, and visual transforms |
| **Persistence** | SQLite JDBC 3.47.1.0 | Serverless embedded database with WAL mode |
| **Build Tool** | Apache Maven 3.9+ | Dependency management and build lifecycle |
| **Testing** | JUnit 5 | Unit, policy, and high-concurrency stress verification |

---

## Coursework & Syllabus Coverage (CSE 309)

| Unit | Focus Topics | Implementation in Project |
|---|---|---|
| **Unit 1: Core OOP & Java Fundamentals** | Classes, Encapsulation, Abstract classes, Interfaces, Inheritance | `Cache<K,V>` interface, `AbstractCache<K,V>` skeletal base, `ThreadSafeCache<K,V>`, `StringCache`. Constructor chaining via `this(...)` in `CacheConfiguration`. Static factories in `CacheFactory`. Immutability with `final` fields in `CacheEntrySnapshot`. |
| **Unit 2: Polymorphism, Exceptions & Generics** | Overloading, Overriding, Dynamic dispatch, Checked exceptions, Generics | Overloaded `put()` and `update()`. Dynamic eviction policy dispatch via `EvictionPolicy<K>`. Custom exception hierarchy rooted at `CacheException` with user-friendly messages. Multi-catch (`A \| B e`), nested `try`/`finally` rollback. Bounded generics `<T extends Comparable<? super T>>` and wildcards `Map<? extends K, ? extends V>`. |
| **Unit 3: Multithreading & Byte Streams I/O** | Thread states, Monitors, Inter-thread comms, Byte streams | `WorkerThread extends Thread`, `OperationProducerTask implements Runnable`. Live thread state inspection (`Thread.getState()`). Monitor synchronization, `wait()` inside `while` loops, and `notifyAll()` in `BoundedBuffer` and `PauseGate`. Binary `.jcs` file I/O via `DataOutputStream` and CRC32. |
| **Unit 4: JDBC & Collections Framework** | SQLite, PreparedStatements, Transactions, Java Collections | `DatabaseManager` with SQLite WAL mode. Parameterized SQL queries, batch updates, and transaction rollbacks (`setAutoCommit(false)`). Java Collections: `LinkedHashSet` (LRU/FIFO), `TreeSet` (LFU/TTL), `LinkedList` (bounded history), `HashMap` (in-memory store), thread safety considerations of legacy vs modern collections. |
| **Unit 5: JavaFX Desktop Architecture** | Application lifecycle, UI controls, Layouts, Effects, Transforms | JavaFX desktop GUI with 4 core tabs (Dashboard, Operations, Simulator, Persistence). Responsive design: background tasks via `Task<T>` and throttled 250ms polling `Timeline`. Controls: `TableView`, `ListView`, `ComboBox`, `LineChart`, `ProgressBar`. Effects: `DropShadow`, `Glow`, `Scale`, and `Rotate`. Procedural 16x16 canvas icons via `IconFactory`. |

---

## Architecture & Design Principles

1. **RAM-First In-Memory Storage**: RAM is the exclusive primary cache. Lookups, inserts, updates, and deletes operate strictly in memory ($< 1\,\mu\text{s}$ latency).
2. **Out-of-Band Persistence**: Disk and database operations never run on the hot path or under the cache lock. Snapshots are copied in memory, released immediately, and written asynchronously.
3. **Deadlock Freedom (Leaf-Lock Discipline)**: The cache engine synchronizes on a single private monitor lock (`lock`). It never invokes callbacks, secondary locks, or I/O while holding the lock, mathematically eliminating circular wait conditions.
4. **Zero-Configuration SQLite**: SQLite is embedded and file-based (`data/cache.db`). No servers, ports, usernames, passwords, or `.env` files are required.

---

## How to Build and Run

### Prerequisites
- JDK 17 or higher
- Apache Maven 3.9+

### 1. Compile
```bash
mvn clean compile
```

### 2. Launch Desktop JavaFX Application
```bash
mvn javafx:run
```

### 3. Package Standalone JAR (Optional)
```bash
mvn package -DskipTests
java -cp "target/multithreaded-in-memory-cache-1.0.0.jar:target/dependency/*" com.cse309.cache.ui.Launcher
```

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| **Ctrl + 1** to **Ctrl + 4** | Switch directly to Tabs 1 through 4 |
| **Ctrl + S** | Save binary snapshot file (`.jcs`) |
| **Ctrl + O** | Load binary snapshot file (`.jcs`) |
| **Ctrl + E** | Export live cache entries to CSV |
| **F5** | Refresh snapshots list |
| **Ctrl + Q** | Gracefully shut down and exit |