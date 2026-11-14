package com.cse309.cache.expiry;

import com.cse309.cache.core.Cache;
import com.cse309.cache.core.CacheDefaults;
import com.cse309.cache.logging.AppLogging;

import java.time.Duration;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dedicated background daemon thread that periodically purges expired cache entries.
 * Concurrency primitives used: named Thread, daemon flag, synchronized(monitor), wait(timeout),
 * notifyAll(), interrupt(), join(2000), and thread state checking (Unit 3).
 */
public final class CacheExpirationService {

    private static final Logger LOGGER = AppLogging.getLogger(CacheExpirationService.class);

    private final Cache<?, ?> cache;
    private final Object monitor = new Object();

    private volatile Duration interval;
    private volatile boolean running;
    private Thread workerThread;

    public CacheExpirationService(Cache<?, ?> cache, Duration initialInterval) {
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
        setInterval(initialInterval != null ? initialInterval : CacheDefaults.DEFAULT_CLEANUP_INTERVAL);
    }

    public CacheExpirationService(Cache<?, ?> cache) {
        this(cache, CacheDefaults.DEFAULT_CLEANUP_INTERVAL);
    }

    public synchronized void start() {
        if (running && workerThread != null && workerThread.isAlive()) {
            return;
        }

        running = true;
        workerThread = new Thread(this::runLoop, "cache-expiry-service");
        workerThread.setDaemon(true);
        workerThread.setPriority(Thread.NORM_PRIORITY);
        workerThread.start();
        LOGGER.info("CacheExpirationService started with interval " + interval.toMillis() + "ms");
    }

    private void runLoop() {
        while (running) {
            long waitMillis = interval.toMillis();
            try {
                synchronized (monitor) {
                    if (running) {
                        monitor.wait(waitMillis);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (!running) {
                break;
            }

            try {
                int purged = cache.purgeExpired().size();
                if (purged > 0 && LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Periodic sweep purged " + purged + " expired entries");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during cache expiration sweep: " + e.getMessage(), e);
            }
        }
        LOGGER.info("CacheExpirationService background thread stopped");
    }

    public void stop() {
        synchronized (this) {
            if (!running) {
                return;
            }
            running = false;
        }

        synchronized (monitor) {
            monitor.notifyAll();
        }

        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning("Interrupted while joining expiration service thread");
            }
        }
    }

    public void setInterval(Duration newInterval) {
        if (newInterval == null ||
            newInterval.compareTo(CacheDefaults.MIN_CLEANUP_INTERVAL) < 0 ||
            newInterval.compareTo(CacheDefaults.MAX_CLEANUP_INTERVAL) > 0) {
            throw new IllegalArgumentException("Interval must be between " +
                    CacheDefaults.MIN_CLEANUP_INTERVAL.toMillis() + "ms and " +
                    CacheDefaults.MAX_CLEANUP_INTERVAL.toHours() + "h");
        }
        this.interval = newInterval;
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public Duration getInterval() {
        return interval;
    }

    public boolean isRunning() {
        return running && workerThread != null && workerThread.isAlive();
    }

    public Thread getWorkerThread() {
        return workerThread;
    }

    public int purgeNow() {
        return cache.purgeExpired().size();
    }
}
