# Multithreaded Java-Based In-Memory Caching System with Concurrent Access Management

**Coursework**: CSE 309 Advanced Java Programming  
**Author**: Shivangi Sharma  
**Target Environment**: Java 17+ LTS (`--release 17`), OpenJFX 21, SQLite JDBC, Apache Maven  

---

## 1. Project Overview & Significance

### 1.1 What We Have Made
We have engineered **Multithreaded Java-Based In-Memory Caching System with Concurrent Access Management**, a high-performance, enterprise-grade local key-value caching system featuring a rich JavaFX desktop management console and an isolated AWT Delegation Event Model inspection tool. 

The application implements a production-grade cache engine entirely in RAM, featuring:
- **Three Independent Eviction Policies**: $O(1)$ Least Recently Used (LRU), $O(1)$ First-In-First-Out (FIFO), and $O(\log n)$ Least Frequently Used (LFU) with deterministic tie-breaking.
- **Wall-Clock TTL & Background Expiration Daemon**: Active sweeping via a dedicated daemon thread operating on an ordered `TreeSet` index, paired with lazy expiration upon access.
- **Concurrent Workload Simulation Pipeline**: A multi-worker producer-consumer subsystem utilizing custom monitor-synchronized bounded buffers, pause gates, and key-space distribution generators.
- **Dual Out-of-Band Persistence**: Point-in-time ACID transactions using SQLite (WAL mode) and portable custom binary `.jcs` file serialization protected by CRC32 checksum verification.
- **Modern Desktop JavaFX User Interface**: 9 interactive, fully-functional management tabs featuring real-time charts, procedural vector canvas icons, visual transforms (`DropShadow`, `Glow`, `Scale`, `Rotate`), and non-blocking 250ms timeline metric polling.
- **Educational Concurrency & Collections Lab**: Live interactive demonstrations of race conditions, thread synchronization benefits, collection benchmarking (`Vector` vs `ArrayList`, `Hashtable` vs `HashMap`), and generic multi-type cache operations.

### 1.2 Significance of the Project
In distributed systems, web backends, and financial transaction engines, the **latency disparity** between processor execution and secondary storage is the primary performance bottleneck:
- **CPU Registers & Dynamic RAM**: Read/write latencies are sub-microsecond ($< 1\,\mu\text{s}$).
- **Solid State Disks & Relational Databases**: Read/write latencies range from hundreds of microseconds to tens of milliseconds ($> 1,000\,\mu\text{s}$ to $50\,\text{ms}$) due to kernel context switches, disk I/O, SQL parsing, and transaction locking.

An in-memory cache resolves this bottleneck by serving hot data directly from memory. However, designing a multithreaded caching system introduces fundamental concurrency challenges:
1. **Thread Safety & Race Conditions**: Uncontrolled concurrent access causes corrupted hash tables, lost updates, and memory leaks.
2. **Deadlock Hazards**: Mismanaged lock ordering or invoking external callbacks inside critical sections causes permanent thread stalls.
3. **Eviction Complexity**: Under finite memory constraints, eviction algorithms must select victims deterministically in $O(1)$ or $O(\log n)$ time without scanning the entire cache.
4. **UI Thread Starvation**: High-throughput background operations (processing 100,000+ operations/second) must never block the single-threaded GUI rendering loop.

This project addresses each of these challenges from first principles, demonstrating how production caching architectures (such as those in Redis, Memcached, and Caffeine) are designed and implemented using foundational Java concurrency, object-oriented design patterns, and database primitives.

---

## 2. Coursework & Syllabus Integration (CSE 309)

The project serves as a comprehensive capstone demonstrating the **CSE 309 Advanced Java Programming** university curriculum across all five syllabus units:

