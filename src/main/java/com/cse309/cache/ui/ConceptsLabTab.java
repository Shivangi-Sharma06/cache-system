package com.cse309.cache.ui;

import com.cse309.cache.core.Cache;
import com.cse309.cache.lab.CollectionsLab;
import com.cse309.cache.lab.GenericCacheLab;
import com.cse309.cache.lab.RaceConditionLab;
import com.cse309.cache.lab.Student;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Clock;
import java.util.List;

/**
 * Tab 9: Concepts Laboratory showcasing syllabus fundamentals (Unit 3, 4, 5).
 * Explicitly labelled: "educational, not part of the cache engine".
 */
public final class ConceptsLabTab extends Tab {

    public ConceptsLabTab() {
        super("Concepts Lab");
        setClosable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        Label disclaimer = new Label("Educational Concept Demonstrations - strictly isolated from the production cache engine");
        disclaimer.setStyle("-fx-font-style: italic; -fx-text-fill: #555; -fx-padding: 0 0 8 0;");
        root.setTop(disclaimer);

        TabPane subTabPane = new TabPane();
        subTabPane.getTabs().addAll(
                createRaceConditionSubTab(),
                createCollectionsSubTab(),
                createGenericCacheSubTab()
        );

        root.setCenter(subTabPane);
        setContent(root);
    }

