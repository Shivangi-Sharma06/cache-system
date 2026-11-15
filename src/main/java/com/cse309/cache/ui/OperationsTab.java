package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.core.CacheLookupResult;
import com.cse309.cache.core.CacheWriteResult;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Duration;

/**
 * Tab 2: Interactive Cache CRUD operations form with latency measurements,
 * result audit panel, and last 50 results ListView (Unit 5).
 */
public final class OperationsTab extends Tab {

    private final CacheManager cacheManager;

    private final TextField keyField = new TextField();
    private final TextArea valueArea = new TextArea();
    private final TextField ttlField = new TextField();
    private final ComboBox<String> ttlUnitCombo = new ComboBox<>();
    private final Label validationLabel = new Label();

    private final Label resultStatusLabel = new Label("IDLE");
    private final Label resultLatencyLabel = new Label("0 µs");
    private final Label resultDetailsLabel = new Label("-");

    private final ListView<String> recentResultsList = new ListView<>();

    public OperationsTab(CacheManager cacheManager) {
        super("Operations");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(14));

        // Form Layout
        VBox leftForm = new VBox(10);
        leftForm.setMinWidth(480);
        leftForm.setMaxWidth(550);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        keyField.setPromptText("e.g. user:1001");
        grid.add(new Label("Key:"), 0, 0);
        grid.add(keyField, 1, 0);

        valueArea.setPromptText("Enter value payload...");
        valueArea.setPrefRowCount(4);
        grid.add(new Label("Value:"), 0, 1);
        grid.add(valueArea, 1, 1);

        // TTL controls
        HBox ttlBox = new HBox(6);
        ttlField.setPromptText("TTL value");
        ttlField.setPrefWidth(120);
        ttlUnitCombo.getItems().addAll("Use default", "Seconds", "Minutes", "Hours", "Never expires");
        ttlUnitCombo.setValue("Use default");
        ttlUnitCombo.setOnAction(e -> updateTtlFieldState());
        ttlBox.getChildren().addAll(ttlField, ttlUnitCombo);

        grid.add(new Label("TTL:"), 0, 2);
        grid.add(ttlBox, 1, 2);

        validationLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        grid.add(validationLabel, 1, 3);

        // Action Buttons
        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button putBtn = new Button("Put");
        Button getBtn = new Button("Get");
        Button updateBtn = new Button("Update");
        Button deleteBtn = new Button("Delete");
        Button searchBtn = new Button("Search");
        Button clearBtn = new Button("Clear Cache");

        putBtn.setStyle("-fx-base: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        getBtn.setStyle("-fx-base: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
        updateBtn.setStyle("-fx-base: #ffc107; -fx-font-weight: bold;");
        deleteBtn.setStyle("-fx-base: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        clearBtn.setStyle("-fx-base: #6c757d; -fx-text-fill: white;");

        putBtn.setOnAction(e -> handlePut());
        getBtn.setOnAction(e -> handleGet());
        updateBtn.setOnAction(e -> handleUpdate());
        deleteBtn.setOnAction(e -> handleDelete());
        searchBtn.setOnAction(e -> handleSearch());
        clearBtn.setOnAction(e -> handleClear());

        buttonBar.getChildren().addAll(putBtn, getBtn, updateBtn, deleteBtn, searchBtn, clearBtn);

        // Result Card
        VBox resultCard = new VBox(6);
        resultCard.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ced4da; " +
                "-fx-border-radius: 4; -fx-padding: 10;");

        Label cardTitle = new Label("Operation Result");
        cardTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        resultStatusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #155724;");
        resultCard.getChildren().addAll(cardTitle, resultStatusLabel, resultLatencyLabel, resultDetailsLabel);

        HBox tuningBox = createTuningBox();
        leftForm.getChildren().addAll(tuningBox, grid, buttonBar, resultCard);

        // Right side: Recent Results ListView (Unit 5)
        VBox rightList = new VBox(6);
        Label listTitle = new Label("Recent Execution History (Last 50)");
        listTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        recentResultsList.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        rightList.getChildren().addAll(listTitle, recentResultsList);
        VBox.setVgrow(recentResultsList, Priority.ALWAYS);
        HBox.setHgrow(rightList, Priority.ALWAYS);

        HBox mainSplit = new HBox(16, leftForm, rightList);
        root.setCenter(mainSplit);

        setContent(root);
        updateTtlFieldState();
    }

