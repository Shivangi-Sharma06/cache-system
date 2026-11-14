package com.cse309.cache.ui;

import javafx.application.Application;

/**
 * Plain Java entry point to launch JavaFX without requiring module path arguments.
 */
public final class Launcher {

    private Launcher() {
        // Prevent instantiation
    }

    public static void main(String[] args) {
        Application.launch(CacheApplication.class, args);
    }
}
