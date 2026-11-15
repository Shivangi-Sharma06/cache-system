package com.cse309.cache.ui;

import com.cse309.cache.app.AppPaths;
import com.cse309.cache.app.CacheManager;
import com.cse309.cache.config.ConfigurationStore;
import com.cse309.cache.event.CacheEvent;
import com.cse309.cache.event.CacheEventListener;
import com.cse309.cache.eviction.EvictionRecord;
import com.cse309.cache.logging.AppLogging;
import com.cse309.cache.persistence.PersistenceStatus;
import com.cse309.cache.simulation.SimulationProgress;
import com.cse309.cache.stats.StatisticsSnapshot;
import com.cse309.cache.threading.ThreadStateMonitor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main JavaFX Application with 4 core tabs (Dashboard, Operations, Simulator, Persistence),
 * menu bar, procedural icons, toolbar, status bar, and 250ms Timeline refresh polling (Unit 5).
 */
public class CacheApplication extends Application {

    private static final Logger LOGGER = AppLogging.getLogger(CacheApplication.class);

    private CacheManager cacheManager;
    private final UiEventBuffer uiEventBuffer = new UiEventBuffer(1000);
    private Timeline timeline;

    private TabPane tabPane;
    private DashboardTab dashboardTab;
    private OperationsTab operationsTab;
    private SimulatorTab simulatorTab;
    private PersistenceTab persistenceTab;

    // Status bar labels
    private final Label statusPersistenceLabel = new Label("Persistence: INITIALIZING");
    private final Label statusPolicyLabel = new Label("Policy: LRU");
    private final Label statusThreadsLabel = new Label("Workers: 0");
    private final Label statusMessageLabel = new Label("Ready");

    private final List<EvictionRecord> recentEvictionsForDashboard = new ArrayList<>();