    private void updateTtlFieldState() {
        String unit = ttlUnitCombo.getValue();
        ttlField.setDisable("Use default".equals(unit) || "Never expires".equals(unit));
        if (ttlField.isDisable()) {
            ttlField.setText("");
        }
    }

    private void handlePut() {
        String key = keyField.getText();
        String val = valueArea.getText();

        var keyVal = InputValidator.validateKey(key);
        if (!keyVal.isValid()) { validationLabel.setText(keyVal.errorMessage()); return; }
        var valVal = InputValidator.validateValue(val);
        if (!valVal.isValid()) { validationLabel.setText(valVal.errorMessage()); return; }
        var ttlVal = InputValidator.validateTtl(ttlField.getText(), ttlUnitCombo.getValue());
        if (!ttlVal.isValid()) { validationLabel.setText(ttlVal.errorMessage()); return; }

        validationLabel.setText("");
        long t0 = System.nanoTime();

        try {
            CacheWriteResult<String> result;
            String unit = ttlUnitCombo.getValue();
            if ("Never expires".equals(unit)) {
                result = cacheManager.getCache().putWithoutExpiry(key, val);
            } else if (!"Use default".equals(unit) && !ttlField.getText().isBlank()) {
                long num = Long.parseLong(ttlField.getText().trim());
                Duration dur = switch (unit.toLowerCase()) {
                    case "minutes" -> Duration.ofMinutes(num);
                    case "hours" -> Duration.ofHours(num);
                    default -> Duration.ofSeconds(num);
                };
                result = cacheManager.getCache().put(key, val, dur);
            } else {
                result = cacheManager.getCache().put(key, val);
            }

            long latency = (System.nanoTime() - t0) / 1000;
            String details = result.hadEvictions() ? "Evicted: " + result.getEvictions().size() + " entries" : "No evictions";
            setResult(result.getStatus().name(), latency, details, true);
        } catch (CacheException e) {
            setResult("ERROR", (System.nanoTime() - t0) / 1000, e.getUserMessage(), false);
        }
    }

    private void handleGet() {
        String key = keyField.getText();
        var keyVal = InputValidator.validateKey(key);
        if (!keyVal.isValid()) { validationLabel.setText(keyVal.errorMessage()); return; }

        validationLabel.setText("");
        long t0 = System.nanoTime();

        CacheLookupResult<String> lookup = cacheManager.getCache().get(key);
        long latency = (System.nanoTime() - t0) / 1000;

        if (lookup.isHit()) {
            valueArea.setText(lookup.getValueOrNull());
            setResult("HIT", latency, "Value fetched successfully", true);
        } else if (lookup.isExpired()) {
            valueArea.setText("");
            setResult("EXPIRED", latency, "Key existed but its TTL has expired", false);
        } else {
            valueArea.setText("");
            setResult("MISS", latency, "Key not found in cache", false);
        }
    }

    private void handleUpdate() {
        String key = keyField.getText();
        String val = valueArea.getText();
        var keyVal = InputValidator.validateKey(key);
        if (!keyVal.isValid()) { validationLabel.setText(keyVal.errorMessage()); return; }

        validationLabel.setText("");
        long t0 = System.nanoTime();

        try {
            CacheWriteResult<String> res = cacheManager.getCache().update(key, val);
            long latency = (System.nanoTime() - t0) / 1000;
            setResult("REPLACED", latency, "Updated value for existing key", true);
        } catch (CacheException e) {
            setResult("NOT_FOUND", (System.nanoTime() - t0) / 1000, e.getUserMessage(), false);
        }
    }

