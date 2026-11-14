package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.config.CacheConfiguration;
import com.cse309.cache.core.CacheDefaults;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.persistence.PersistenceMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Duration;

/**
 * Tab 4: Interactive cache configuration management.
 * Features RadioButton ToggleGroup for eviction policies and capacity reduction confirmation (Unit 5).
 */
public final class ConfigurationTab extends Tab {

    private final CacheManager cacheManager;

    private final TextField capacityField = new TextField();
    private final ToggleGroup policyGroup = new ToggleGroup();
    private final RadioButton lruRadio = new RadioButton("LRU - Least Recently Used");
    private final RadioButton fifoRadio = new RadioButton("FIFO - First In First Out");
    private final RadioButton lfuRadio = new RadioButton("LFU - Least Frequently Used");

    private final TextField defaultTtlField = new TextField();
    private final TextField cleanupIntervalField = new TextField();
    private final TextField workerThreadsField = new TextField();
    private final ComboBox<PersistenceMode> persistenceModeCombo = new ComboBox<>();
    private final TextField historyCapacityField = new TextField();

    private final Label statusMessageLabel = new Label();

    public ConfigurationTab(CacheManager cacheManager) {
        super("Configuration");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        VBox form = new VBox(14);
        form.setMaxWidth(650);

        Label header = new Label("Cache Configuration & Tuning");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        // 1. Capacity
        grid.add(new Label("Capacity (1 - 1,000,000):"), 0, 0);
        grid.add(capacityField, 1, 0);

        // 2. Policy RadioButtons in ToggleGroup (Unit 5)
        lruRadio.setToggleGroup(policyGroup);
        fifoRadio.setToggleGroup(policyGroup);
        lfuRadio.setToggleGroup(policyGroup);

        VBox policyBox = new VBox(4, lruRadio, fifoRadio, lfuRadio);
        grid.add(new Label("Eviction Policy:"), 0, 1);
        grid.add(policyBox, 1, 1);

        // 3. Default TTL
        grid.add(new Label("Default TTL (seconds, -1 for none):"), 0, 2);
        grid.add(defaultTtlField, 1, 2);

        // 4. Cleanup Interval
        grid.add(new Label("Background Expiration Interval (sec):"), 0, 3);
        grid.add(cleanupIntervalField, 1, 3);

        // 5. Worker Threads
        grid.add(new Label("Default Simulator Workers (1 - 64):"), 0, 4);
        grid.add(workerThreadsField, 1, 4);

        // 6. Persistence Mode
        persistenceModeCombo.getItems().addAll(PersistenceMode.IN_MEMORY_ONLY, PersistenceMode.PERSISTENT);
        grid.add(new Label("Persistence Mode:"), 0, 5);
        grid.add(persistenceModeCombo, 1, 5);

        // 7. History Capacity
        grid.add(new Label("Operation History Buffer (100 - 10,000):"), 0, 6);
        grid.add(historyCapacityField, 1, 6);

        // Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button applyBtn = new Button("Apply Changes");
        applyBtn.setStyle("-fx-base: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        Button resetBtn = new Button("Reset Form to Current");
        Button defaultsBtn = new Button("Reset to System Defaults");
        Button saveStartupBtn = new Button("Save as Startup Defaults");

        applyBtn.setOnAction(e -> handleApply());
        resetBtn.setOnAction(e -> populateFromConfig(cacheManager.getConfiguration()));
        defaultsBtn.setOnAction(e -> populateFromConfig(new CacheConfiguration()));
        saveStartupBtn.setOnAction(e -> handleSaveStartup());

        buttonBar.getChildren().addAll(applyBtn, resetBtn, defaultsBtn, saveStartupBtn);

        statusMessageLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        form.getChildren().addAll(header, grid, buttonBar, statusMessageLabel);
        root.setCenter(form);

        setContent(root);
        populateFromConfig(cacheManager.getConfiguration());
    }

    private void populateFromConfig(CacheConfiguration config) {
        capacityField.setText(String.valueOf(config.getCapacity()));

        switch (config.getEvictionPolicy()) {
            case LRU -> lruRadio.setSelected(true);
            case FIFO -> fifoRadio.setSelected(true);
            case LFU -> lfuRadio.setSelected(true);
        }

        defaultTtlField.setText(String.valueOf(config.getDefaultTtl().map(Duration::getSeconds).orElse(-1L)));
        cleanupIntervalField.setText(String.valueOf(config.getCleanupInterval().toSeconds()));
        workerThreadsField.setText(String.valueOf(config.getWorkerThreads()));
        persistenceModeCombo.setValue(config.getPersistenceMode());
        historyCapacityField.setText(String.valueOf(config.getHistoryCapacity()));
        statusMessageLabel.setText("");
    }

    private void handleApply() {
        try {
            int newCapacity = Integer.parseInt(capacityField.getText().trim());

            // Check capacity reduction preview (Section 7)
            int currentSize = cacheManager.getCache().size();
            if (newCapacity < currentSize) {
                int toEvict = cacheManager.getCache().previewCapacityChange(newCapacity);
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        String.format("Reducing capacity to %d will immediately evict %d entries using %s policy.\nProceed?",
                                newCapacity, toEvict, cacheManager.getCache().getEvictionPolicyType()),
                        ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Confirm Immediate Eviction");
                var response = confirm.showAndWait();
                if (response.isEmpty() || response.get() != ButtonType.YES) {
                    return;
                }
            }

            EvictionPolicyType policy = lruRadio.isSelected() ? EvictionPolicyType.LRU
                    : (fifoRadio.isSelected() ? EvictionPolicyType.FIFO : EvictionPolicyType.LFU);

            long ttlSec = Long.parseLong(defaultTtlField.getText().trim());
            Duration defaultTtl = ttlSec > 0 ? Duration.ofSeconds(ttlSec) : null;

            long intervalSec = Long.parseLong(cleanupIntervalField.getText().trim());
            Duration cleanupInterval = Duration.ofSeconds(Math.max(1, intervalSec));

            int workers = Integer.parseInt(workerThreadsField.getText().trim());
            PersistenceMode mode = persistenceModeCombo.getValue();
            int histCap = Integer.parseInt(historyCapacityField.getText().trim());

            CacheConfiguration newConfig = CacheConfiguration.builder()
                    .capacity(newCapacity)
                    .evictionPolicy(policy)
                    .defaultTtl(defaultTtl)
                    .cleanupInterval(cleanupInterval)
                    .workerThreads(workers)
                    .persistenceMode(mode)
                    .historyCapacity(histCap)
                    .build();

            cacheManager.applyConfiguration(newConfig);
            statusMessageLabel.setText("Configuration applied successfully!");
            statusMessageLabel.setStyle("-fx-text-fill: #155724; -fx-font-weight: bold;");

        } catch (Exception e) {
            statusMessageLabel.setText("Error applying configuration: " + e.getMessage());
            statusMessageLabel.setStyle("-fx-text-fill: #721c24; -fx-font-weight: bold;");
        }
    }

    private void handleSaveStartup() {
        try {
            cacheManager.saveConfigurationAsStartupDefaults();
            statusMessageLabel.setText("Configuration saved as startup defaults in data/config.properties!");
            statusMessageLabel.setStyle("-fx-text-fill: #155724; -fx-font-weight: bold;");
        } catch (Exception e) {
            statusMessageLabel.setText("Failed to save startup configuration: " + e.getMessage());
            statusMessageLabel.setStyle("-fx-text-fill: #721c24; -fx-font-weight: bold;");
        }
    }
}