### Unit 1: Object-Oriented Design, Classes, Encapsulation & Inheritance
- **Abstract Classes vs. Interfaces**: The public API contract is defined by the pure generic interface `Cache<K,V>`, while the skeletal base class `AbstractCache<K,V>` provides shared state (capacity, statistics, listener registries) and template methods.
- **Constructor Chaining**: `CacheConfiguration` implements constructor chaining via `this(...)`, routing overloaded convenience constructors to a single canonical constructor that enforces validation invariants.
- **Static Factory Methods & Constants**: `CacheFactory` provides centralized creation points for generic and string-specialized caches; `CacheDefaults` encapsulates system constants.
- **Immutability & Access Control**: `CacheEntrySnapshot` uses `final` fields and defensive copying to guarantee complete immutability. Encapsulation is enforced using all four access levels (`public` APIs, `protected` extension hooks, package-private mutable entry structures like `CacheEntry<V>`, and `private` monitor locks).
- **Multilevel Inheritance Hierarchy**: Demonstrates deep polymorphic hierarchies (`AbstractCache` $\to$ `ThreadSafeCache` $\to$ `StringCache`) with proper `super(...)` constructor calls and method overrides.

### Unit 2: Polymorphism, Custom Exceptions & Generics
- **Compile-Time Polymorphism (Overloading)**: Overloaded methods are implemented throughout the engine (e.g., `put(K, V)` and `put(K, V, Duration)`; `update(K, V)` and `update(K, V, Duration)`).
- **Runtime Polymorphism (Dynamic Method Dispatch)**: The cache engine interacts with an abstract `EvictionPolicy<K>` interface reference. At runtime, when the user switches policies, the engine dynamically dispatches calls to concrete policy implementations (`LeastRecentlyUsedPolicy`, `FirstInFirstOutPolicy`, `LeastFrequentlyUsedPolicy`) without modifying engine internals.
- **Checked Exception Hierarchy**: A structured checked exception hierarchy rooted at `CacheException` separates internal technical diagnostic messages (`getMessage()`) from user-friendly UI explanations (`getUserMessage()`). Specialized exceptions include `InvalidCacheOperationException`, `InvalidCacheCapacityException`, `CacheKeyNotFoundException`, `CacheConfigurationException`, and `CachePersistenceException` (with `SnapshotFormatException`).
- **Robust Exception Handling**: Demonstrates nested `try` blocks (for JDBC transaction rollbacks), multiple `catch` blocks, multi-catch (`catch (IOException | SQLException e)`), and mandatory `finally` blocks for resource cleanup.
- **Uncaught Exception Handlers**: Per-worker `UncaughtExceptionHandler` instances ensure that worker thread errors are logged at `SEVERE` and displayed in the UI without crashing the application.
- **Object Class Methods**: Overrides `equals()`, `hashCode()`, and `toString()` across immutable models (`CacheEntrySnapshot`, `OperationRecord`, and the `Student` domain model).
- **Generics & Bounded Type Parameters**: The engine is fully generic (`Cache<K,V>`). Method signatures use wildcards (`putAll(Map<? extends K, ? extends V>)`, `search(Predicate<? super K>)`) and bounded generic methods (`<T extends Comparable<? super T>>`) to ensure complete type safety.

### Unit 3: Multithreading, Concurrency Primitives, I/O & Event Handling
- **Thread Lifecycle & State Inspection**: Implements custom `WorkerThread extends Thread` and `OperationProducerTask implements Runnable`. The UI inspects and displays live thread states (`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`) using `Thread.getState()`.
- **Monitor Synchronization & Inter-Thread Communication**: Core data structures are synchronized using Java's built-in `synchronized` monitors. Producer-consumer coordination in `BoundedBuffer<T>` and the `PauseGate` barrier utilize `wait()` inside `while` loops (to prevent spurious wakeups) and `notifyAll()` to coordinate threads safely.
- **Thread Priorities**: An optional priority experiment dynamically assigns `Thread.MIN_PRIORITY` and `Thread.MAX_PRIORITY` to demonstrate thread scheduling hints.
- **Daemon Threads**: `CacheExpirationService` and the persistence write-behind queue execute as daemon threads, ensuring clean background execution without preventing JVM shutdown.
- **Byte Stream I/O**: Portable binary `.jcs` snapshots are written and read using `FileOutputStream`, `FileInputStream`, `BufferedOutputStream`, and `DataOutputStream` wrapped in `CheckedOutputStream` (CRC32).
- **Delegation Event Model**: The standalone AWT Event Console implements the Delegation Event Model, demonstrating Event Sources (buttons, text fields), Event Objects (`ActionEvent`, `KeyEvent`), and Event Listeners (`ActionListener`, `KeyListener`, `KeyAdapter`, `WindowAdapter`).

