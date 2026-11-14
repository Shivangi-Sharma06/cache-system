package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.history.OperationRecord;
import com.cse309.cache.logging.AppLogging;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;

/**
 * Tab 8: Historical operations audit trail and live logging console (Unit 3, Unit 4).
 */
public final class HistoryTab extends Tab {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final CacheManager cacheManager;

    private final TableView<OperationRecord> historyTable = new TableView<>();
    private final ObservableList<OperationRecord> historyData = FXCollections.observableArrayList();
    private final ComboBox<String> filterCombo = new ComboBox<>();

    private final ListView<String> liveLogView = new ListView<>();
    private final ComboBox<String> logLevelCombo = new ComboBox<>();

    public HistoryTab(CacheManager cacheManager) {
        super("History & Logs");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // Top Toolbar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        filterCombo.getItems().addAll("ALL", "GET", "PUT", "UPDATE", "DELETE", "SEARCH", "EVICT", "EXPIRE", "CLEAR");
        filterCombo.setValue("ALL");
        filterCombo.setOnAction(e -> refreshHistoryTable());

        Button refreshBtn = new Button("Refresh");
        Button exportCsvBtn = new Button("Export History CSV");
        Button clearHistoryBtn = new Button("Clear History");

        refreshBtn.setOnAction(e -> refreshHistoryTable());
        exportCsvBtn.setOnAction(e -> handleExportCsv());
        clearHistoryBtn.setOnAction(e -> {
            cacheManager.getHistory().clear();
            refreshHistoryTable();
        });

        // Log Level Selector
        logLevelCombo.getItems().addAll("ALL", "FINE", "INFO", "WARNING", "SEVERE");
        logLevelCombo.setValue(AppLogging.getLevel().getName());
        logLevelCombo.setOnAction(e -> {
            Level level = Level.parse(logLevelCombo.getValue());
            AppLogging.setLevel(level);
        });

        Button clearLogsBtn = new Button("Clear Logs");
        clearLogsBtn.setOnAction(e -> {
            AppLogging.getInMemoryLogHandler().clear();
            liveLogView.getItems().clear();
        });

        topBar.getChildren().addAll(
                new Label("Filter Op:"), filterCombo, refreshBtn, exportCsvBtn, clearHistoryBtn,
                new Label("  Log Level:"), logLevelCombo, clearLogsBtn
        );
        root.setTop(topBar);

        // Center split: History Table on Left, Live Logs on Right
        HBox centerSplit = new HBox(12);

        VBox historyBox = createHistoryBox();
        VBox logsBox = createLogsBox();

        HBox.setHgrow(historyBox, Priority.ALWAYS);
        HBox.setHgrow(logsBox, Priority.ALWAYS);
        centerSplit.getChildren().addAll(historyBox, logsBox);

        root.setCenter(centerSplit);
        setContent(root);

        // Connect to in-memory log handler
        AppLogging.getInMemoryLogHandler().addListener(line -> {
            javafx.application.Platform.runLater(() -> {
                liveLogView.getItems().add(0, line);
                while (liveLogView.getItems().size() > 500) {
                    liveLogView.getItems().remove(liveLogView.getItems().size() - 1);
                }
            });
        });

        // Pre-fill existing logs
        for (String line : AppLogging.getInMemoryLogHandler().getRecentLogs()) {
            liveLogView.getItems().add(0, line);
        }
    }

    private VBox createHistoryBox() {
        VBox box = new VBox(6);
        Label title = new Label("Operation History Log (Bounded In-Memory + SQLite Audit)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        TableColumn<OperationRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(TIME_FORMATTER.format(Instant.ofEpochMilli(d.getValue().getTimestamp()))));
        timeCol.setPrefWidth(90);

        TableColumn<OperationRecord, String> threadCol = new TableColumn<>("Thread");
        threadCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getThreadName()));
        threadCol.setPrefWidth(100);

        TableColumn<OperationRecord, String> opCol = new TableColumn<>("Op");
        opCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOperation()));
        opCol.setPrefWidth(70);

        TableColumn<OperationRecord, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCacheKey()));
        keyCol.setPrefWidth(100);

        TableColumn<OperationRecord, String> outcomeCol = new TableColumn<>("Outcome");
        outcomeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOutcome()));
        outcomeCol.setPrefWidth(90);

        TableColumn<OperationRecord, Number> latCol = new TableColumn<>("Latency (µs)");
        latCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getLatencyNanos() / 1000));
        latCol.setPrefWidth(85);

        TableColumn<OperationRecord, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDetails()));
        detailsCol.setPrefWidth(120);

        historyTable.getColumns().addAll(timeCol, threadCol, opCol, keyCol, outcomeCol, latCol, detailsCol);
        historyTable.setItems(historyData);

        box.getChildren().addAll(title, historyTable);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        return box;
    }

    private VBox createLogsBox() {
        VBox box = new VBox(6);
        Label title = new Label("Application Logs (java.util.logging Stream)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        liveLogView.setStyle("-fx-font-family: monospace; -fx-font-size: 10px;");
        box.getChildren().addAll(title, liveLogView);
        VBox.setVgrow(liveLogView, Priority.ALWAYS);
        return box;
    }

    public void refreshHistoryTable() {
        String filter = filterCombo.getValue();
        List<OperationRecord> records = cacheManager.getHistory().getFilteredRecords(filter);
        historyData.setAll(records);
    }

    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Operation History CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("operation-history.csv");
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                cacheManager.getHistory().exportCsv(fw);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported operation history to: " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
