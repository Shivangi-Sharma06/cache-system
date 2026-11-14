package com.cse309.cache.awt;

import com.cse309.cache.core.CacheLookupResult;
import com.cse309.cache.core.CacheWriteResult;
import com.cse309.cache.core.StringCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.stats.StatisticsSnapshot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone AWT console demonstrating the Delegation Event Model (Unit 3).
 * Uses all four listener patterns: ActionListener, KeyListener, KeyAdapter, and WindowAdapter.
 * Imports strictly no JavaFX or JDBC dependencies. Runs in an isolated JVM.
 */
public final class AwtEventConsole extends Frame {

    private static final long serialVersionUID = 1L;

    private final transient StringCache privateCache;
    private final TextField commandField;
    private final TextArea outputArea;
    private final TextArea eventInspectorArea;
    private final Label charCountLabel;

    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    public AwtEventConsole() {
        super("AWT Delegation Event Console (CSE 309)");
        this.privateCache = new StringCache(5, EvictionPolicyType.LRU);

        setLayout(new BorderLayout(5, 5));
        setSize(780, 520);
        setLocationRelativeTo(null);
        setBackground(Color.LIGHT_GRAY);

        // Top control panel
        Panel topPanel = new Panel(new BorderLayout(5, 5));
        Panel inputPanel = new Panel(new BorderLayout(5, 5));
        Label promptLabel = new Label(" Command: ");
        promptLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        commandField = new TextField();
        commandField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(commandField, BorderLayout.CENTER);

        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        Button putBtn = new Button("Put");
        Button getBtn = new Button("Get");
        Button delBtn = new Button("Del");
        Button statsBtn = new Button("Stats");
        Button clearBtn = new Button("Clear");
        Button helpBtn = new Button("Help");

        buttonPanel.add(putBtn);
        buttonPanel.add(getBtn);
        buttonPanel.add(delBtn);
        buttonPanel.add(statsBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(helpBtn);

        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Center split: Output + Event Inspector
        Panel centerPanel = new Panel(new GridLayout(1, 2, 5, 5));

        Panel outputPanel = new Panel(new BorderLayout());
        Label outTitle = new Label(" Console Output (Private Cache: Cap=5, LRU)");
        outTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        outputArea = new TextArea("", 15, 30, TextArea.SCROLLBARS_VERTICAL_ONLY);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBackground(Color.WHITE);
        outputPanel.add(outTitle, BorderLayout.NORTH);
        outputPanel.add(outputArea, BorderLayout.CENTER);

        Panel inspectorPanel = new Panel(new BorderLayout());
        Label inspTitle = new Label(" Event Inspector (Delegation Event Model: Source -> Event -> Listener)");
        inspTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        eventInspectorArea = new TextArea("", 15, 30, TextArea.SCROLLBARS_VERTICAL_ONLY);
        eventInspectorArea.setEditable(false);
        eventInspectorArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        eventInspectorArea.setBackground(new Color(245, 245, 250));
        inspectorPanel.add(inspTitle, BorderLayout.NORTH);
        inspectorPanel.add(eventInspectorArea, BorderLayout.CENTER);

        centerPanel.add(outputPanel);
        centerPanel.add(inspectorPanel);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom status panel
        Panel bottomPanel = new Panel(new FlowLayout(FlowLayout.RIGHT));
        charCountLabel = new Label("Length: 0 chars | Events: Active ");
        charCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        bottomPanel.add(charCountLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // =====================================================================
        // SYLLABUS U3 EVENT MODEL: ALL FOUR LISTENER STYLES
        // =====================================================================

        // Style 1: Named class implementing ActionListener
        CommandActionListener actionListener = new CommandActionListener();
        commandField.addActionListener(actionListener);
        putBtn.addActionListener(actionListener);
        getBtn.addActionListener(actionListener);
        delBtn.addActionListener(actionListener);
        statsBtn.addActionListener(actionListener);
        clearBtn.addActionListener(actionListener);
        helpBtn.addActionListener(actionListener);

        // Style 2: Class implementing KeyListener with all three methods
        commandField.addKeyListener(new HistoryNavigationKeyListener());

        // Style 3: KeyAdapter overriding only keyReleased for live character count
        commandField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                logInspectorEvent(e, "KeyAdapter (keyReleased)");
                charCountLabel.setText("Length: " + commandField.getText().length() + " chars | Events: Active ");
            }
        });