### Unit 4: JDBC, SQLite Database & Java Collections Framework
- **JDBC Database Management**: `DatabaseManager` connects to local SQLite via `DriverManager`, enabling Write-Ahead Logging (`PRAGMA journal_mode=WAL`), foreign key enforcement, and connection-level busy timeouts.
- **Parameterized Queries & SQL Injection Immunity**: All database interactions in `SnapshotRepository`, `OperationHistoryRepository`, and `BenchmarkRepository` use parameterized `PreparedStatement`s and process `ResultSet`s with deterministic column mapping.
- **ACID Transaction Management**: Multi-row snapshot writes execute inside atomic transactions (`setAutoCommit(false)`, `executeBatch()`, `commit()`, with nested `rollback()` on exceptions).
- **Java Collections Framework**:
  - `LinkedHashSet<K>`: Powers $O(1)$ LRU recency tracking and $O(1)$ FIFO chronological sequencing.
  - `TreeSet<E>`: Backs LFU frequency ordering and the TTL `ExpiryIndex`, guaranteeing $O(\log n)$ operations.
  - `LinkedList<E>`: Implements the bounded operation history audit log with $O(1)$ head/tail operations.
  - `HashMap<K,V>`: Serves as the primary in-memory entry store and LFU node index.
  - Legacy Collections & CME: The Concepts Lab benchmarks legacy collections (`Vector`, `Hashtable`) against modern alternatives and demonstrates fail-fast `ConcurrentModificationException` vs. safe `Iterator.remove()`.

### Unit 5: JavaFX Desktop Architecture, Visual Transforms & Controls
- **Desktop Application Lifecycle**: Main GUI extends `javafx.application.Application` initialized via a standalone `Launcher` class to avoid classpath/module issues.
- **Responsive GUI Threading**: The JavaFX Application Thread is strictly reserved for scene graph rendering. Database I/O, file serialization, and benchmark tasks run on background threads using JavaFX `Task<T>`.
- **UI Refresh Throttling**: A central 250ms `Timeline` polls immutable metric snapshots and drains a thread-safe `UiEventBuffer`, maintaining smooth 60 FPS performance even when the engine executes over 100,000 operations/second.
- **Comprehensive UI Controls**: Utilizes `TabPane` (9 dedicated tabs), `TableView`, `TreeView` (namespace key explorer), `ListView`, `RadioButton` (toggle groups), `ComboBox`, `TextField`, `ProgressBar`, `LineChart`, and `BarChart`.
- **Visual Effects & Transforms**: Applies `DropShadow` to dashboard metric cards, `Glow` flashes on counter updates, `Scale` pulse transitions on cache evictions, and `Rotate` spinner animations during simulation runs.
- **Procedural Canvas Icons**: `IconFactory` dynamically draws 16x16 vector icons onto a `Canvas` and snapshots them into `WritableImage`s for menu items and toolbars without requiring external image files.

---

## 3. Engineering Approach & Architectural Decisions

### 3.1 RAM is the Cache; SQLite is Out-of-Band
A core design principle is that **RAM is the exclusive cache**. Primary operations (`get`, `put`, `update`, `remove`, `search`) touch memory only.
- SQLite is strictly an out-of-band durable store used for warm-start snapshots, audit logs, and benchmark history.
- Database access is never on the critical hot path and never occurs while holding the cache lock.
- Taking a database snapshot copies entries inside the engine lock (a microsecond in-memory copy), releases the lock immediately, and writes the batch to SQLite in a single transaction on a background thread.

