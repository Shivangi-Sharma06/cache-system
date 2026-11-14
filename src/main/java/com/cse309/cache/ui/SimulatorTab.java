package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.simulation.KeyDistribution;
import com.cse309.cache.simulation.OperationMix;
import com.cse309.cache.simulation.SimulationConfig;
import com.cse309.cache.simulation.SimulationProgress;
import com.cse309.cache.threading.ThreadStateMonitor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Tab 5: Concurrency Workload Simulator.
 * Demonstrates thread states, producer-consumer queues, and priority hints (Unit 3).
 */
public final class SimulatorTab extends Tab {

    private final CacheManager cacheManager;

    // Controls
    private final TextField threadCountField = new TextField("4");
    private final TextField opsCountField = new TextField("5000");
    private final ComboBox<String> mixCombo = new ComboBox<>();
    private final ComboBox<KeyDistribution> distCombo = new ComboBox<>();
    private final TextField keySpaceField = new TextField("200");
    private final TextField seedField = new TextField();
    private final CheckBox priorityCheck = new CheckBox("Priority experiment (half MIN, half MAX)");
    private final CheckBox logEveryCheck = new CheckBox("Log every operation (<= 500 ops only)");

    private final Button startBtn = new Button("Start Simulation");
    private final Button pauseResumeBtn = new Button("Pause");
    private final Button stopBtn = new Button("Stop");

    // Telemetry
    private final ProgressBar progressBar = new ProgressBar(0.0);
    private final Label progressText = new Label("0 / 0 (0%)");
    private final Label speedLabel = new Label("0.0 ops/sec");
    private final Label elapsedLabel = new Label("Elapsed: 0.0s");
    private final Label countersLabel = new Label("Succeeded: 0 | Failed: 0 | Pending: 0");

    // Lists
    private final ListView<String> threadStateListView = new ListView<>();
    private final ListView<String> simulationLogView = new ListView<>();

