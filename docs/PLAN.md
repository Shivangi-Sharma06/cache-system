# Execution Plan: Multithreaded In-Memory Caching System (CSE 309)

## 1. Overview and Objectives
Build a production-grade, university-level Multithreaded Java-Based In-Memory Caching System with Concurrent Access Management.
The project strictly satisfies all CSE 309 (Advanced Java Programming) syllabus requirements across Units 1 through 5, enforces thread safety using core Java concurrency primitives, implements three independent eviction policies (LRU, FIFO, LFU), features wall-clock TTL with an active purge daemon, persists data via SQLite and binary `.jcs` file I/O, exposes a rich JavaFX desktop UI with 9 dedicated tabs, and includes an isolated AWT Delegation Event Console and educational Concepts Lab.

---

## 2. Phases & Milestones

### Phase 0: Project Setup and Infrastructure [COMPLETED]
- Configure `pom.xml` with:
  - Java release 17 target compiler
  - JavaFX controls (`org.openjfx:javafx-controls`)
  - SQLite JDBC (`org.xerial:sqlite-jdbc`)
  - JUnit Jupiter 5 (`org.junit.jupiter:junit-jupiter`)
  - Plugins: `maven-compiler-plugin` (with `-Xlint:all`), `maven-surefire-plugin`, `javafx-maven-plugin`
- Set up directory structure under `src/main/java/com/cse309/cache` and `src/test/java/com/cse309/cache`
- Implement `app.AppPaths` (resolving `data/`, `logs/`, `data/snapshots/`)
- Implement `logging.SingleLineFormatter`, `logging.InMemoryLogHandler`, and `logging.AppLogging`
- Create minimal `ui.Launcher` and `ui.CacheApplication`
- **Gate**: `mvn clean test` succeeds; verify compilation with `-Xlint:all`. (PASSED)

### Phase 1: Core Model, Exceptions, and Configuration [COMPLETED]
- Implement exception hierarchy in `exception`:
  - `CacheException` (root checked exception, `getUserMessage()`)
  - `InvalidCacheOperationException`
  - `InvalidCacheCapacityException`
  - `CacheKeyNotFoundException`
  - `CacheConfigurationException`
  - `CachePersistenceException`
  - `SnapshotFormatException`
- Implement core data transfer and result models in `core`:
  - `CacheEntry<V>` (package-private, mutable internal representation)
  - `CacheEntrySnapshot<K,V>` (immutable, full metadata)
  - `CacheLookupResult<V>` (`HIT`, `MISS`, `EXPIRED`)
  - `CacheWriteResult<K>` (`INSERTED`, `REPLACED`, evictions list)
  - `CacheDefaults` (constants)
- Implement configuration in `config`:
  - `CacheConfiguration` (immutable, constructor chaining with `this(...)`, static `Builder`)
  - `ConfigurationValidator` (validates all fields together, plain Java)
- Implement unit tests for configuration validation and exception behavior.
- **Gate**: `mvn -q test` passes all Phase 1 unit tests. (PASSED)

### Phase 2: Cache Engine, Interfaces, and Eviction Policies [COMPLETED]
- Implement `event`:
  - `CacheEvent`, `CacheEventListener`, `CacheEventAdapter`
- Implement `eviction`:
  - `EvictionPolicyType` (`LRU`, `FIFO`, `LFU`)
  - `EvictionRecord` (with detailed reason and English explanation)
  - `EvictionPolicy<K>` generic interface
  - `AbstractEvictionPolicy<K>`
  - `LeastRecentlyUsedPolicy<K>` (using `LinkedHashSet<K>`)
  - `FirstInFirstOutPolicy<K>` (using `LinkedHashSet<K>`)
  - `LeastFrequentlyUsedPolicy<K>` (using `TreeSet` with `(frequency, sequence)` and `HashMap` node index)
- Implement `core`:
  - `Cache<K,V>` interface
  - `AbstractCache<K,V>` abstract class
  - `ThreadSafeCache<K,V>` (single private lock, synchronized methods, leaf-lock design)
  - `StringCache` (subclass enforcing 250-char key and 8192-char value limits)
  - `CacheFactory`
