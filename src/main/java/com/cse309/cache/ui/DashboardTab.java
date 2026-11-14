package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.eviction.EvictionRecord;
import com.cse309.cache.simulation.SimulationProgress;
import com.cse309.cache.stats.StatisticsSnapshot;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;

/**
 * Tab 1: Dashboard providing real-time metrics cards, hit-ratio trend chart,
 * eviction log, and visual transforms/effects (DropShadow, Glow, Rotate, Scale) (Unit 5).
 */
public final class DashboardTab extends Tab {

    private final CacheManager cacheManager;

    // Metric Labels
    private final Label capacityVal = new Label("0");
    private final Label sizeVal = new Label("0");
    private final Label hitRatioVal = new Label("0.0%");
    private final Label missRatioVal = new Label("0.0%");
    private final Label totalOpsVal = new Label("0");
    private final Label evictionsVal = new Label("0");
    private final Label expirationsVal = new Label("0");
    private final Label activePolicyVal = new Label("LRU");
    private final Label persistenceModeVal = new Label("IN_MEMORY_ONLY");
    private final Label activeThreadsVal = new Label("0");

    private final ProgressBar utilizationBar = new ProgressBar(0.0);
    private final Label utilizationLabel = new Label("0.0%");

    // Visual Transforms & Effects (Unit 5)
    private final Glow glowEffect = new Glow(0.8);
    private final Circle spinnerCircle = new Circle(8, Color.DODGERBLUE);
    private final RotateTransition rotateTransition;
    private final ScaleTransition scaleTransition;

    // Charts & Lists
    private final XYChart.Series<Number, Number> hitRatioSeries = new XYChart.Series<>();
    private final ListView<String> evictionListView = new ListView<>();
    private int sampleIndex = 0;
    private long lastEvictionsCount = 0;

    public DashboardTab(CacheManager cacheManager) {
        super("Dashboard");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // Setup Rotate transform on spinner (Unit 5)
        rotateTransition = new RotateTransition(Duration.millis(1000), spinnerCircle);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);