### 3.2 Single Private Monitor Lock Discipline
All internal engine collections, indices, and tick counters are guarded by a single private monitor:
```java
private final Object lock = new Object();
```
- **Why not `ReentrantReadWriteLock`?** In caching systems implementing LRU or LFU, a read (`get()`) modifies eviction metadata (updating access ticks in LRU and incrementing frequency in LFU). Because `get()` is fundamentally a mutating write to metadata, `readLock()` cannot be used. Every lookup would require `writeLock()`, eliminating multi-reader concurrency while adding significant lock overhead. A single private `synchronized` lock provides higher throughput, lower overhead, and clean semantics.
- **Deadlock-Freedom (Leaf-Lock Discipline)**: A thread holding `lock` performs only memory operations. It never invokes external callbacks, never executes blocking I/O or database queries, and never acquires secondary locks. Because circular wait conditions cannot form, deadlocks involving the cache engine are mathematically impossible.

### 3.3 Zero-Credential Embedded SQLite Architecture
SQLite is an **in-process, serverless embedded database engine**.
- The database is stored directly as a local file (`data/cache.db`).
- There is **no daemon process, no network port, no username, and no password**.
- Connections are established via `DriverManager.getConnection("jdbc:sqlite:" + path)`.
- Consequently, **no credentials, environment variables, or secrets files are needed**. The application runs immediately out of the box on any system with zero configuration.

### 3.4 Logical Monotonic Ticks for Ordering
Rather than relying on wall-clock milliseconds (where multiple operations in the same millisecond have identical timestamps), recency and insertion sequences are tracked using monotonic `long` sequence counters. This guarantees deterministic tie-breaking under high concurrent throughput.

---

## 4. Key Features & Tab Capabilities

### Cache Engine & Eviction Algorithms
- **Least Recently Used (LRU)**: Maintains an access-ordered `LinkedHashSet`. Reading or writing an entry moves it to the most recent position. When capacity is exceeded, the oldest entry at the head is evicted ($O(1)$).
- **First-In-First-Out (FIFO)**: Maintains an insertion-ordered `LinkedHashSet`. Reads do not modify order; updating an existing key preserves its original position. The earliest inserted entry is evicted first ($O(1)$).
- **Least Frequently Used (LFU)**: Tracks frequency using a sorted `TreeSet` of `(frequency, sequence)` pairs with a `HashMap` node index. Access increments frequency and re-indexes the node in $O(\log n)$ time. Ties are deterministically resolved by evicting the earliest inserted entry among those with equal frequency.
- **Atomic Dynamic Reconfiguration**: Changing eviction policies rebuilds the policy index atomically without lost or duplicate entries. Reducing capacity previews the number of victims and evicts excess entries atomically with reason `CAPACITY_REDUCED`.

### Desktop Interface (9 Tabs)
1. **Dashboard Tab**: High-level system cards (Capacity, Size, Hit Ratio, Miss Ratio, Total Ops, Evictions, Expirations, Active Policy), real-time hit ratio `LineChart`, and a live eviction log `ListView`.
2. **Operations Tab**: Interactive CRUD interface for manual cache manipulation (Put, Get, Update, Delete, Search, Clear) with microsecond latency badges and a 50-entry outcome history.
3. **Explorer Tab**: Namespace explorer grouping keys by prefix before `:` into a `TreeView`, filtering an interactive `TableView` displaying key, value, created time, last access, expiration, remaining live TTL countdown, and access count.
4. **Configuration Tab**: Dynamic settings management, policy switching via `RadioButton` toggle groups, TTL unit selectors, cleanup interval controls, and persistence mode toggles.
5. **Simulator Tab**: Concurrency testing laboratory supporting 1 to 64 threads, operation counts up to 500,000+, preset and custom operation mixes (read-heavy, mixed, write-heavy), key distributions (Uniform, Hotspot 80/20, Sequential Scan), random seed reproducibility, and live thread state monitoring.
6. **Statistics Tab**: Per-operation latency breakdown table, a 6-bucket `LatencyHistogram` `BarChart` ($<1\mu\text{s}$ to $>10\text{ms}$), throughput monitors, CSV export, and an integrated benchmark comparison runner.
7. **Persistence Tab**: Snapshot file save/load dialogs (`.jcs`), SQLite snapshot management table (save, restore, delete), and CSV exporters for cache entries, history, and benchmarks.
8. **History & Logs Tab**: Bounded `LinkedList` operation history audit log with operation-type filtering, live `InMemoryLogHandler` log console, and dynamic `java.util.logging` level selector.
9. **Concepts Lab Tab**: Educational concurrency testbeds demonstrating race conditions in `UnsafeCounterBox` vs. `SafeCounterBox`, concurrent `HashMap` corruption, collection benchmarks (`ArrayList`, `LinkedList`, `Vector`, `HashMap`, `Hashtable`, `TreeMap`, `TreeSet`), fail-fast iterators, and generic multi-type caches (`Integer/String`, `String/Student`, `Long/Double`).