- Implement atomic policy switching (`setEvictionPolicy` with `rebuild`) and capacity changes (`setCapacity`, `previewCapacityChange`)
- Write tests: `ThreadSafeCacheTest`, `LeastRecentlyUsedPolicyTest`, `FirstInFirstOutPolicyTest`, `LeastFrequentlyUsedPolicyTest`, `PolicySwitchTest`, `CapacityChangeTest`.
- **Gate**: All Phase 2 tests pass, specifically confirming the section 7 worked examples. (PASSED)

### Phase 3: TTL and Expiration Daemon [COMPLETED]
- Implement `expiry`:
  - `ExpiryIndex` (backed by `TreeSet` ordered by `(expiresAt, insertionSequence)`)
  - `CacheExpirationService` (daemon `Thread`, `wait(interval)`/`notifyAll()`, clean shutdown with `join(2000)`)
- Connect lazy expiry on `get`, `containsKey`, `size`, and active sweep via `purgeExpired()`
- Implement `MutableClock` in test scaffolding for deterministic instant testing without `Thread.sleep`
- Write tests: `ExpirationTest` and `CacheExpirationServiceTest`.
- **Gate**: Expiration tests pass with 100% deterministic clock ticks. (PASSED)

### Phase 4: Statistics and Operation History [COMPLETED]
- Implement `stats`:
  - `LatencyHistogram` (fixed `long[]` buckets)
  - `CacheStatistics` (`AtomicLong` counters, nanosecond timings, formulas for hit/miss/utilization/throughput)
  - `StatisticsSnapshot` (immutable snapshot)
  - `StatisticsExporter` (CSV output using `BufferedWriter`)
- Implement `history`:
  - `OperationRecord` (immutable)
  - `OperationHistory` (bounded `LinkedList` with `addLast`/`removeFirst` under its own lock)
- Write tests: `StatisticsTest` and `OperationHistoryTest`.
- **Gate**: `StatisticsTest` passes; conservation invariants and mathematical boundaries verified. (PASSED)

### Phase 5: Concurrency, Threading, and Workload Simulation [COMPLETED]
- Implement `threading`:
  - `BoundedBuffer<T>` (generic thread-safe buffer with `wait`/`notifyAll` in while loops, `close()`)
  - `PauseGate` (inter-thread pause/resume using `wait`/`notifyAll`)
  - `WorkerThread extends Thread` (takes tasks from buffer, executes on shared cache)
  - `WorkerPool` (names `cache-worker-N`, priority management)
  - `ThreadStateMonitor` (inspects `Thread.getState()`)
- Implement `simulation`:
  - `KeyDistribution` (`UNIFORM`, `HOTSPOT_80_20`, `SEQUENTIAL_SCAN`)
  - `OperationMix` (percentages summing to 100, presets)
  - `OperationGenerator` (deterministic with optional seed)
  - `OperationProducerTask implements Runnable`
  - `SimulationProgress` (immutable progress snapshot)
  - `SimulationController` (state management: Start, Pause, Resume, Stop)
- Write tests: `BoundedBufferTest`, `SimulationControllerTest`, and `ConcurrencyStressTest` (16 threads x 20,000 operations).
- **Gate**: Run `ConcurrencyStressTest` and `SimulationControllerTest` 20 times in a row with zero failures. (PASSED - 20/20 SUCCESS)

### Phase 6: Persistence Layer (SQLite & Binary `.jcs`) [COMPLETED]
- Implement `persistence`:
  - `ValueCodec<V>` interface and `StringValueCodec`
  - `PersistenceStatus` enum/record and `PersistenceMode`
  - `DatabaseManager` (SQLite connection, WAL mode, foreign keys, schema migrations)
  - `Repository<T, ID>` generic interface
  - `SnapshotRepository`, `OperationHistoryRepository`, `BenchmarkRepository` (JDBC with `PreparedStatement`, batching, rollback)
  - `SnapshotFileManager` (`DataOutputStream`/`DataInputStream`, magic `0x4A435331`, CRC32, atomic replacement with temp file)
  - `PersistenceCoordinator` (background write-behind worker, scheduled or manual snapshots)
- Implement `config.ConfigurationStore` (reads and writes `config.properties` via `FileInputStream`/`FileOutputStream`)
- Write tests: `SnapshotFileManagerTest`, SQLite repository tests, transaction rollback tests, `ConfigurationStoreTest`.
- **Gate**: All persistence tests pass. (PASSED)