        // Setup Scale transform on eviction label (Unit 5)
        scaleTransition = new ScaleTransition(Duration.millis(250), evictionsVal);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.3);
        scaleTransition.setToY(1.3);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);

        // Top: Metric Cards Grid
        GridPane cardsGrid = createCardsGrid();
        root.setTop(cardsGrid);

        // Center split: Hit Ratio Chart + Eviction Log
        HBox centerBox = new HBox(12);
        centerBox.setPadding(new Insets(12, 0, 0, 0));

        VBox chartBox = createChartBox();
        VBox evictionBox = createEvictionBox();

        HBox.setHgrow(chartBox, Priority.ALWAYS);
        HBox.setHgrow(evictionBox, Priority.ALWAYS);
        centerBox.getChildren().addAll(chartBox, evictionBox);
        root.setCenter(centerBox);

        setContent(root);
    }

    private GridPane createCardsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        grid.add(createCard("Capacity", capacityVal, Color.STEELBLUE), 0, 0);
        grid.add(createCard("Live Size", sizeVal, Color.SEAGREEN), 1, 0);

        VBox utilCard = createCard("Utilization", utilizationLabel, Color.TEAL);
        utilizationBar.setMaxWidth(Double.MAX_VALUE);
        utilCard.getChildren().add(utilizationBar);
        grid.add(utilCard, 2, 0);

        grid.add(createCard("Hit Ratio", hitRatioVal, Color.FORESTGREEN), 3, 0);
        grid.add(createCard("Miss Ratio", missRatioVal, Color.INDIANRED), 4, 0);

        grid.add(createCard("Total Operations", totalOpsVal, Color.DARKORANGE), 0, 1);
        grid.add(createCard("Evictions", evictionsVal, Color.CRIMSON), 1, 1);
        grid.add(createCard("Expirations", expirationsVal, Color.DARKGOLDENROD), 2, 1);
        grid.add(createCard("Active Policy", activePolicyVal, Color.DARKSLATEBLUE), 3, 1);

        VBox threadsCard = createCard("Simulation Status", activeThreadsVal, Color.PURPLE);
        HBox spinnerBox = new HBox(6, new Label("Engine:"), spinnerCircle);
        spinnerBox.setAlignment(Pos.CENTER);
        threadsCard.getChildren().add(spinnerBox);
        grid.add(threadsCard, 4, 1);

        return grid;
    }

    private VBox createCard(String title, Label valueLabel, Color accent) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(170);
        card.setStyle(String.format("-fx-background-color: white; -fx-background-radius: 6; " +
                "-fx-border-color: %s; -fx-border-radius: 6; -fx-border-width: 0 0 3 0;", toHex(accent)));

        // DropShadow effect on cards (Unit 5)
        DropShadow shadow = new DropShadow(4, 2, 2, Color.rgb(0, 0, 0, 0.15));
        card.setEffect(shadow);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-weight: bold;");

        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #222;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createChartBox() {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-padding: 8;");
        box.setEffect(new DropShadow(4, 2, 2, Color.rgb(0, 0, 0, 0.1)));

        Label title = new Label("Hit Ratio Trend (%)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Sample Tick");
        xAxis.setAutoRanging(true);

        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("Hit %");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Real-time Hit Ratio");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        hitRatioSeries.setName("Hit %");
        chart.getData().add(hitRatioSeries);

        box.getChildren().addAll(title, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return box;
    }

    private VBox createEvictionBox() {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-padding: 8;");
        box.setEffect(new DropShadow(4, 2, 2, Color.rgb(0, 0, 0, 0.1)));

        Label title = new Label("Recent Evictions Log");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        evictionListView.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        box.getChildren().addAll(title, evictionListView);
        VBox.setVgrow(evictionListView, Priority.ALWAYS);
        return box;
    }

    public void update(StatisticsSnapshot stats, SimulationProgress simProgress, List<EvictionRecord> newEvictions) {
        if (stats == null) return;

        capacityVal.setText(String.format("%,d", stats.getCurrentCapacity()));
        sizeVal.setText(String.format("%,d", stats.getCurrentSize()));

        double hitRatio = stats.getHitRatio();
        hitRatioVal.setText(String.format("%.1f%%", hitRatio));
        missRatioVal.setText(String.format("%.1f%%", stats.getMissRatio()));

        double util = stats.getUtilization() / 100.0;
        utilizationBar.setProgress(util);
        utilizationLabel.setText(String.format("%.1f%%", stats.getUtilization()));

        totalOpsVal.setText(String.format("%,d", stats.getTotalRequests()));

        long evictions = stats.getEvictions();
        if (evictions > lastEvictionsCount) {
            evictionsVal.setEffect(glowEffect); // Glow effect on counter update (Unit 5)
            scaleTransition.playFromStart();    // Scale transform pulse (Unit 5)
            lastEvictionsCount = evictions;
        } else {
            evictionsVal.setEffect(null);
        }
        evictionsVal.setText(String.format("%,d", evictions));

        expirationsVal.setText(String.format("%,d", stats.getExpirations()));
        activePolicyVal.setText(cacheManager.getCache().getEvictionPolicyType().name());

        // Update simulation indicator
        if (simProgress != null && (simProgress.getState() == SimulationProgress.State.RUNNING)) {
            if (rotateTransition.getStatus() != Animation.Status.RUNNING) {
                rotateTransition.play(); // Rotate transform while simulation runs (Unit 5)
            }
            activeThreadsVal.setText(String.format("RUNNING (%d workers)", simProgress.getRunningWorkers()));
        } else {
            rotateTransition.pause();
            activeThreadsVal.setText(simProgress != null ? simProgress.getState().name() : "IDLE");
        }

        // Update LineChart bounded to 120 samples
        sampleIndex++;
        hitRatioSeries.getData().add(new XYChart.Data<>(sampleIndex, hitRatio));
        if (hitRatioSeries.getData().size() > 120) {
            hitRatioSeries.getData().remove(0);
        }

        // Update eviction list
        if (newEvictions != null && !newEvictions.isEmpty()) {
            for (EvictionRecord r : newEvictions) {
                evictionListView.getItems().add(0, String.format("[%s] Key '%s' (%s)",
                        r.getReason(), r.getKey(), r.getExplanation()));
            }
            while (evictionListView.getItems().size() > 100) {
                evictionListView.getItems().remove(evictionListView.getItems().size() - 1);
            }
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