    private Tab createRaceConditionSubTab() {
        Tab tab = new Tab("1. Race Condition Lab");
        tab.setClosable(false);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));

        Label info = new Label("Demonstrates race conditions on unsynchronized variables vs synchronized atomic protection.");
        info.setStyle("-fx-font-weight: bold;");

        TextField threadsField = new TextField("8");
        TextField incsField = new TextField("10000");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Concurrent Threads:"), 0, 0);
        grid.add(threadsField, 1, 0);
        grid.add(new Label("Increments per Thread:"), 0, 1);
        grid.add(incsField, 1, 1);

        Button runCounterBtn = new Button("Run Counter Race Test");
        Button runMapBtn = new Button("Run Plain HashMap Race Test");
        runCounterBtn.setStyle("-fx-base: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
        runMapBtn.setStyle("-fx-base: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox btnBox = new HBox(10, runCounterBtn, runMapBtn);

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setFont(javafx.scene.text.Font.font("Monospaced", 12));
        output.setPrefRowCount(12);

        runCounterBtn.setOnAction(e -> {
            try {
                int th = Integer.parseInt(threadsField.getText().trim());
                int inc = Integer.parseInt(incsField.getText().trim());
                var res = RaceConditionLab.runCounterRace(th, inc);
                output.setText(String.format("""
                    === Counter Race Condition Results ===
                    Expected Total:          %,d
                    Synchronized (Safe):     %,d (Elapsed: %.2f ms) - EXACT!
                    Unsynchronized (Unsafe): %,d (Elapsed: %.2f ms) - %s
                    Discrepancy (Lost Ops):  %,d
                    """,
                        res.expectedTotal(),
                        res.safeActual(), res.safeElapsedNanos() / 1_000_000.0,
                        res.unsafeActual(), res.unsafeElapsedNanos() / 1_000_000.0,
                        res.unsafeActual() == res.expectedTotal() ? "Accidentally matched" : "DATA CORRUPTED / LOST UPDATES!",
                        res.expectedTotal() - res.unsafeActual()
                ));
            } catch (Exception ex) {
                output.setText("Error: " + ex.getMessage());
            }
        });

        runMapBtn.setOnAction(e -> {
            try {
                int th = Integer.parseInt(threadsField.getText().trim());
                var res = RaceConditionLab.runUnsynchronizedMapRace(th, 1000);
                output.setText(String.format("""
                    === Plain HashMap Concurrency Test ===
                    Expected Total Entries: %,d
                    Actual Map Size:        %,d (Elapsed: %.2f ms)
                    Lost / Corrupted Keys:  %,d
                    Note: Unsynchronized HashMaps under concurrent writes suffer from silent data loss
                    and corrupted internal bucket chains!
                    """,
                        res.expectedEntries(),
                        res.actualEntries(),
                        res.elapsedNanos() / 1_000_000.0,
                        res.expectedEntries() - res.actualEntries()
                ));
            } catch (Exception ex) {
                output.setText("Error: " + ex.getMessage());
            }
        });

        content.getChildren().addAll(info, grid, btnBox, output);
        VBox.setVgrow(output, Priority.ALWAYS);
        tab.setContent(content);
        return tab;
    }

    private Tab createCollectionsSubTab() {
        Tab tab = new Tab("2. Collections Comparison");
        tab.setClosable(false);

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));

        Label info = new Label("Benchmarking Java Collections (Unit 4). Marks legacy Vector & Hashtable.");
        info.setStyle("-fx-font-weight: bold;");

        TableView<CollectionsLab.BenchmarkEntry> table = new TableView<>();
        ObservableList<CollectionsLab.BenchmarkEntry> data = FXCollections.observableArrayList();

        TableColumn<CollectionsLab.BenchmarkEntry, String> nameCol = new TableColumn<>("Collection");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().collectionType()));
        nameCol.setPrefWidth(120);

        TableColumn<CollectionsLab.BenchmarkEntry, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category()));
        catCol.setPrefWidth(80);

        TableColumn<CollectionsLab.BenchmarkEntry, Boolean> legCol = new TableColumn<>("Legacy");
        legCol.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().legacy()));
        legCol.setPrefWidth(65);

        TableColumn<CollectionsLab.BenchmarkEntry, Number> insCol = new TableColumn<>("Insert (µs)");
        insCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().insertNanos() / 1000));
        insCol.setPrefWidth(90);

        TableColumn<CollectionsLab.BenchmarkEntry, Number> lookCol = new TableColumn<>("Lookup (µs)");
        lookCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().lookupNanos() / 1000));
        lookCol.setPrefWidth(90);

        TableColumn<CollectionsLab.BenchmarkEntry, Number> iterCol = new TableColumn<>("Iterate (µs)");
        iterCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().iterateNanos() / 1000));
        iterCol.setPrefWidth(90);

        TableColumn<CollectionsLab.BenchmarkEntry, Number> remCol = new TableColumn<>("Remove (µs)");
        remCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().removeNanos() / 1000));
        remCol.setPrefWidth(90);

        table.getColumns().addAll(nameCol, catCol, legCol, insCol, lookCol, iterCol, remCol);
        table.setItems(data);

        Button runBtn = new Button("Run Comparison (1,000 items)");
        Button cmeBtn = new Button("Demo Fail-Fast CME");
        Button safeRemoveBtn = new Button("Demo Safe Iterator.remove()");

        runBtn.setStyle("-fx-base: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        runBtn.setOnAction(e -> {
            data.setAll(CollectionsLab.runComparisons(1000));
        });

        cmeBtn.setOnAction(e -> {
            boolean caught = CollectionsLab.demonstrateFailFastCme();
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "ConcurrentModificationException caught successfully: mutating collection during for-each iteration threw CME as expected!");
            alert.showAndWait();
        });

        safeRemoveBtn.setOnAction(e -> {
            boolean ok = CollectionsLab.demonstrateSafeIteratorRemove();
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Safe structural modification verified: elements removed cleanly using Iterator.remove() without exception!");
            alert.showAndWait();
        });

        HBox btnBox = new HBox(10, runBtn, cmeBtn, safeRemoveBtn);
        content.getChildren().addAll(info, btnBox, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        tab.setContent(content);
        return tab;
    }

    private Tab createGenericCacheSubTab() {
        Tab tab = new Tab("3. Generic Cache Demo");
        tab.setClosable(false);

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));

        Label info = new Label("Proves cache engine type safety with non-String types, wildcards, and bounded generic methods (Unit 5).");
        info.setStyle("-fx-font-weight: bold;");

        TextArea output = new TextArea();
        output.setEditable(false);
        output.setFont(javafx.scene.text.Font.font("Monospaced", 12));

        Button runStudentBtn = new Button("Test Cache<String, Student>");
        Button runIntegerBtn = new Button("Test Cache<Integer, String> & Bounded Generic <K extends Comparable>");
        Button runDoubleBtn = new Button("Test Cache<Long, Double>");

        runStudentBtn.setOnAction(e -> {
            try {
                Cache<String, Student> stdCache = GenericCacheLab.createStringStudentCache(10, Clock.systemUTC());
                stdCache.put("std:1", new Student(1, "Alice Sharma", 3.95));
                stdCache.put("std:2", new Student(2, "Bob Patel", 3.82));
                stdCache.put("std:3", new Student(3, "Charlie Davis", 3.65));

                StringBuilder sb = new StringBuilder("=== Cache<String, Student> Entries ===\n");
                for (String line : GenericCacheLab.inspectAllEntries(stdCache)) {
                    sb.append(line).append("\n");
                }
                output.setText(sb.toString());
            } catch (Exception ex) {
                output.setText("Error: " + ex.getMessage());
            }
        });

        runIntegerBtn.setOnAction(e -> {
            try {
                Cache<Integer, String> intCache = GenericCacheLab.createIntegerStringCache(10, Clock.systemUTC());
                intCache.put(500, "Five Hundred");
                intCache.put(100, "One Hundred");
                intCache.put(900, "Nine Hundred");

                Integer smallest = GenericCacheLab.smallestKey(intCache);
                StringBuilder sb = new StringBuilder("=== Cache<Integer, String> Entries ===\n");
                for (String line : GenericCacheLab.inspectAllEntries(intCache)) {
                    sb.append(line).append("\n");
                }
                sb.append("\nBounded Generic Method Result: smallestKey() = ").append(smallest);
                output.setText(sb.toString());
            } catch (Exception ex) {
                output.setText("Error: " + ex.getMessage());
            }
        });

        runDoubleBtn.setOnAction(e -> {
            try {
                Cache<Long, Double> dblCache = GenericCacheLab.createLongDoubleCache(10, Clock.systemUTC());
                dblCache.put(123456789L, 99.95);
                dblCache.put(987654321L, 149.50);

                StringBuilder sb = new StringBuilder("=== Cache<Long, Double> Entries ===\n");
                for (String line : GenericCacheLab.inspectAllEntries(dblCache)) {
                    sb.append(line).append("\n");
                }
                output.setText(sb.toString());
            } catch (Exception ex) {
                output.setText("Error: " + ex.getMessage());
            }
        });

        HBox btnBox = new HBox(10, runStudentBtn, runIntegerBtn, runDoubleBtn);
        content.getChildren().addAll(info, btnBox, output);
        VBox.setVgrow(output, Priority.ALWAYS);
        tab.setContent(content);
        return tab;
    }
}
