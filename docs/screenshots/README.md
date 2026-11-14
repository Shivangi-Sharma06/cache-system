# Required Screenshots for Project Submission

According to Master Specification Section 0, 17, and 18, screenshots must be captured from actual execution by the human evaluator. Never fabricate or invent screenshots.

Save the captured PNG images in this directory (`docs/screenshots/`).

---

## Screenshot Inventory & Capture Checklist

| Filename | View / Component | How to Trigger / What to Show |
|---|---|---|
| `01_dashboard_tab.png` | **Dashboard Tab** | Start the app (`mvn javafx:run`). Put several items and run a short simulation. Show the DropShadow metric cards, the hit ratio `LineChart`, and the live eviction log `ListView`. |
| `02_operations_tab.png` | **Operations Tab** | Navigate to Tab 2 (Ctrl+2). Perform a `PUT` operation with key `user:101`, value `Alice`, and TTL `60s`. Show the green result status card, microsecond latency badge, and recent outcomes list. |
| `03_explorer_tab.png` | **Explorer Tab** | Navigate to Tab 3 (Ctrl+3). Populate keys with namespaces (e.g. `user:1`, `user:2`, `order:99`). Show the `TreeView` on the left grouping by namespace and the `TableView` on the right displaying metadata (Created, Last Access, Remaining TTL). |
| `04_configuration_tab.png` | **Configuration Tab** | Navigate to Tab 4 (Ctrl+4). Show the Eviction Policy `RadioButton` group (`LRU`, `FIFO`, `LFU`), TTL selectors, and the capacity reduction confirmation warning dialog. |
| `05_simulator_tab.png` | **Simulator Tab** | Navigate to Tab 5 (Ctrl+5). Configure 8 worker threads and 25,000 operations with `Hotspot 80/20`. Click **Start**. Capture the screen while running to show the animated `ProgressBar`, `ThreadStateMonitor` `ListView` (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING), and live throughput. |
| `06_statistics_tab.png` | **Statistics Tab** | Navigate to Tab 6 (Ctrl+6). Show the per-operation latency breakdown table, the `LatencyHistogram` `BarChart` (`<1µs`, `1-10µs`, `10-100µs`, `100µs-1ms`, `1-10ms`, `>10ms`), and click **Run Benchmark Comparison** to display the LRU vs FIFO vs LFU benchmark chart. |
| `07_persistence_tab.png` | **Persistence Tab** | Navigate to Tab 7 (Ctrl+7). Show the SQLite snapshots `TableView`, the WAL mode indicator, the file snapshot save/load controls, and CSV export buttons. |
| `08_history_tab.png` | **History & Logs Tab** | Navigate to Tab 8 (Ctrl+8). Show the bounded `LinkedList` operation history audit table with operation type filter and the live `InMemoryLogHandler` log viewer. |
| `09_concepts_lab_tab.png` | **Concepts Lab Tab** | Navigate to Tab 9 (Ctrl+9). Run the **Race Condition Lab** showing safe vs unsafe counter discrepancies, the **Collections Comparison** table, and the **Generic Cache Demo** with Student domain objects. |
| `10_awt_event_console.png` | **AWT Event Console** | Open via **Tools > Launch AWT Event Console** (or `java -cp target/classes com.cse309.cache.awt.AwtEventConsole`). Execute `put session:1 active 30` and press Enter. Show the lower Event Inspector text area capturing `ActionEvent`, `KeyEvent`, and window events. |
| `11_about_dialog.png` | **About Dialog** | Open via **Help > About**. Show the dialog presenting course information (CSE 309 Advanced Java Programming), author details, and syllabus technology mappings. |

---

## Instructions for Human Capture
1. Launch the application:
   ```bash
   mvn javafx:run
   ```
2. Follow the table above to navigate to each tab and trigger the respective state.
3. Use your desktop screenshot utility (e.g., Spectacle, Flameshot, GNOME Screenshot, or PrtScn) to capture the window and save it with the exact filenames listed above.
