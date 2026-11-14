package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.benchmark.BenchmarkResult;
import com.cse309.cache.benchmark.BenchmarkRunner;
import com.cse309.cache.simulation.KeyDistribution;
import com.cse309.cache.simulation.OperationMix;
import com.cse309.cache.stats.LatencyHistogram;
import com.cse309.cache.stats.StatisticsExporter;
import com.cse309.cache.stats.StatisticsSnapshot;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import com.cse309.cache.logging.AppLogging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tab 6: Comprehensive Statistics, Latency Histograms, and Automated Benchmarking (Unit 5).
 */
public final class StatisticsTab extends Tab {

    private static final Logger LOGGER = AppLogging.getLogger(StatisticsTab.class);

    private final CacheManager cacheManager;

    // Latency Row Model
    public record LatencyRow(String operation, long count, double avgMicros, double maxMicros) {}
    private final TableView<LatencyRow> latencyTable = new TableView<>();
    private final ObservableList<LatencyRow> latencyRows = FXCollections.observableArrayList();

    // Latency Histogram BarChart (Unit 5)
    private final XYChart.Series<String, Number> histSeries = new XYChart.Series<>();

    // Benchmark Table & Chart
    private final TableView<BenchmarkResult> benchmarkTable = new TableView<>();
    private final ObservableList<BenchmarkResult> benchmarkData = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> benchmarkSeries = new XYChart.Series<>();

    private final Button resetBtn = new Button("Reset Statistics");
    private final Button exportCsvBtn = new Button("Export CSV");
    private final Button runBenchmarkBtn = new Button("Run Benchmark Comparison");

    public StatisticsTab(CacheManager cacheManager) {
        super("Statistics & Benchmark");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // Top Toolbar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        resetBtn.setStyle("-fx-base: #ffc107; -fx-font-weight: bold;");
        exportCsvBtn.setStyle("-fx-base: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold;");
        runBenchmarkBtn.setStyle("-fx-base: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");

        resetBtn.setOnAction(e -> handleReset());
        exportCsvBtn.setOnAction(e -> handleExportCsv());
        runBenchmarkBtn.setOnAction(e -> handleRunBenchmark());

        topBar.getChildren().addAll(resetBtn, exportCsvBtn, runBenchmarkBtn);
        root.setTop(topBar);

        // Center: Latency Analysis (Left) & Benchmark Suite (Right)
        HBox centerSplit = new HBox(12);

        VBox latencyBox = createLatencyBox();
        VBox benchmarkBox = createBenchmarkBox();

        HBox.setHgrow(latencyBox, Priority.ALWAYS);
        HBox.setHgrow(benchmarkBox, Priority.ALWAYS);
        centerSplit.getChildren().addAll(latencyBox, benchmarkBox);

        root.setCenter(centerSplit);
        setContent(root);
    }