    public SimulatorTab(CacheManager cacheManager) {
        super("Simulator");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // Left Panel: Workload Configuration & Controls
        VBox leftBox = new VBox(10);
        leftBox.setMinWidth(380);
        leftBox.setMaxWidth(440);

        Label header = new Label("Workload Configuration");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        grid.add(new Label("Worker Threads (1 - 64):"), 0, 0);
        grid.add(threadCountField, 1, 0);

        grid.add(new Label("Total Operations:"), 0, 1);
        grid.add(opsCountField, 1, 1);

        mixCombo.getItems().addAll("Mixed (50/30/10/5/5)", "Read-Heavy (85/10/3/2/0)", "Write-Heavy (20/60/10/10/0)");
        mixCombo.setValue("Mixed (50/30/10/5/5)");
        grid.add(new Label("Operation Mix:"), 0, 2);
        grid.add(mixCombo, 1, 2);

        distCombo.getItems().addAll(KeyDistribution.values());
        distCombo.setValue(KeyDistribution.HOTSPOT_80_20);
        grid.add(new Label("Key Distribution:"), 0, 3);
        grid.add(distCombo, 1, 3);

        grid.add(new Label("Key Space Size:"), 0, 4);
        grid.add(keySpaceField, 1, 4);

        seedField.setPromptText("Optional seed for repeatability");
        grid.add(new Label("Random Seed:"), 0, 5);
        grid.add(seedField, 1, 5);

        grid.add(priorityCheck, 0, 6, 2, 1);
        grid.add(logEveryCheck, 0, 7, 2, 1);

        // Buttons
        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        startBtn.setStyle("-fx-base: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        pauseResumeBtn.setStyle("-fx-base: #ffc107; -fx-font-weight: bold;");
        stopBtn.setStyle("-fx-base: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");

        startBtn.setOnAction(e -> handleStart());
        pauseResumeBtn.setOnAction(e -> handlePauseResume());
        stopBtn.setOnAction(e -> handleStop());
        pauseResumeBtn.setDisable(true);
        stopBtn.setDisable(true);

        buttonBar.getChildren().addAll(startBtn, pauseResumeBtn, stopBtn);

        // Telemetry Card
        VBox teleCard = new VBox(6);
        teleCard.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ced4da; -fx-padding: 10; -fx-border-radius: 4;");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressText.setStyle("-fx-font-weight: bold;");
        speedLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #007bff;");

        teleCard.getChildren().addAll(new Label("Live Progress:"), progressBar, progressText, speedLabel, elapsedLabel, countersLabel);

        leftBox.getChildren().addAll(header, grid, buttonBar, teleCard);

        // Right Panel: Live Worker Thread States + Simulation Log
        VBox rightBox = new VBox(10);
        Label threadTitle = new Label("Worker Thread States (Thread.getState() - Unit 3)");
        threadTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        threadStateListView.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        threadStateListView.setPrefHeight(180);

        Label logTitle = new Label("Live Simulation Output");
        logTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        simulationLogView.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        rightBox.getChildren().addAll(threadTitle, threadStateListView, logTitle, simulationLogView);
        VBox.setVgrow(simulationLogView, Priority.ALWAYS);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        HBox split = new HBox(16, leftBox, rightBox);
        root.setCenter(split);

        setContent(root);
    }

    private void handleStart() {
        try {
            int threads = Integer.parseInt(threadCountField.getText().trim());
            int ops = Integer.parseInt(opsCountField.getText().trim());
            int keys = Integer.parseInt(keySpaceField.getText().trim());

            OperationMix mix = switch (mixCombo.getValue()) {
                case "Read-Heavy (85/10/3/2/0)" -> OperationMix.READ_HEAVY;
                case "Write-Heavy (20/60/10/10/0)" -> OperationMix.WRITE_HEAVY;
                default -> OperationMix.MIXED;
            };

            Long seed = null;
            if (!seedField.getText().isBlank()) {
                seed = Long.parseLong(seedField.getText().trim());
            }

            SimulationConfig config = new SimulationConfig(
                    threads, ops, mix, distCombo.getValue(), keys, 256,
                    seed, priorityCheck.isSelected(), logEveryCheck.isSelected()
            );

            cacheManager.getSimulationController().start(config);
            startBtn.setDisable(true);
            pauseResumeBtn.setDisable(false);
            pauseResumeBtn.setText("Pause");
            stopBtn.setDisable(false);

            simulationLogView.getItems().add(0, String.format("Simulation started with %d operations on %d workers (seed=%s)",
                    ops, threads, seed != null ? seed : "random"));

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid simulation input: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handlePauseResume() {
        var controller = cacheManager.getSimulationController();
        if (controller.getProgress().getState() == SimulationProgress.State.PAUSED) {
            controller.resume();
            pauseResumeBtn.setText("Pause");
        } else {
            controller.pause();
            pauseResumeBtn.setText("Resume");
        }
    }

    private void handleStop() {
        cacheManager.getSimulationController().stop();
        startBtn.setDisable(false);
        pauseResumeBtn.setDisable(true);
        stopBtn.setDisable(true);
    }

    public void update(SimulationProgress progress, List<ThreadStateMonitor.WorkerStatus> statuses) {
        if (progress == null) return;

        SimulationConfig activeCfg = cacheManager.getSimulationController().getActiveConfig();
        int totalOps = activeCfg != null ? activeCfg.getTotalOperations() : progress.getGenerated();

        double fraction = progress.getProgressFraction(totalOps);
        progressBar.setProgress(fraction);

        progressText.setText(String.format("%,d / %,d (%.1f%%) - State: %s",
                progress.getCompleted(), totalOps, fraction * 100.0, progress.getState()));

        speedLabel.setText(String.format("Throughput: %,.1f ops/sec", progress.getOpsPerSecond()));
        elapsedLabel.setText(String.format("Elapsed Time: %.2f seconds", progress.getElapsedSeconds()));
        countersLabel.setText(String.format("Succeeded: %,d | Failed: %,d | Pending: %,d",
                progress.getSucceeded(), progress.getFailed(), progress.getPending()));

        // Update button states if completed
        if (progress.getState() == SimulationProgress.State.COMPLETED || progress.getState() == SimulationProgress.State.CANCELLED) {
            startBtn.setDisable(false);
            pauseResumeBtn.setDisable(true);
            stopBtn.setDisable(true);
        }

        // Update Worker Thread States ListView
        if (statuses != null && !statuses.isEmpty()) {
            threadStateListView.getItems().clear();
            for (ThreadStateMonitor.WorkerStatus s : statuses) {
                threadStateListView.getItems().add(s.toString());
            }
        }
    }

    public void appendLog(String message) {
        simulationLogView.getItems().add(0, message);
        while (simulationLogView.getItems().size() > 500) {
            simulationLogView.getItems().remove(simulationLogView.getItems().size() - 1);
        }
    }
}