        // Style 4: WindowAdapter overriding windowClosing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logInspectorEvent(e, "WindowAdapter (windowClosing)");
                dispose();
                System.exit(0);
            }
        });

        appendOutput("AWT Delegation Event Console Initialized.");
        appendOutput("Type commands above or click buttons. Examples:");
        appendOutput("  put myKey myValue 60");
        appendOutput("  get myKey");
        appendOutput("  stats\n");
    }

    private void logInspectorEvent(java.awt.AWTEvent e, String listenerType) {
        String source = e.getSource().getClass().getSimpleName();
        String eventType = e.getClass().getSimpleName();
        String details = e.paramString();
        eventInspectorArea.append(String.format("[%s] Source=%s | Event=%s\n  %s\n",
                listenerType, source, eventType, details));
    }

    private void appendOutput(String line) {
        outputArea.append(line + "\n");
    }

    /**
     * Style 1: Named ActionListener handling button clicks and TextField Enter key.
     */
    private final class CommandActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            logInspectorEvent(e, "ActionListener");
            String cmd = e.getActionCommand();

            if (e.getSource() == commandField) {
                executeInput(commandField.getText().trim());
            } else if ("Put".equals(cmd)) {
                executeInput("put demoKey demoValue 30");
            } else if ("Get".equals(cmd)) {
                executeInput("get demoKey");
            } else if ("Del".equals(cmd)) {
                executeInput("del demoKey");
            } else if ("Stats".equals(cmd)) {
                executeInput("stats");
            } else if ("Clear".equals(cmd)) {
                executeInput("clear");
            } else if ("Help".equals(cmd)) {
                executeInput("help");
            }
        }
    }

    /**
     * Style 2: KeyListener implementing all three methods for command history recall.
     */
    private final class HistoryNavigationKeyListener implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            logInspectorEvent(e, "KeyListener (keyPressed)");
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (!commandHistory.isEmpty() && historyIndex > 0) {
                    historyIndex--;
                    commandField.setText(commandHistory.get(historyIndex));
                    commandField.setCaretPosition(commandField.getText().length());
                }
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;
                    commandField.setText(commandHistory.get(historyIndex));
                    commandField.setCaretPosition(commandField.getText().length());
                } else {
                    historyIndex = commandHistory.size();
                    commandField.setText("");
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            logInspectorEvent(e, "KeyListener (keyTyped)");
        }

        @Override
        public void keyReleased(KeyEvent e) {
            logInspectorEvent(e, "KeyListener (keyReleased)");
        }
    }

    private void executeInput(String input) {
        if (input == null || input.isBlank()) return;

        commandHistory.add(input);
        historyIndex = commandHistory.size();
        commandField.setText("");
        charCountLabel.setText("Length: 0 chars | Events: Active ");

        appendOutput("> " + input);
        String[] tokens = input.split("\\s+");
        String verb = tokens[0].toLowerCase();

        try {
            switch (verb) {
                case "put" -> {
                    if (tokens.length < 3) {
                        appendOutput("Usage: put <key> <value> [ttlSeconds]");
                        return;
                    }
                    String key = tokens[1];
                    String val = tokens[2];
                    CacheWriteResult<String> result;
                    if (tokens.length >= 4) {
                        int ttl = Integer.parseInt(tokens[3]);
                        result = privateCache.put(key, val, Duration.ofSeconds(ttl));
                    } else {
                        result = privateCache.put(key, val);
                    }
                    appendOutput("Result: " + result.getStatus() + " (key=" + key + ")");
                    if (result.hadEvictions()) {
                        appendOutput("Evicted: " + result.getEvictions());
                    }
                }
                case "get" -> {
                    if (tokens.length < 2) {
                        appendOutput("Usage: get <key>");
                        return;
                    }
                    CacheLookupResult<String> lookup = privateCache.get(tokens[1]);
                    appendOutput("Result: " + lookup.getStatus() +
                            (lookup.isHit() ? " => " + lookup.getValueOrNull() : ""));
                }
                case "del" -> {
                    if (tokens.length < 2) {
                        appendOutput("Usage: del <key>");
                        return;
                    }
                    CacheLookupResult<String> rem = privateCache.remove(tokens[1]);
                    appendOutput("Result: " + rem.getStatus() +
                            (rem.isHit() ? " (removed value: " + rem.getValueOrNull() + ")" : ""));
                }
                case "stats" -> {
                    StatisticsSnapshot s = privateCache.getStatistics().snapshot(privateCache.size(), privateCache.getCapacity());
                    appendOutput(String.format("Stats: size=%d/%d | Hits=%d, Misses=%d (%.1f%%) | Evictions=%d, Expirations=%d",
                            s.getCurrentSize(), s.getCurrentCapacity(), s.getHits(), s.getMisses(),
                            s.getHitRatio(), s.getEvictions(), s.getExpirations()));
                }
                case "clear" -> {
                    privateCache.clear();
                    appendOutput("Cache cleared. Current size: " + privateCache.size());
                }
                case "help" -> {
                    appendOutput("Supported Commands:");
                    appendOutput("  put <key> <value> [ttlSeconds] - Store entry");
                    appendOutput("  get <key>                     - Fetch entry");
                    appendOutput("  del <key>                     - Remove entry");
                    appendOutput("  stats                         - Display hit/miss metrics");
                    appendOutput("  clear                         - Flush private cache");
                }
                default -> appendOutput("Unknown command '" + verb + "'. Type 'help' for usage.");
            }
        } catch (Exception ex) {
            appendOutput("Error: " + ex.getMessage());
        }
    }

    public static Process launchInNewProcess() throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = AwtEventConsole.class.getName();

        ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath, className);
        return pb.start();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            AwtEventConsole console = new AwtEventConsole();
            console.setVisible(true);
        });
    }
}