    private void handleDelete() {
        String key = keyField.getText();
        var keyVal = InputValidator.validateKey(key);
        if (!keyVal.isValid()) { validationLabel.setText(keyVal.errorMessage()); return; }

        validationLabel.setText("");
        long t0 = System.nanoTime();

        CacheLookupResult<String> rem = cacheManager.getCache().remove(key);
        long latency = (System.nanoTime() - t0) / 1000;

        if (rem.isHit()) {
            valueArea.setText("");
            setResult("REMOVED", latency, "Key deleted from cache", true);
        } else {
            setResult("MISS", latency, "Key was not present to remove", false);
        }
    }

    private void handleSearch() {
        String prefix = keyField.getText();
        long t0 = System.nanoTime();
        var matches = cacheManager.getCache().search(k -> prefix.isBlank() || k.contains(prefix));
        long latency = (System.nanoTime() - t0) / 1000;
        setResult("MATCHES: " + matches.size(), latency, "Found " + matches.size() + " matching live entries", true);
    }

    private void handleClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Clear all entries in the cache?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Clear");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                cacheManager.getCache().clear();
                valueArea.setText("");
                setResult("CLEARED", 0, "Cache flushed completely", true);
            }
        });
    }

    private void setResult(String status, long latencyMicros, String details, boolean success) {
        resultStatusLabel.setText(status);
        resultStatusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " +
                (success ? "#155724;" : "#721c24;"));
        resultLatencyLabel.setText(String.format("Latency: %,d µs", latencyMicros));
        resultDetailsLabel.setText(details);

        String logEntry = String.format("[%s] Key='%s' -> %s (%d µs) - %s",
                java.time.LocalTime.now().toString().substring(0, 8),
                keyField.getText(), status, latencyMicros, details);

        recentResultsList.getItems().add(0, logEntry);
        while (recentResultsList.getItems().size() > 50) {
            recentResultsList.getItems().remove(recentResultsList.getItems().size() - 1);
        }
    }

    private HBox createTuningBox() {
        HBox tuningBox = new HBox(10);
        tuningBox.setAlignment(Pos.CENTER_LEFT);
        tuningBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; -fx-border-color: #dee2e6; -fx-border-radius: 4;");

        ComboBox<EvictionPolicyType> policyCombo = new ComboBox<>();
        policyCombo.getItems().addAll(EvictionPolicyType.LRU, EvictionPolicyType.FIFO, EvictionPolicyType.LFU);
        policyCombo.setValue(cacheManager.getCache().getEvictionPolicyType());
        policyCombo.setOnAction(e -> {
            EvictionPolicyType selected = policyCombo.getValue();
            if (selected != null) {
                cacheManager.getCache().setEvictionPolicy(selected);
                setResult("POLICY_CHANGED", 0, "Switched eviction policy to " + selected, true);
            }
        });

        TextField capField = new TextField(String.valueOf(cacheManager.getCache().getCapacity()));
        capField.setPrefWidth(75);
        Button setCapBtn = new Button("Apply Capacity");
        setCapBtn.setOnAction(e -> {
            try {
                int newCap = Integer.parseInt(capField.getText().trim());
                if (newCap <= 0) return;
                int currentSize = cacheManager.getCache().size();
                if (newCap < currentSize) {
                    int toEvict = cacheManager.getCache().previewCapacityChange(newCap);
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            String.format("Reducing capacity to %d will immediately evict %d entries using %s.\nProceed?",
                                    newCap, toEvict, cacheManager.getCache().getEvictionPolicyType()),
                            ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText("Confirm Eviction");
                    var res = confirm.showAndWait();
                    if (res.isEmpty() || res.get() != ButtonType.YES) return;
                }
                cacheManager.getCache().setCapacity(newCap);
                setResult("CAPACITY_UPDATED", 0, "Capacity set to " + newCap, true);
            } catch (com.cse309.cache.exception.InvalidCacheCapacityException ex) {
                setResult("INVALID_CAPACITY", 0, ex.getUserMessage(), false);
            } catch (NumberFormatException ignored) {}
        });

        tuningBox.getChildren().addAll(
                new Label("Policy:"), policyCombo,
                new Label("Capacity:"), capField, setCapBtn
        );
        return tuningBox;
    }
}
