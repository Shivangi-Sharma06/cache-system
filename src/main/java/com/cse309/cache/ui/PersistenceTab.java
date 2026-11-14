package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.persistence.DbSnapshot;
import com.cse309.cache.persistence.PersistenceCoordinator;
import com.cse309.cache.persistence.PersistenceStatus;
import com.cse309.cache.stats.StatisticsExporter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tab 7: Persistence Management for binary .jcs snapshots, SQLite tables,
 * and data exports. Uses JavaFX Task for asynchronous I/O (Unit 5).
 */
public final class PersistenceTab extends Tab {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final CacheManager cacheManager;

    private final Label statusBanner = new Label();
    private final TableView<DbSnapshot> snapshotTable = new TableView<>();
    private final ObservableList<DbSnapshot> snapshotData = FXCollections.observableArrayList();

    public PersistenceTab(CacheManager cacheManager) {
        super("Persistence");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // Top Status Banner
        statusBanner.setStyle("-fx-padding: 8; -fx-background-color: #e9ecef; -fx-border-radius: 4; -fx-font-weight: bold;");
        statusBanner.setMaxWidth(Double.MAX_VALUE);
        updateStatusBanner();
        root.setTop(statusBanner);

        // Center: SQLite Snapshot Table & Actions
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(10, 0, 0, 0));

        Label tableTitle = new Label("Durable SQLite Snapshots (Warm-Start & Recovery)");
        tableTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        setupSnapshotTable();

        HBox dbButtonBar = new HBox(10);
        dbButtonBar.setAlignment(Pos.CENTER_LEFT);

        Button saveDbBtn = new Button("Save Current State to Database");
        Button restoreDbBtn = new Button("Restore Selected Snapshot");
        Button deleteDbBtn = new Button("Delete Selected");
        Button refreshDbBtn = new Button("Refresh Snapshots List");

        saveDbBtn.setStyle("-fx-base: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        restoreDbBtn.setStyle("-fx-base: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteDbBtn.setStyle("-fx-base: #dc3545; -fx-text-fill: white;");

        saveDbBtn.setOnAction(e -> handleSaveDbSnapshot());
        restoreDbBtn.setOnAction(e -> handleRestoreDbSnapshot());
        deleteDbBtn.setOnAction(e -> handleDeleteDbSnapshot());
        refreshDbBtn.setOnAction(e -> loadDatabaseSnapshotsAsync());

        dbButtonBar.getChildren().addAll(saveDbBtn, restoreDbBtn, deleteDbBtn, refreshDbBtn);

        // File I/O & CSV Export Actions Bar
        HBox fileButtonBar = new HBox(10);
        fileButtonBar.setAlignment(Pos.CENTER_LEFT);
        fileButtonBar.setPadding(new Insets(8, 0, 0, 0));

        Button saveFileBtn = new Button("Save .jcs Binary File");
        Button loadFileBtn = new Button("Load .jcs Binary File");
        Button exportEntriesBtn = new Button("Export Entries CSV");
        Button exportHistoryBtn = new Button("Export History CSV");
        Button exportStatsBtn = new Button("Export Statistics CSV");

        saveFileBtn.setOnAction(e -> handleSaveBinaryFile());
        loadFileBtn.setOnAction(e -> handleLoadBinaryFile());
        exportEntriesBtn.setOnAction(e -> handleExportEntriesCsv());
        exportHistoryBtn.setOnAction(e -> handleExportHistoryCsv());
        exportStatsBtn.setOnAction(e -> handleExportStatsCsv());

        fileButtonBar.getChildren().addAll(saveFileBtn, loadFileBtn, exportEntriesBtn, exportHistoryBtn, exportStatsBtn);

        centerBox.getChildren().addAll(tableTitle, snapshotTable, dbButtonBar, fileButtonBar);
        VBox.setVgrow(snapshotTable, Priority.ALWAYS);
        root.setCenter(centerBox);

        setContent(root);
        loadDatabaseSnapshotsAsync();
    }