### Phase 7: Benchmark Utility & Concepts Lab [COMPLETED]
- Implement `benchmark`:
  - `BenchmarkRunner` (clean isolated private caches, single-threaded determinism, multi-threaded scaling, warmups + repetitions)
  - `BenchmarkResult`
- Implement `lab`:
  - `RaceConditionLab` (`UnsafeCounterBox` vs `SafeCounterBox`, concurrent HashMap demonstration)
  - `CollectionsLab` (benchmarking `ArrayList`, `LinkedList`, `Vector`, `HashMap`, `Hashtable`, `TreeMap`, `TreeSet`, CME demo)
  - `Student` (implements `Comparable<Student>`, overrides `equals`, `hashCode`, `toString`)
  - `GenericCacheLab` (`Cache<Integer,String>`, `Cache<String,Student>`, `Cache<Long,Double>`, wildcards and bounded generics)
- Implement tests: `BenchmarkRunnerTest`, `RaceConditionLabTest`, `GenericsTest`, `CollectionsLabTest`.
- **Gate**: All Phase 7 tests pass. (PASSED)

### Phase 8: Desktop UI (JavaFX & AWT Console) [COMPLETED]
- Implement `app.CacheManager` (composition root wiring all subsystems, clean shutdown)
- Implement `ui.InputValidator` and `ui.UiEventBuffer`
- Implement `ui.IconFactory` (Canvas snapshotting to `WritableImage` for menu icons)
- Implement JavaFX UI tabs in `ui`:
  1. `DashboardTab` (metric cards, DropShadow, Glow, Scale, hit-ratio LineChart, eviction log)
  2. `OperationsTab` (interactive cache CRUD, latency display, recent outcomes)
  3. `ExplorerTab` (filtered TableView, Key TreeView grouped by prefix before `:`)
  4. `ConfigurationTab` (interactive settings, RadioButton policy switch, capacity cut warning)
  5. `SimulatorTab` (simulation controls, live progress, worker thread state list, live log)
  6. `StatisticsTab` (latency tables, histogram BarChart, throughput chart, benchmark runner UI)
  7. `PersistenceTab` (file snapshot chooser, database snapshot table, CSV exporters)
  8. `HistoryTab` (operation history table with filter, log level selector)
  9. `ConceptsLabTab` (interactive runners for race condition, collections, and generic cache labs)
- Implement `awt.AwtEventConsole` (standalone `Frame`, Delegation Event Model, `ActionListener`, `KeyListener`, `KeyAdapter`, `WindowAdapter`)
- Implement `ProcessBuilder` launcher for AWT Console from JavaFX Tools menu.
- **Gate**: UI builds cleanly; manual checklist items prepared and verified. (PASSED)

### Phase 9: Hardening & Verification [COMPLETED]
- Implement `ShutdownTest` (verifies all app-spawned non-daemon threads terminate on shutdown)
- Thread hygiene verification: test interruption, gate resumption, buffer drainage
- Stress testing under heavy concurrent load (320,000 ops and 500,000 operations simulation test)
- Uncaught exception handlers verification across threads.
- Comprehensive audit: zero TODOs, zero printStackTrace, zero empty catch blocks, all 42 syllabus keywords verified.
- **Gate**: All 91 tests pass cleanly; clean shutdown confirmed; 500,000-op run verified. (PASSED)

### Phase 10: Documentation & Deliverables [COMPLETED]
- Compile `README.md` following the required 15-section outline, including verified Syllabus Coverage Contract.
- Compile `docs/VIVA.md` with at least 35 thorough Q&As across Units 1–5.
- Compile `docs/MANUAL_TEST_CHECKLIST.md` with numbered verification steps.
- Compile `docs/DECISIONS.md` documenting all implementation decisions.
- Compile `docs/screenshots/README.md` listing required human screenshots.
- Conduct final self-audit against required syllabus keywords and language features. (PASSED)

---

## 3. Comprehensive Class List by Package