    private VBox createLatencyBox() {
        VBox box = new VBox(8);
        Label title = new Label("Operation Latency Analysis");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // Latency Table
        TableColumn<LatencyRow, String> opCol = new TableColumn<>("Operation");
        opCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().operation()));
        opCol.setPrefWidth(90);

        TableColumn<LatencyRow, Number> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().count()));
        countCol.setPrefWidth(85);

        TableColumn<LatencyRow, Number> avgCol = new TableColumn<>("Avg (µs)");
        avgCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().avgMicros()));
        avgCol.setPrefWidth(85);

        TableColumn<LatencyRow, Number> maxCol = new TableColumn<>("Max (µs)");
        maxCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().maxMicros()));
        maxCol.setPrefWidth(85);

        latencyTable.getColumns().addAll(opCol, countCol, avgCol, maxCol);
        latencyTable.setItems(latencyRows);
        latencyTable.setPrefHeight(160);

        // Latency Histogram BarChart (Unit 5)
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Latency Bucket");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Operations Count");

        BarChart<String, Number> histChart = new BarChart<>(xAxis, yAxis);
        histChart.setTitle("Latency Distribution");
        histChart.setLegendVisible(false);
        histChart.setAnimated(false);
        histSeries.setName("Latency Buckets");
        histChart.getData().add(histSeries);

        box.getChildren().addAll(title, latencyTable, histChart);
        VBox.setVgrow(histChart, Priority.ALWAYS);
        return box;
    }

    private VBox createBenchmarkBox() {
        VBox box = new VBox(8);
        Label title = new Label("In-Memory Benchmark Suite (Isolated Cache Runs)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // Benchmark Results Table
        TableColumn<BenchmarkResult, String> polCol = new TableColumn<>("Policy");
        polCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvictionPolicy()));
        polCol.setPrefWidth(65);

        TableColumn<BenchmarkResult, Number> thCol = new TableColumn<>("Threads");
        thCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getThreads()));
        thCol.setPrefWidth(60);

        TableColumn<BenchmarkResult, Number> opsCol = new TableColumn<>("Ops/sec");
        opsCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getOpsPerSecond()));
        opsCol.setPrefWidth(100);

        TableColumn<BenchmarkResult, Number> hitCol = new TableColumn<>("Hit %");
        hitCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getHitRatio()));
        hitCol.setPrefWidth(70);

        TableColumn<BenchmarkResult, Number> durCol = new TableColumn<>("Duration ms");
        durCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getDurationNanos() / 1_000_000.0));
        durCol.setPrefWidth(90);

        benchmarkTable.getColumns().addAll(polCol, thCol, opsCol, hitCol, durCol);
        benchmarkTable.setItems(benchmarkData);
        benchmarkTable.setPrefHeight(160);

        // Benchmark BarChart
        CategoryAxis bXAxis = new CategoryAxis();
        bXAxis.setLabel("Policy Comparison");
        NumberAxis bYAxis = new NumberAxis();
        bYAxis.setLabel("Operations / Second");

        BarChart<String, Number> bChart = new BarChart<>(bXAxis, bYAxis);
        bChart.setTitle("Comparative Throughput");
        bChart.setLegendVisible(false);
        bChart.setAnimated(false);
        benchmarkSeries.setName("Benchmark Results");
        bChart.getData().add(benchmarkSeries);

        box.getChildren().addAll(title, benchmarkTable, bChart);
        VBox.setVgrow(bChart, Priority.ALWAYS);
        return box;
    }

    public void update(StatisticsSnapshot stats) {
        if (stats == null) return;

        // Reset button disabled while simulation runs
        resetBtn.setDisable(cacheManager.getSimulationController().isActive());

        // Update Latency Rows
        long[] getLat = stats.getGetLatency();
        long[] putLat = stats.getPutLatency();
        long[] updLat = stats.getUpdateLatency();
        long[] remLat = stats.getRemoveLatency();
        long[] seaLat = stats.getSearchLatency();

        latencyRows.setAll(List.of(
                new LatencyRow("GET", getLat[0], stats.getAverageLatencyMicros(getLat), stats.getMaxLatencyMicros(getLat)),
                new LatencyRow("PUT", putLat[0], stats.getAverageLatencyMicros(putLat), stats.getMaxLatencyMicros(putLat)),
                new LatencyRow("UPDATE", updLat[0], stats.getAverageLatencyMicros(updLat), stats.getMaxLatencyMicros(updLat)),
                new LatencyRow("REMOVE", remLat[0], stats.getAverageLatencyMicros(remLat), stats.getMaxLatencyMicros(remLat)),
                new LatencyRow("SEARCH", seaLat[0], stats.getAverageLatencyMicros(seaLat), stats.getMaxLatencyMicros(seaLat))
        ));

        // Update Latency Histogram BarChart (Unit 5)
        histSeries.getData().clear();
        long[] counts = stats.getHistogramCounts();
        for (int i = 0; i < counts.length && i < LatencyHistogram.BUCKET_LABELS.length; i++) {
            histSeries.getData().add(new XYChart.Data<>(LatencyHistogram.BUCKET_LABELS[i], counts[i]));
        }
    }

    private void handleReset() {
        cacheManager.getStatistics().reset();
    }

    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Statistics CSV Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("cache-statistics.csv");
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                var stats = cacheManager.getStatistics().snapshot(cacheManager.getCache().size(), cacheManager.getCache().getCapacity());
                StatisticsExporter.exportStatisticsCsv(stats, fw);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported statistics report to: " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export CSV: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void handleRunBenchmark() {
        runBenchmarkBtn.setDisable(true);
        runBenchmarkBtn.setText("Running Benchmark...");

        // Execute in JavaFX Task (Responsiveness Rule 1)
        Task<List<BenchmarkResult>> task = new Task<>() {
            @Override
            protected List<BenchmarkResult> call() {
                return BenchmarkRunner.runPolicyComparison(
                        100, 2000, 200, KeyDistribution.HOTSPOT_80_20, OperationMix.MIXED, System.currentTimeMillis()
                );
            }
        };

        task.setOnSucceeded(e -> {
            List<BenchmarkResult> results = task.getValue();
            benchmarkData.setAll(results);
            benchmarkSeries.getData().clear();
            for (BenchmarkResult r : results) {
                benchmarkSeries.getData().add(new XYChart.Data<>(r.getEvictionPolicy(), r.getOpsPerSecond()));
                // Save to SQLite if persistent mode
                if (cacheManager.getConfiguration().getPersistenceMode() == com.cse309.cache.persistence.PersistenceMode.PERSISTENT) {
                    try {
                        cacheManager.getPersistenceCoordinator().getBenchmarkRepository().save(r);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Failed to persist benchmark result: " + ex.getMessage(), ex);
                    }
                }
            }
            runBenchmarkBtn.setDisable(false);
            runBenchmarkBtn.setText("Run Benchmark Comparison");
        });

        task.setOnFailed(e -> {
            runBenchmarkBtn.setDisable(false);
            runBenchmarkBtn.setText("Run Benchmark Comparison");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Benchmark failed: " + task.getException().getMessage());
            alert.showAndWait();
        });

        new Thread(task, "benchmark-runner-task").start();
    }
}