    private void setupSnapshotTable() {
        TableColumn<DbSnapshot, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getId() != null ? d.getValue().getId() : 0));
        idCol.setPrefWidth(55);

        TableColumn<DbSnapshot, String> nameCol = new TableColumn<>("Snapshot Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<DbSnapshot, String> dateCol = new TableColumn<>("Created At");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(TIME_FORMATTER.format(Instant.ofEpochMilli(d.getValue().getCreatedAt()))));
        dateCol.setPrefWidth(140);

        TableColumn<DbSnapshot, String> polCol = new TableColumn<>("Policy");
        polCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvictionPolicy()));
        polCol.setPrefWidth(80);

        TableColumn<DbSnapshot, Number> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCapacity()));
        capCol.setPrefWidth(80);

        TableColumn<DbSnapshot, Number> countCol = new TableColumn<>("Entries Count");
        countCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEntryCount()));
        countCol.setPrefWidth(95);

        snapshotTable.getColumns().addAll(idCol, nameCol, dateCol, polCol, capCol, countCol);
        snapshotTable.setItems(snapshotData);
    }

    public void updateStatusBanner() {
        PersistenceStatus status = cacheManager.getPersistenceCoordinator().getStatus();
        if (status.isAvailable()) {
            statusBanner.setText("Persistence Status: AVAILABLE | Database connected at " +
                    cacheManager.getPersistenceCoordinator().getDatabaseManager().getDatabasePath().getFileName() +
                    " | Mode: " + cacheManager.getConfiguration().getPersistenceMode());
            statusBanner.setStyle("-fx-padding: 8; -fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-border-radius: 4; -fx-font-weight: bold;");
        } else {
            statusBanner.setText("Persistence Status: UNAVAILABLE | " + status.getLastError().orElse(""));
            statusBanner.setStyle("-fx-padding: 8; -fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-border-radius: 4; -fx-font-weight: bold;");
        }
    }

    public void loadDatabaseSnapshotsAsync() {
        Task<List<DbSnapshot>> task = new Task<>() {
            @Override
            protected List<DbSnapshot> call() throws Exception {
                return cacheManager.getPersistenceCoordinator().getSnapshotRepository().findAll();
            }
        };

        task.setOnSucceeded(e -> {
            snapshotData.setAll(task.getValue());
            updateStatusBanner();
        });

        new Thread(task, "load-snapshots-task").start();
    }

    private void handleSaveDbSnapshot() {
        TextInputDialog dialog = new TextInputDialog("snapshot-" + System.currentTimeMillis());
        dialog.setTitle("Save Database Snapshot");
        dialog.setHeaderText("Save Point-in-Time Cache Snapshot to SQLite");
        dialog.setContentText("Snapshot Name:");

        dialog.showAndWait().ifPresent(name -> {
            Task<DbSnapshot> task = new Task<>() {
                @Override
                protected DbSnapshot call() throws Exception {
                    return cacheManager.saveDatabaseSnapshot(name);
                }
            };
            task.setOnSucceeded(e -> {
                loadDatabaseSnapshotsAsync();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Snapshot saved successfully to database!");
                alert.showAndWait();
            });
            task.setOnFailed(e -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save snapshot: " + task.getException().getMessage());
                alert.showAndWait();
            });
            new Thread(task, "save-db-snapshot-task").start();
        });
    }

    private void handleRestoreDbSnapshot() {
        DbSnapshot selected = snapshotTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a snapshot row from the table first.");
            alert.showAndWait();
            return;
        }

        Task<PersistenceCoordinator.RestoreResult> task = new Task<>() {
            @Override
            protected PersistenceCoordinator.RestoreResult call() throws Exception {
                Optional<DbSnapshot> full = cacheManager.getPersistenceCoordinator().getSnapshotRepository().findById(selected.getId());
                if (full.isEmpty()) throw new IllegalStateException("Snapshot not found in database");
                return cacheManager.getPersistenceCoordinator().restoreSnapshot(
                        cacheManager.getCache(), full.get(), cacheManager.getClock()
                );
            }
        };

        task.setOnSucceeded(e -> {
            var res = task.getValue();
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    String.format("Snapshot restored!\n- Restored: %d entries\n- Expired Skipped: %d\n- Excess Dropped: %d",
                            res.entriesRestored(), res.expiredSkipped(), res.excessDropped()));
            alert.showAndWait();
        });

        task.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Restore failed: " + task.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(task, "restore-db-snapshot-task").start();
    }

    private void handleDeleteDbSnapshot() {
        DbSnapshot selected = snapshotTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getId() != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    cacheManager.getPersistenceCoordinator().getSnapshotRepository().deleteById(selected.getId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> loadDatabaseSnapshotsAsync());
            new Thread(task, "delete-db-snapshot-task").start();
        }
    }

    private void handleSaveBinaryFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save .jcs Snapshot File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Cache Snapshot (*.jcs)", "*.jcs"));
        chooser.setInitialFileName("cache-snapshot.jcs");
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    cacheManager.saveBinarySnapshot(file.toPath());
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Saved binary snapshot file: " + file.getAbsolutePath());
                alert.showAndWait();
            });
            task.setOnFailed(e -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Save failed: " + task.getException().getMessage());
                alert.showAndWait();
            });
            new Thread(task, "save-jcs-task").start();
        }
    }

    private void handleLoadBinaryFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load .jcs Snapshot File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Cache Snapshot (*.jcs)", "*.jcs"));
        File file = chooser.showOpenDialog(null);

        if (file != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    cacheManager.loadBinarySnapshot(file.toPath());
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Loaded binary snapshot file successfully!");
                alert.showAndWait();
            });
            task.setOnFailed(e -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Load failed: " + task.getException().getMessage());
                alert.showAndWait();
            });
            new Thread(task, "load-jcs-task").start();
        }
    }

    private void handleExportEntriesCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Cache Entries to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("cache-entries.csv");
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                List<CacheEntrySnapshot<String, String>> entries = cacheManager.getCache().getAllEntries();
                StatisticsExporter.exportEntriesCsv(entries, fw);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported " + entries.size() + " entries to " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void handleExportHistoryCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Operation History to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("cache-history.csv");
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                cacheManager.getHistory().exportCsv(fw);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported operation history to " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void handleExportStatsCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Cache Statistics to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("cache-statistics.csv");
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                var stats = cacheManager.getStatistics().snapshot(cacheManager.getCache().size(), cacheManager.getCache().getCapacity());
                StatisticsExporter.exportStatisticsCsv(stats, fw);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported statistics to " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