| Package | Class / Interface / Enum | Purpose |
|---|---|---|
| `core` | `Cache<K,V>` | Generic public cache interface |
| `core` | `AbstractCache<K,V>` | Base implementation with template methods |
| `core` | `ThreadSafeCache<K,V>` | Core thread-safe cache engine using single private lock |
| `core` | `StringCache` | String-specialized cache with key/value size constraints |
| `core` | `CacheEntry<V>` | Package-private mutable internal cache entry |
| `core` | `CacheEntrySnapshot<K,V>` | Immutable snapshot of cache entry metadata and value |
| `core` | `CacheLookupResult<V>` | Value object for cache lookups (`HIT`, `MISS`, `EXPIRED`) |
| `core` | `CacheWriteResult<K>` | Value object for cache writes (`INSERTED`, `REPLACED`, evictions) |
| `core` | `CacheFactory` | Static factories for creating cache instances |
| `core` | `CacheDefaults` | Default constants (capacities, TTLs, intervals) |
| `core` | `CollectionTools` | Generic utility with bounded generic method `<T extends Comparable<? super T>>` |
| `eviction` | `EvictionPolicy<K>` | Generic eviction policy interface |
| `eviction` | `AbstractEvictionPolicy<K>` | Shared eviction logic |
| `eviction` | `LeastRecentlyUsedPolicy<K>` | O(1) LRU policy using `LinkedHashSet<K>` |
| `eviction` | `FirstInFirstOutPolicy<K>` | O(1) FIFO policy using `LinkedHashSet<K>` |
| `eviction` | `LeastFrequentlyUsedPolicy<K>` | O(log n) LFU policy using `TreeSet` & `HashMap` |
| `eviction` | `EvictionPolicyType` | Enum for LRU, FIFO, LFU |
| `eviction` | `EvictionRecord` | Record describing key evicted, reason, and human explanation |
| `expiry` | `ExpiryIndex` | Ordered `TreeSet` index of expiring entries |
| `expiry` | `CacheExpirationService` | Dedicated daemon thread for periodic sweep |
| `event` | `CacheEvent` | Cache mutation event object |
| `event` | `CacheEventListener` | Listener interface for cache events |
| `event` | `CacheEventAdapter` | Adapter class providing no-op default implementations |
| `stats` | `CacheStatistics` | Thread-safe performance counters (`AtomicLong`) |
| `stats` | `StatisticsSnapshot` | Immutable statistics snapshot |
| `stats` | `LatencyHistogram` | Bucketed latency array (`long[]`) |
| `stats` | `StatisticsExporter` | CSV export utility using `BufferedWriter` |
| `history` | `OperationRecord` | Immutable log entry for an executed operation |
| `history` | `OperationHistory` | Bounded thread-safe `LinkedList` log |
| `threading` | `BoundedBuffer<T>` | Generic thread-safe blocking buffer with `wait`/`notifyAll` |
| `threading` | `PauseGate` | Thread synchronization gate with `wait`/`notifyAll` |
| `threading` | `WorkerThread` | Custom `Thread` subclass running simulation tasks |
| `threading` | `WorkerPool` | Manager for worker threads and priorities |
| `threading` | `ThreadStateMonitor` | Inspects thread states (`Thread.getState()`) |
| `simulation` | `KeyDistribution` | Distribution enum: `UNIFORM`, `HOTSPOT_80_20`, `SEQUENTIAL_SCAN` |
| `simulation` | `OperationMix` | Configurable operation ratio container |
| `simulation` | `OperationGenerator` | Deterministic operation generation |
| `simulation` | `OperationProducerTask` | `Runnable` generating operations into `BoundedBuffer` |
| `simulation` | `SimulationConfig` | Immutable configuration for a simulation run |
| `simulation` | `SimulationProgress` | Immutable snapshot of live simulation metrics |
| `simulation` | `SimulationController` | Orchestrates simulation start, pause, resume, and stop |
| `persistence` | `DatabaseManager` | SQLite connection manager and idempotent schema creator |
| `persistence` | `Repository<T,ID>` | Generic CRUD repository interface |
| `persistence` | `SnapshotRepository` | SQLite snapshot repository |
| `persistence` | `OperationHistoryRepository`| SQLite history log repository |
| `persistence` | `BenchmarkRepository` | SQLite benchmark run repository |
| `persistence` | `SnapshotFileManager` | Custom binary `.jcs` format read/write with CRC32 |
| `persistence` | `ValueCodec<V>` | Codec interface for value serialization |
| `persistence` | `StringValueCodec` | Codec implementation for String values |
| `persistence` | `PersistenceCoordinator` | Background persistence coordinator |
| `persistence` | `PersistenceStatus` | Status record for persistence health |
| `persistence` | `PersistenceMode` | Enum: `IN_MEMORY_ONLY`, `PERSISTENT` |
| `config` | `CacheConfiguration` | Immutable configuration with chained constructors & Builder |
| `config` | `ConfigurationValidator` | Plain Java validator collecting all errors |
| `config` | `ConfigurationStore` | Properties store using `FileInputStream`/`FileOutputStream` |
| `exception` | `CacheException` | Root checked exception with technical and user messages |
| `exception` | `InvalidCacheOperationException` | Invalid operation parameters or state |
| `exception` | `InvalidCacheCapacityException` | Invalid capacity argument |
| `exception` | `CacheKeyNotFoundException` | Key missing on update |
| `exception` | `CacheConfigurationException` | Multiple configuration validation failures |
| `exception` | `CachePersistenceException` | Database and file I/O errors |
| `exception` | `SnapshotFormatException` | Corrupt or invalid snapshot file format |
| `logging` | `AppLogging` | Logger configuration utility |
| `logging` | `SingleLineFormatter` | Compact single-line formatter |
| `logging` | `InMemoryLogHandler` | Circular memory buffer for UI log display |
| `benchmark` | `BenchmarkRunner` | Deterministic benchmark execution engine |
| `benchmark` | `BenchmarkResult` | Value object containing 2-D results and metrics |
| `lab` | `RaceConditionLab` | Educational concurrent counter and map test |
| `lab` | `CollectionsLab` | Educational collection comparison and CME demonstration |
| `lab` | `Student` | Sample domain class overriding `equals`, `hashCode`, `toString`, `Comparable` |
| `lab` | `GenericCacheLab` | Multi-type generic demonstration |
| `app` | `AppPaths` | Path resolver for `data/`, `logs/`, etc. |
| `app` | `CacheManager` | Central application facade and lifecycle coordinator |
| `ui` | `Launcher` | Plain Java main class to launch JavaFX without module issues |
| `ui` | `CacheApplication` | Main JavaFX `Application` subclass |
| `ui` | `IconFactory` | Procedural 16x16 icon generator via Canvas snapshots |
| `ui` | `InputValidator` | UI input validation helper |
| `ui` | `UiEventBuffer` | Thread-safe UI event buffer |
| `ui` | `DashboardTab` | Dashboard UI view |
| `ui` | `OperationsTab` | Cache CRUD operations UI view |
| `ui` | `ExplorerTab` | Cache explorer and prefix TreeView view |
| `ui` | `ConfigurationTab` | Configuration management UI view |
| `ui` | `SimulatorTab` | Concurrency workload simulator UI view |
| `ui` | `StatisticsTab` | Real-time statistics and benchmark UI view |
| `ui` | `PersistenceTab` | Snapshot and SQLite management UI view |
| `ui` | `HistoryTab` | Operations history and logging UI view |
| `ui` | `ConceptsLabTab` | Educational lab runners UI view |
| `awt` | `AwtEventConsole` | Standalone AWT Frame demonstrating Delegation Event Model |

---

## 4. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Race conditions in cache engine | Single private lock guards all mutable cache state. Zero lock acquisition inside callbacks. Conservation invariants asserted under high concurrent stress. |
| Deadlocks during events/persistence | Strictly no external callbacks or blocking I/O while holding engine lock. Leaf-lock discipline. |
| Non-deterministic expiration tests | Injected `java.time.Clock` with test-controlled `MutableClock`. Zero reliance on `Thread.sleep` for expiry assertions. |
| UI thread freezing under heavy load | Heavy tasks dispatched via JavaFX `Task`. UI updates throttled via `Timeline` polling immutable snapshots every 250ms. |
| Corrupt snapshot file crash | Binary format validated upfront (magic bytes, version, count, CRC32 checksum) before engine modification. Atomic file replacement with temp file. |
| SQLite locking / concurrent write failure | WAL mode enabled (`PRAGMA journal_mode=WAL`), busy timeout set to 5000ms. Operations isolated to dedicated background threads. |