### AWT Event Console (Standalone Process)
An isolated AWT `Frame` running in a separate JVM process via `ProcessBuilder`, demonstrating the pure Delegation Event Model. It features an interactive command interpreter (`put`, `get`, `del`, `stats`, `clear`, `help`) operating against its own private cache, command history recall via arrow keys, live character counter, and an **Event Inspector** displaying event class names, sources, and action parameters.

---

## 5. Technology Stack Summary

| Layer / Subsystem | Technology / Library | Purpose |
|---|---|---|
| **Programming Language** | Java 17+ LTS | Modern Java features (records, switch expressions, sealed hierarchies, pattern matching) compiled with `--release 17`. |
| **Compiler Options** | `-Xlint:all -Xlint:-processing` | Strict linting ensuring zero compiler warnings, zero deprecations, and robust type safety. |
| **Build Management** | Apache Maven 3.9+ | Dependency management, compilation lifecycle, and execution plugins. |
| **Desktop UI Framework** | OpenJFX (`javafx-controls` 21.0.5) | Desktop GUI, scene graph, layout panes, visual effects, and charting components. |
| **Embedded Database** | SQLite JDBC (`sqlite-jdbc` 3.47.1.0) | Zero-configuration serverless persistence with Write-Ahead Logging (WAL). |
| **Testing Framework** | JUnit Jupiter 5 (`junit-jupiter` 5.11.3) | Unit and stress testing with parameterized tests and injected clock mocks. |
| **Core Concurrency** | Standard JDK Primitives | `Thread`, `Runnable`, `synchronized`, `wait()`, `notifyAll()`, `join()`, `volatile`, `AtomicLong`. |
| **Logging Subsystem** | `java.util.logging` | Structured application logging with rotating file handlers and single-line formatters. |

---

## 6. How to Build, Run, and Verify

### Prerequisites
- **Java Development Kit (JDK)**: JDK 17 or higher (tested on OpenJDK 27).
- **Apache Maven**: Version 3.9+ installed and configured on your system `PATH`.
- **Display Environment**: An active X11 or Wayland desktop session for GUI display.

### 6.1 Compile the Application
To compile all production source files with strict linting:
```bash
mvn clean compile
```

### 6.2 Package the Application JAR
To package the compiled application into a runnable JAR file:
```bash
mvn package -DskipTests
```
The resulting JAR is generated at `target/multithreaded-in-memory-cache-1.0.0.jar`.

### 6.3 Launch the Main JavaFX Desktop Application
To launch the primary JavaFX desktop cache management application:
```bash
mvn javafx:run
```
*Alternatively, run the packaged JAR directly:*
```bash
java -cp "target/multithreaded-in-memory-cache-1.0.0.jar:target/dependency/*" com.cse309.cache.ui.Launcher
```

### 6.4 Launch the Standalone AWT Event Console
To launch the isolated AWT Delegation Event Model console in its own process:
```bash
java -cp target/classes com.cse309.cache.awt.AwtEventConsole
```
*(You can also launch this directly from inside the JavaFX application by navigating to **Tools > Launch AWT Event Console** or clicking the AWT button on the toolbar.)*

---

## 7. Keyboard Shortcuts Reference

The JavaFX desktop application provides intuitive keyboard accelerators:
- **Ctrl + 1** to **Ctrl + 9**: Switch directly to Tabs 1 through 9.
- **Ctrl + S**: Open the Save Snapshot File dialog.
- **Ctrl + O**: Open the Load Snapshot File dialog.
- **Ctrl + E**: Export all live cache entries to CSV.
- **F5**: Refresh Explorer, Statistics, and Dashboard views.
- **Ctrl + Q**: Gracefully exit the application.