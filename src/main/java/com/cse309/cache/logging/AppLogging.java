package com.cse309.cache.logging;

import com.cse309.cache.app.AppPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global logging configurator for com.cse309.cache.
 * Installs ConsoleHandler, rotating FileHandler, and an InMemoryLogHandler for GUI viewing.
 */
public final class AppLogging {

    private static final String ROOT_LOGGER_NAME = "com.cse309.cache";
    private static final int FILE_LIMIT_BYTES = 1024 * 1024; // 1 MB
    private static final int FILE_COUNT = 3;

    private static final InMemoryLogHandler IN_MEMORY_HANDLER = new InMemoryLogHandler(500);
    private static boolean configured = false;

    private AppLogging() {
        // Utility class
    }

    public static synchronized void configure(Level initialLevel) {
        if (configured) {
            setLevel(initialLevel);
            return;
        }

        try {
            AppPaths.ensureDirectoriesExist();
        } catch (IOException e) {
            System.err.println("Failed to initialize log directory: " + e.getMessage());
        }

        Logger logger = Logger.getLogger(ROOT_LOGGER_NAME);
        logger.setUseParentHandlers(false);
        logger.setLevel(initialLevel != null ? initialLevel : Level.INFO);

        // Remove existing handlers if any
        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }

        SingleLineFormatter formatter = new SingleLineFormatter();

        // 1. Console Handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        // 2. Rotating File Handler
        try {
            String pattern = AppPaths.getLogsDir().resolve("cache-%g.log").toString();
            FileHandler fileHandler = new FileHandler(pattern, FILE_LIMIT_BYTES, FILE_COUNT, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Failed to initialize rotating file logger: " + e.getMessage());
        }

        // 3. In-Memory Handler for UI
        IN_MEMORY_HANDLER.setLevel(Level.ALL);
        IN_MEMORY_HANDLER.setFormatter(formatter);
        logger.addHandler(IN_MEMORY_HANDLER);

        configured = true;
    }

    public static InMemoryLogHandler getInMemoryLogHandler() {
        return IN_MEMORY_HANDLER;
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    public static void setLevel(Level level) {
        Logger logger = Logger.getLogger(ROOT_LOGGER_NAME);
        logger.setLevel(level);
    }

    public static Level getLevel() {
        return Logger.getLogger(ROOT_LOGGER_NAME).getLevel();
    }
}