    @Override
    public void init() {
        AppLogging.configure(Level.INFO);
        this.cacheManager = new CacheManager(ConfigurationStore.load(AppPaths.getConfigFile()));

        // Buffer events for UI
        this.cacheManager.getCache().addListener(new CacheEventListener() {
            @Override
            public void onEvent(CacheEvent event) {
                uiEventBuffer.add(event);
            }
        });
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Multithreaded In-Memory Caching System (CSE 309)");

        // JavaFX Thread Uncaught Exception Handler (Unit 2, Section 15)
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception on JavaFX thread: " + e.getMessage(), e);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "An error occurred: " + e.getMessage());
                alert.showAndWait();
            });
        });

        BorderPane root = new BorderPane();

        // 1. MenuBar & ToolBar on Top
        MenuBar menuBar = createMenuBar(primaryStage);
        ToolBar toolBar = createToolBar(primaryStage);
        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // 2. TabPane in Center
        tabPane = new TabPane();
        dashboardTab = new DashboardTab(cacheManager);
        operationsTab = new OperationsTab(cacheManager);
        simulatorTab = new SimulatorTab(cacheManager);
        persistenceTab = new PersistenceTab(cacheManager);

        tabPane.getTabs().addAll(
                dashboardTab,
                operationsTab,
                simulatorTab,
                persistenceTab
        );
        root.setCenter(tabPane);

        // 3. Status Bar at Bottom
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1150, 750);
        setupKeyboardAccelerators(scene, primaryStage);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);

        primaryStage.setOnCloseRequest(e -> {
            if (cacheManager.getSimulationController().isActive()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "A simulation is currently running. Stopping now will cancel it. Exit application?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Simulation In Progress");
                confirm.showAndWait().ifPresent(res -> {
                    if (res != ButtonType.YES) {
                        e.consume();
                    }
                });
            }
        });

        primaryStage.show();

        // Start 250ms polling Timeline (Responsiveness Rule 3)
        startPollingTimeline();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        // --- File Menu ---
        Menu fileMenu = new Menu("File");
        MenuItem saveSnap = new MenuItem("Save Snapshot File...", IconFactory.createIcon(IconFactory.IconType.SAVE));
        saveSnap.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveSnap.setOnAction(e -> handleSaveSnapshotFile(stage));

        MenuItem loadSnap = new MenuItem("Load Snapshot File...", IconFactory.createIcon(IconFactory.IconType.LOAD));
        loadSnap.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        loadSnap.setOnAction(e -> handleLoadSnapshotFile(stage));

        MenuItem exportEntries = new MenuItem("Export Entries CSV...", IconFactory.createIcon(IconFactory.IconType.EXPORT));
        exportEntries.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));
        exportEntries.setOnAction(e -> tabPane.getSelectionModel().select(persistenceTab));

        MenuItem exitItem = new MenuItem("Exit", IconFactory.createIcon(IconFactory.IconType.EXIT));
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitItem.setOnAction(e -> {
            Platform.exit();
        });

        fileMenu.getItems().addAll(saveSnap, loadSnap, exportEntries, new SeparatorMenuItem(), exitItem);

        // --- Cache Menu ---
        Menu cacheMenu = new Menu("Cache");
        MenuItem clearItem = new MenuItem("Clear Cache", IconFactory.createIcon(IconFactory.IconType.CLEAR));
        clearItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Clear all cache entries?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) cacheManager.getCache().clear();
            });
        });

        MenuItem purgeItem = new MenuItem("Purge Expired Now", IconFactory.createIcon(IconFactory.IconType.PURGE));
        purgeItem.setOnAction(e -> {
            int purged = cacheManager.getCache().purgeExpired().size();
            statusMessageLabel.setText("Purged " + purged + " expired entries.");
        });

        MenuItem resetStats = new MenuItem("Reset Statistics", IconFactory.createIcon(IconFactory.IconType.STATS));
        resetStats.setOnAction(e -> cacheManager.getStatistics().reset());

        cacheMenu.getItems().addAll(clearItem, purgeItem, resetStats);

        // --- Tools Menu ---
        Menu toolsMenu = new Menu("Tools");
        MenuItem startSim = new MenuItem("Start Simulation", IconFactory.createIcon(IconFactory.IconType.START));
        startSim.setOnAction(e -> tabPane.getSelectionModel().select(simulatorTab));

        toolsMenu.getItems().add(startSim);

        // --- View Menu ---
        Menu viewMenu = new Menu("View");
        KeyCode[] digitKeys = {
            KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4
        };
        for (int i = 0; i < 4; i++) {
            final int index = i;
            MenuItem tabItem = new MenuItem("Switch to Tab " + (index + 1) + " (" + getTabName(index) + ")");
            tabItem.setAccelerator(new KeyCodeCombination(digitKeys[index], KeyCombination.CONTROL_DOWN));
            tabItem.setOnAction(e -> tabPane.getSelectionModel().select(index));
            viewMenu.getItems().add(tabItem);
        }

        // --- Help Menu ---
        Menu helpMenu = new Menu("Help");
        MenuItem shortcuts = new MenuItem("Keyboard Shortcuts");
        shortcuts.setOnAction(e -> showShortcutsDialog());

        MenuItem about = new MenuItem("About", IconFactory.createIcon(IconFactory.IconType.ABOUT));
        about.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().addAll(shortcuts, about);

        menuBar.getMenus().addAll(fileMenu, cacheMenu, toolsMenu, viewMenu, helpMenu);
        return menuBar;
    }

    private String getTabName(int idx) {
        return switch (idx) {
            case 0 -> "Dashboard";
            case 1 -> "Operations";
            case 2 -> "Simulator";
            case 3 -> "Persistence";
            default -> "Tab";
        };
    }

    private ToolBar createToolBar(Stage stage) {
        Button saveBtn = new Button("", IconFactory.createIcon(IconFactory.IconType.SAVE));
        saveBtn.setTooltip(new Tooltip("Save Snapshot (.jcs)"));
        saveBtn.setOnAction(e -> handleSaveSnapshotFile(stage));

        Button loadBtn = new Button("", IconFactory.createIcon(IconFactory.IconType.LOAD));
        loadBtn.setTooltip(new Tooltip("Load Snapshot (.jcs)"));
        loadBtn.setOnAction(e -> handleLoadSnapshotFile(stage));

        Button refreshBtn = new Button("", IconFactory.createIcon(IconFactory.IconType.REFRESH));
        refreshBtn.setTooltip(new Tooltip("Refresh Snapshots (F5)"));
        refreshBtn.setOnAction(e -> {
            persistenceTab.loadDatabaseSnapshotsAsync();
        });

        Button purgeBtn = new Button("", IconFactory.createIcon(IconFactory.IconType.PURGE));
        purgeBtn.setTooltip(new Tooltip("Purge Expired Now"));
        purgeBtn.setOnAction(e -> {
            int purged = cacheManager.getCache().purgeExpired().size();
            statusMessageLabel.setText("Purged " + purged + " expired entries.");
        });

        Button startSimBtn = new Button("", IconFactory.createIcon(IconFactory.IconType.START));
        startSimBtn.setTooltip(new Tooltip("Start Simulation Tab"));
        startSimBtn.setOnAction(e -> tabPane.getSelectionModel().select(simulatorTab));

        Button stopSimBtn = new Button("", IconFactory.createIcon(IconFactory.IconType.STOP));
        stopSimBtn.setTooltip(new Tooltip("Stop Simulation"));
        stopSimBtn.setOnAction(e -> cacheManager.getSimulationController().stop());

        Button statsBtn = new Button("", IconFactory.createIcon(IconFactory.IconType.STATS));
        statsBtn.setTooltip(new Tooltip("Reset Statistics"));
        statsBtn.setOnAction(e -> cacheManager.getStatistics().reset());

        return new ToolBar(saveBtn, loadBtn, refreshBtn, purgeBtn, startSimBtn, stopSimBtn, statsBtn);
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(5, 12, 5, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #ced4da; -fx-border-width: 1 0 0 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(statusPersistenceLabel, statusPolicyLabel, statusThreadsLabel, spacer, statusMessageLabel);
        return bar;
    }

    private void setupKeyboardAccelerators(Scene scene, Stage stage) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> {
            persistenceTab.loadDatabaseSnapshotsAsync();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN), () -> tabPane.getSelectionModel().select(0));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN), () -> tabPane.getSelectionModel().select(1));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN), () -> tabPane.getSelectionModel().select(2));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN), () -> tabPane.getSelectionModel().select(3));
    }

    private void startPollingTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.millis(250), e -> onTick250ms()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void onTick250ms() {
        var cache = cacheManager.getCache();
        StatisticsSnapshot stats = cache.getStatistics().snapshot(cache.size(), cache.getCapacity());
        SimulationProgress simProgress = cacheManager.getSimulationController().getProgress();
        List<ThreadStateMonitor.WorkerStatus> workerStatuses = cacheManager.getSimulationController().getWorkerStatuses();

        // Drain UI event buffer
        List<CacheEvent> events = uiEventBuffer.drain();
        recentEvictionsForDashboard.clear();
        for (CacheEvent ev : events) {
            if (ev.getType() == CacheEvent.EventType.EVICT && ev.getDetails() instanceof EvictionRecord record) {
                recentEvictionsForDashboard.add(record);
            }
        }

        // Update tabs
        dashboardTab.update(stats, simProgress, recentEvictionsForDashboard);
        simulatorTab.update(simProgress, workerStatuses);

        // Update status bar
        PersistenceStatus pStat = cacheManager.getPersistenceCoordinator().getStatus();
        statusPersistenceLabel.setText("Persistence: " + (pStat.isAvailable() ? "AVAILABLE" : "UNAVAILABLE"));
        statusPolicyLabel.setText("Policy: " + cache.getEvictionPolicyType().name());
        statusThreadsLabel.setText(String.format("Workers: %d running", simProgress.getRunningWorkers()));
    }

    private void handleSaveSnapshotFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Cache Snapshot (.jcs)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Cache Snapshot (*.jcs)", "*.jcs"));
        chooser.setInitialFileName("cache-snapshot.jcs");
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                cacheManager.saveBinarySnapshot(file.toPath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Snapshot saved to " + file.getName());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save snapshot: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void handleLoadSnapshotFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Cache Snapshot (.jcs)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Cache Snapshot (*.jcs)", "*.jcs"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                cacheManager.loadBinarySnapshot(file.toPath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Snapshot loaded successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load snapshot: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void showShortcutsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Available Application Shortcuts");
        alert.setContentText("""
            Ctrl+1 .. Ctrl+4 : Switch between Tabs 1 to 4
            Ctrl+S           : Save binary snapshot file (.jcs)
            Ctrl+O           : Load binary snapshot file (.jcs)
            Ctrl+E           : Export entries to CSV
            F5               : Refresh snapshots list
            Ctrl+Q           : Exit application
            """);
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Multithreaded In-Memory Caching System");
        alert.setContentText("""
            Course: CSE 309 Advanced Java Programming
            Architecture: High-performance In-Memory Key-Value Cache
            Concurrency: Core Java Thread, synchronized, wait/notifyAll, join
            Eviction: O(1) LRU, O(1) FIFO, O(log n) LFU
            Persistence: SQLite JDBC WAL + Atomic binary .jcs file I/O
            GUI: Pure JavaFX code (no FXML) with 250ms polling Timeline
            Author: Shivangi Sharma (CSE 309)
            """);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }
}
