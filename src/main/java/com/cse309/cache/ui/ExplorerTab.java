package com.cse309.cache.ui;

import com.cse309.cache.app.CacheManager;
import com.cse309.cache.core.CacheEntrySnapshot;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tab 3: Cache Explorer featuring TableView and prefix TreeView (Unit 5).
 * Only refreshes if visible and modificationCount has incremented.
 */
public final class ExplorerTab extends Tab {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final CacheManager cacheManager;

    private final TableView<CacheEntrySnapshot<String, String>> tableView = new TableView<>();
    private final ObservableList<CacheEntrySnapshot<String, String>> tableData = FXCollections.observableArrayList();

    private final TreeView<String> prefixTreeView = new TreeView<>();
    private final TreeItem<String> rootNode = new TreeItem<>("All Prefixes");

    private final TextField filterField = new TextField();
    private final Label rowCountLabel = new Label("Entries: 0");

    private long lastSeenModificationCount = -1;
    private String selectedPrefixFilter = null;

    public ExplorerTab(CacheManager cacheManager) {
        super("Explorer");
        this.cacheManager = cacheManager;
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top Toolbar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        filterField.setPromptText("Filter by key contains...");
        filterField.setPrefWidth(240);
        filterField.textProperty().addListener((obs, oldV, newV) -> filterTable());

        Button refreshBtn = new Button("Refresh");
        Button inspectBtn = new Button("Inspect Entry");
        Button removeBtn = new Button("Remove Selected");

        refreshBtn.setOnAction(e -> forceRefresh());
        inspectBtn.setOnAction(e -> handleInspect());
        removeBtn.setOnAction(e -> handleRemoveSelected());

        topBar.getChildren().addAll(new Label("Filter:"), filterField, refreshBtn, inspectBtn, removeBtn, rowCountLabel);
        root.setTop(topBar);

        // Center: SplitPane with TreeView on left and TableView on right
        SplitPane splitPane = new SplitPane();

        // Left: Prefix TreeView (Unit 5)
        VBox treeBox = new VBox(6);
        Label treeTitle = new Label("Namespace Hierarchy (prefix before ':')");
        treeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        rootNode.setExpanded(true);
        prefixTreeView.setRoot(rootNode);
        prefixTreeView.setShowRoot(true);
        prefixTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != rootNode) {
                selectedPrefixFilter = newVal.getValue();
            } else {
                selectedPrefixFilter = null;
            }
            filterTable();
        });

        treeBox.getChildren().addAll(treeTitle, prefixTreeView);
        VBox.setVgrow(prefixTreeView, Priority.ALWAYS);

        // Right: TableView (Unit 5)
        setupTableView();
        splitPane.getItems().addAll(treeBox, tableView);
        splitPane.setDividerPositions(0.25);

        root.setCenter(splitPane);
        setContent(root);
    }

    private void setupTableView() {
        TableColumn<CacheEntrySnapshot<String, String>, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKey()));
        keyCol.setPrefWidth(160);

        TableColumn<CacheEntrySnapshot<String, String>, String> valCol = new TableColumn<>("Value Preview");
        valCol.setCellValueFactory(d -> {
            String v = d.getValue().getValue();
            return new SimpleStringProperty(v.length() > 40 ? v.substring(0, 37) + "..." : v);
        });
        valCol.setPrefWidth(180);

        TableColumn<CacheEntrySnapshot<String, String>, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(d -> new SimpleStringProperty(TIME_FORMATTER.format(Instant.ofEpochMilli(d.getValue().getCreatedAt()))));
        createdCol.setPrefWidth(95);

        TableColumn<CacheEntrySnapshot<String, String>, String> accessCol = new TableColumn<>("Last Access");
        accessCol.setCellValueFactory(d -> new SimpleStringProperty(TIME_FORMATTER.format(Instant.ofEpochMilli(d.getValue().getLastAccessAt()))));
        accessCol.setPrefWidth(95);

        TableColumn<CacheEntrySnapshot<String, String>, String> expiresCol = new TableColumn<>("Expires");
        expiresCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().hasExpiry() ? TIME_FORMATTER.format(Instant.ofEpochMilli(d.getValue().getExpiresAt())) : "Never"
        ));
        expiresCol.setPrefWidth(95);

        TableColumn<CacheEntrySnapshot<String, String>, String> ttlCol = new TableColumn<>("Remaining TTL");
        ttlCol.setCellValueFactory(d -> {
            Optional<Duration> rem = d.getValue().remainingTtl(cacheManager.getClock());
            return new SimpleStringProperty(rem.map(duration -> duration.toSeconds() + "s").orElse("Persistent"));
        });
        ttlCol.setPrefWidth(110);

        TableColumn<CacheEntrySnapshot<String, String>, Number> freqCol = new TableColumn<>("Freq");
        freqCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getAccessCount()));
        freqCol.setPrefWidth(65);

        tableView.getColumns().addAll(keyCol, valCol, createdCol, accessCol, expiresCol, ttlCol, freqCol);
        tableView.setItems(tableData);
    }

    public void checkAndRefresh() {
        if (!isSelected()) return;
        long currentMod = cacheManager.getCache().getModificationCount();
        if (currentMod != lastSeenModificationCount) {
            forceRefresh();
            lastSeenModificationCount = currentMod;
        } else {
            tableView.refresh(); // Refresh dynamic Remaining TTL column
        }
    }

    public void forceRefresh() {
        List<CacheEntrySnapshot<String, String>> all = cacheManager.getCache().getAllEntries();
        updateTreeView(all);
        filterTable();
    }

    private void updateTreeView(List<CacheEntrySnapshot<String, String>> entries) {
        rootNode.getChildren().clear();
        Map<String, Integer> counts = new HashMap<>();

        for (CacheEntrySnapshot<String, String> entry : entries) {
            String key = entry.getKey();
            int colonIdx = key.indexOf(':');
            String prefix = colonIdx > 0 ? key.substring(0, colonIdx) : "default";
            counts.put(prefix, counts.getOrDefault(prefix, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            TreeItem<String> item = new TreeItem<>(entry.getKey() + " (" + entry.getValue() + ")");
            rootNode.getChildren().add(item);
        }
        rootNode.setValue("All (" + entries.size() + ")");
    }

    private void filterTable() {
        String search = filterField.getText().trim().toLowerCase();
        List<CacheEntrySnapshot<String, String>> all = cacheManager.getCache().getAllEntries();

        List<CacheEntrySnapshot<String, String>> filtered = all.stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    if (selectedPrefixFilter != null) {
                        String prefix = selectedPrefixFilter.contains(" ") ? selectedPrefixFilter.substring(0, selectedPrefixFilter.indexOf(' ')) : selectedPrefixFilter;
                        if (!"default".equals(prefix) && !key.startsWith(prefix + ":")) return false;
                        if ("default".equals(prefix) && key.contains(":")) return false;
                    }
                    if (!search.isEmpty() && !key.toLowerCase().contains(search)) {
                        return false;
                    }
                    return true;
                })
                .limit(5000)
                .toList();

        tableData.setAll(filtered);
        rowCountLabel.setText(String.format("Entries: %,d%s",
                tableData.size(), all.size() > 5000 ? " (truncated to 5,000)" : ""));
    }

    private void handleInspect() {
        CacheEntrySnapshot<String, String> selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please select an entry from the table first.");
            alert.showAndWait();
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Inspect Entry: " + selected.getKey());
        dialog.setHeaderText("Detailed Entry Metadata");

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));

        TextArea fullVal = new TextArea(selected.getValue());
        fullVal.setEditable(false);
        fullVal.setWrapText(true);
        fullVal.setPrefRowCount(8);

        Label meta = new Label(String.format(
                "Created: %s\nLast Access: %s\nExpires: %s\nAccess Frequency: %,d\nInsertion Seq: %,d",
                selected.getCreatedInstant(), selected.getLastAccessInstant(),
                selected.getExpiresInstant().map(Instant::toString).orElse("Never"),
                selected.getAccessCount(), selected.getInsertionSequence()
        ));

        content.getChildren().addAll(new Label("Full Value:"), fullVal, meta);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleRemoveSelected() {
        CacheEntrySnapshot<String, String> selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cacheManager.getCache().remove(selected.getKey());
            forceRefresh();
        }
    }
}
