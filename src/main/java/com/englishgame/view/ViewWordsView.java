package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * View Words Window
 * Displays all saved words and expressions
 */
@Slf4j
public class ViewWordsView extends JFrame {

    private final GameController gameController;
    private final LandingPageView landingPage;
    
    private enum SortMode {
        SPANISH_AZ, ENGLISH_AZ, SCORE_DESC
    }

    private static final class RowData {
        private final String expression;
        private final String translation;
        private final int score;
        private final String spanishKey;
        private final String englishKey;

        private RowData(String expression, String translation, int score, String spanishKey, String englishKey) {
            this.expression = expression;
            this.translation = translation;
            this.score = score;
            this.spanishKey = spanishKey;
            this.englishKey = englishKey;
        }
    }

    // Data storage for filtering/sorting
    private final List<RowData> allData = new ArrayList<>();

    // Components
    private JComboBox<String> databaseSelector;
    private JTable wordsTable;
    private JButton refreshButton;
    private JButton deleteAllButton;
    private JTextField searchField;
    private JComboBox<String> sortSelector;
    private JLabel recordsCountLabel;
    private JButton backToLandingButton;
    private JButton dataManagementButton;
    private JButton playGameButton;
    private JButton learnedWordsButton;

    public ViewWordsView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("Saved Words");
        setSize(1000, 800);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        
        initComponents();
        setupLayout();
        addListeners();
        refreshDatabaseSelector();
        refreshWordsTable();
        
        log.info("View words window initialized");
    }

    private void initComponents() {
        // Database selector
        databaseSelector = new JComboBox<>();
        databaseSelector.setPreferredSize(new Dimension(200, 30));
        
        // Words table
        String[] columnNames = {"Expression", "Translation", "Score", "Move", "Delete"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        wordsTable = new JTable(tableModel);
        wordsTable.setRowHeight(35);
        wordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        wordsTable.getColumn("Move").setCellRenderer(new ButtonRenderer("Move"));
        wordsTable.getColumn("Delete").setCellRenderer(new ButtonRenderer("Delete"));
        
        wordsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = wordsTable.rowAtPoint(e.getPoint());
                int col = wordsTable.columnAtPoint(e.getPoint());
                
                if (row >= 0 && col >= 0) {
                    String columnName = wordsTable.getColumnName(col);
                    if ("Move".equals(columnName)) {
                        handleMoveExpression(row);
                    } else if ("Delete".equals(columnName)) {
                        handleDeleteExpression(row);
                    }
                }
            }
        });
        
        refreshButton = createStyledButton("Refresh", "Refresh the words list");
        deleteAllButton = createStyledButton("Delete All",
                "Remove every expression from the database currently selected above");
        backToLandingButton = createStyledButton("Back to Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("Manage Data", "Go to data management");
        playGameButton = createStyledButton("Play Game", "Start interactive game");
        learnedWordsButton = createStyledButton("Learned Words", "View learned words");
        
        // Search field
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setToolTipText("Search expressions...");
        searchField.setPreferredSize(new Dimension(200, 30));

        sortSelector = new JComboBox<>(new String[] {
                "Español (A-Z)", "Inglés (A-Z)", "Score (mayor a menor)"
        });
        sortSelector.setPreferredSize(new Dimension(220, 30));
        sortSelector.setToolTipText("Ordena los registros por español, inglés o score");

        recordsCountLabel = new JLabel("Registros: 0");
        recordsCountLabel.setFont(new Font("Arial", Font.BOLD, 13));
        recordsCountLabel.setForeground(new Color(70, 70, 80));
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(150, 40));
        button.setMinimumSize(new Dimension(120, 35));
        button.setMaximumSize(new Dimension(200, 45));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        
        // Assign different colors based on button function
        Color buttonColor = getButtonColor(text);
        button.setBackground(buttonColor);
        
        // Create darker border color
        Color borderColor = new Color(
            Math.max(0, buttonColor.getRed() - 30),
            Math.max(0, buttonColor.getGreen() - 30),
            Math.max(0, buttonColor.getBlue() - 30)
        );
        button.setBorder(BorderFactory.createLineBorder(borderColor, 2));
        
        // Enhanced hover effect with subtle glow
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                Color hoverColor = new Color(
                    Math.min(255, buttonColor.getRed() + 25),
                    Math.min(255, buttonColor.getGreen() + 25),
                    Math.min(255, buttonColor.getBlue() + 25)
                );
                button.setBackground(hoverColor);
                // Create a subtle glow effect with lighter border
                Color glowBorder = new Color(
                    Math.min(255, buttonColor.getRed() + 50),
                    Math.min(255, buttonColor.getGreen() + 50),
                    Math.min(255, buttonColor.getBlue() + 50)
                );
                button.setBorder(BorderFactory.createLineBorder(glowBorder, 2));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(buttonColor);
                button.setBorder(BorderFactory.createLineBorder(borderColor, 2));
            }
        });
        
        return button;
    }

    private Color getButtonColor(String buttonText) {
        // Assign colors based on button function
        if (buttonText.contains("Refresh")) {
            return new Color(59, 130, 246); // Vibrant blue for refresh
        } else if (buttonText.contains("Borrar todo")
                || (buttonText.contains("Delete") && buttonText.contains("All"))) {
            return new Color(220, 38, 127); // Vibrant pink for delete all
        } else if (buttonText.contains("Manage") || buttonText.contains("Data")) {
            return new Color(37, 99, 235); // Vibrant blue for data management
        } else if (buttonText.contains("View") || buttonText.contains("Words")) {
            return new Color(16, 185, 129); // Vibrant green for view actions
        } else if (buttonText.contains("Play") || buttonText.contains("Game")) {
            return new Color(245, 101, 101); // Vibrant coral for game
        } else if (buttonText.contains("Learned")) {
            return new Color(124, 58, 237); // Vibrant purple for learned words
        } else if (buttonText.contains("Back") || buttonText.contains("Main")) {
            return new Color(220, 38, 127); // Vibrant pink for return actions
        } else {
            return new Color(59, 130, 246); // Default vibrant blue
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Create main panel with soft background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create soft gradient from light cream to very light blue
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(248, 250, 252),  // Very light cream at top
                    0, getHeight(), new Color(240, 248, 255)  // Very light blue at bottom
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Database Selection Section
        JPanel dbSection = createSectionPanel("Database Selection");
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dbPanel.add(new JLabel("Database:"));
        dbPanel.add(databaseSelector);
        dbPanel.add(Box.createHorizontalStrut(10));
        dbPanel.add(refreshButton);
        dbPanel.add(Box.createHorizontalStrut(10));
        dbPanel.add(deleteAllButton);
        dbSection.add(dbPanel);
        
        // Search Section
        JPanel searchSection = createSectionPanel("Search");
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(new JLabel("Orden:"));
        searchPanel.add(sortSelector);
        searchPanel.add(Box.createHorizontalStrut(14));
        searchPanel.add(recordsCountLabel);
        searchSection.add(searchPanel);
        
        // Words Table Section
        JPanel tableSection = createSectionPanel("Words and Expressions");
        JScrollPane tableScrollPane = new JScrollPane(wordsTable);
        tableScrollPane.setPreferredSize(new Dimension(900, 400));
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableSection.add(tableScrollPane);
        
        // Navigation Section
        JPanel navSection = createSectionPanel("Navigation");
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        navPanel.add(dataManagementButton);
        navPanel.add(playGameButton);
        navPanel.add(learnedWordsButton);
        navPanel.add(backToLandingButton);
        navSection.add(navPanel);
        
        // Add all sections to main panel
        mainPanel.add(dbSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(searchSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(tableSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(navSection);
        
        // Add to frame with scroll
        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(mainScrollPane, BorderLayout.CENTER);
    }

    private JPanel createSectionPanel(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2), title));
        section.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        section.setOpaque(true);
        return section;
    }

    private void addListeners() {
        // Database selection
        databaseSelector.addActionListener(e -> refreshWordsTable());
        
        // Search field
        searchField.addActionListener(e -> filterWordsTable());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterWordsTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterWordsTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterWordsTable(); }
        });
        sortSelector.addActionListener(e -> filterWordsTable());
        
        // Buttons
        refreshButton.addActionListener(e -> refreshWordsTable());
        deleteAllButton.addActionListener(e -> {
            String db = (String) databaseSelector.getSelectedItem();
            if (db == null) {
                JOptionPane.showMessageDialog(this, "Please select a database first", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            deleteAllExpressionsForDatabase(db);
        });
        backToLandingButton.addActionListener(e -> returnToLanding());
        dataManagementButton.addActionListener(e -> openDataManagement());
        playGameButton.addActionListener(e -> openGame());
        learnedWordsButton.addActionListener(e -> openLearnedWords());
    }

    private void refreshDatabaseSelector() {
        databaseSelector.removeAllItems();
        gameController.getAvailableDatabases().forEach(databaseSelector::addItem);
        log.debug("Database selector refreshed with {} databases", databaseSelector.getItemCount());
    }

    private void refreshWordsTable() {
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            return;
        }
        
        DefaultTableModel model = (DefaultTableModel) wordsTable.getModel();
        model.setRowCount(0); // Clear existing data
        allData.clear(); // Clear stored data
        
        try {
            // Get Spanish expressions
            List<SpanishExpression> spanishExpressions = gameController.getSpanishExpressionsFromDatabase(selectedDb);
            for (SpanishExpression spanish : spanishExpressions) {
                String translations = spanish.getTranslations().stream()
                    .map(EnglishExpression::getExpression)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");
                
                Object[] rowData = {
                    spanish.getExpression(),
                    translations,
                    spanish.getScore(),
                    "Move"
                };
                allData.add(new RowData(
                        rowData[0].toString(),
                        rowData[1].toString(),
                        (Integer) rowData[2],
                        spanish.getExpression(),
                        translations));
            }
            
            // Get English expressions
            List<EnglishExpression> englishExpressions = gameController.getEnglishExpressionsFromDatabase(selectedDb);
            for (EnglishExpression english : englishExpressions) {
                String translations = english.getTranslations().stream()
                    .map(SpanishExpression::getExpression)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");
                
                Object[] rowData = {
                    english.getExpression(),
                    translations,
                    english.getScore(),
                    "Move"
                };
                allData.add(new RowData(
                        rowData[0].toString(),
                        rowData[1].toString(),
                        (Integer) rowData[2],
                        translations,
                        english.getExpression()));
            }

            filterWordsTable();
            
            log.info("Words table refreshed with {} expressions from database '{}'", 
                model.getRowCount(), selectedDb);
            
        } catch (Exception e) {
            log.error("Error refreshing words table", e);
            JOptionPane.showMessageDialog(this, "Error loading words: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDataManagement() {
        log.info("Opening data management window");
        this.setVisible(false);
        DataManagementView dataManagementView = new DataManagementView(gameController, landingPage);
        dataManagementView.setVisible(true);
    }

    private void openGame() {
        log.info("Opening game window");
        this.setVisible(false);
        GameView gameView = new GameView(gameController, landingPage);
        gameView.setVisible(true);
    }

    private void openLearnedWords() {
        log.info("Opening learned words window");
        this.setVisible(false);
        LearnedWordsView learnedWordsView = new LearnedWordsView(gameController, landingPage);
        learnedWordsView.setVisible(true);
    }

    private void returnToLanding() {
        log.info("Returning to landing page");
        this.setVisible(false);
        landingPage.returnToLanding();
    }

    private void filterWordsTable() {
        String searchText = searchField.getText().toLowerCase().trim();
        DefaultTableModel model = (DefaultTableModel) wordsTable.getModel();
        model.setRowCount(0); // Clear existing data

        List<RowData> filtered = new ArrayList<>();
        for (RowData rowData : allData) {
            String expression = rowData.expression.toLowerCase();
            String translation = rowData.translation.toLowerCase();
            if (searchText.isEmpty() || expression.contains(searchText) || translation.contains(searchText)) {
                filtered.add(rowData);
            }
        }

        filtered.sort(buildComparatorForSelectedSort());

        for (RowData rowData : filtered) {
            model.addRow(new Object[] {
                    rowData.expression, rowData.translation, rowData.score, "Move", "Delete"
            });
        }

        recordsCountLabel.setText("Registros: " + filtered.size());
        log.debug("Table filtered with search text: '{}', showing {} rows", searchText, model.getRowCount());
    }

    private Comparator<RowData> buildComparatorForSelectedSort() {
        SortMode mode = selectedSortMode();
        Comparator<RowData> byExpression = Comparator.comparing(r -> r.expression, String.CASE_INSENSITIVE_ORDER);
        return switch (mode) {
            case ENGLISH_AZ -> Comparator.comparing((RowData r) -> r.englishKey, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byExpression);
            case SCORE_DESC -> Comparator.comparingInt((RowData r) -> r.score)
                    .reversed()
                    .thenComparing(byExpression);
            case SPANISH_AZ -> Comparator.comparing((RowData r) -> r.spanishKey, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byExpression);
        };
    }

    private SortMode selectedSortMode() {
        String selected = (String) sortSelector.getSelectedItem();
        if (selected == null) {
            return SortMode.SPANISH_AZ;
        }
        if (selected.startsWith("Ingl")) {
            return SortMode.ENGLISH_AZ;
        }
        if (selected.startsWith("Score")) {
            return SortMode.SCORE_DESC;
        }
        return SortMode.SPANISH_AZ;
    }

    private void deleteAllExpressionsForDatabase(String databaseName) {
        log.info("deleteAllExpressionsForDatabase called for database: {}", databaseName);

        if (databaseName == null || databaseName.isBlank()) {
            log.warn("No database name for delete all operation");
            JOptionPane.showMessageDialog(this, "Base de datos no válida.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "¿Seguro de borrar TODAS las expresiones de la base \"" + databaseName + "\"?\n\nEsta acción no se puede deshacer.",
                "Confirmar borrar todo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        log.info("User confirmation result: {}", result);

        if (result != JOptionPane.YES_OPTION) {
            log.info("User cancelled delete all operation for {}", databaseName);
            return;
        }

        try {
            boolean deleted = gameController.deleteAllExpressions(databaseName);
            log.info("deleteAllExpressions result for {}: {}", databaseName, deleted);

            if (deleted) {
                JOptionPane.showMessageDialog(this,
                        "Se eliminaron todas las expresiones de la base \"" + databaseName + "\".",
                        "Borrado completado", JOptionPane.INFORMATION_MESSAGE);

                String selectedDb = (String) databaseSelector.getSelectedItem();
                if (databaseName.equals(selectedDb)) {
                    refreshWordsTable();
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "No había expresiones que borrar en \"" + databaseName + "\".",
                        "Sin datos", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            log.error("Error deleting all expressions", e);
            JOptionPane.showMessageDialog(this, "Error al borrar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void handleMoveExpression(int row) {
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String expression = (String) wordsTable.getValueAt(row, 0);
        log.info("Move button clicked for expression: {}", expression);
        
        // Get available databases excluding the currently selected one (trim/case-insensitive).
        String selectedNorm = selectedDb == null ? "" : selectedDb.trim().toLowerCase();
        List<String> availableDatabases = gameController.getAvailableDatabases().stream()
                .filter(db -> db != null && !db.trim().isEmpty())
                .filter(db -> !db.trim().toLowerCase().equals(selectedNorm))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(java.util.stream.Collectors.toList());
        
        if (availableDatabases.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No other databases available to move the expression to.", 
                "No Target Database", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Show database selection dialog
        String targetDatabase = (String) JOptionPane.showInputDialog(
            this,
            "Select target database to move '" + expression + "' to:",
            "Move Expression",
            JOptionPane.QUESTION_MESSAGE,
            null,
            availableDatabases.toArray(),
            availableDatabases.get(0)
        );
        
        if (targetDatabase != null && !targetDatabase.isEmpty()) {
            // Confirm the move
            int confirmResult = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to move '" + expression + "' from '" + selectedDb + "' to '" + targetDatabase + "'?",
                "Confirm Move", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (confirmResult == JOptionPane.YES_OPTION) {
                try {
                    boolean moved = gameController.moveExpression(selectedDb, targetDatabase, expression);
                    
                    if (moved) {
                        JOptionPane.showMessageDialog(this, 
                            "Expression '" + expression + "' moved successfully to '" + targetDatabase + "'!",
                            "Move Successful", JOptionPane.INFORMATION_MESSAGE);
                        
                        // Refresh the table to show updated data
                        refreshWordsTable();
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Failed to move expression '" + expression + "'. It may not exist in the source database.",
                            "Move Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log.error("Error moving expression", e);
                    JOptionPane.showMessageDialog(this, 
                        "Error moving expression: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void handleDeleteExpression(int row) {
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String expression = (String) wordsTable.getValueAt(row, 0);
        int confirmResult = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete '" + expression + "' from '" + selectedDb + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmResult != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted = gameController.deleteExpression(selectedDb, expression);
            if (deleted) {
                JOptionPane.showMessageDialog(this,
                        "Expression '" + expression + "' deleted successfully.",
                        "Delete Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshWordsTable();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not delete '" + expression + "'.",
                        "Delete Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Error deleting expression '{}'", expression, e);
            JOptionPane.showMessageDialog(this,
                    "Error deleting expression: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Custom button renderer for table cells
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(String text) {
            setOpaque(true);
            setText(text);
            setFont(new Font("Arial", Font.BOLD, 10));
            setPreferredSize(new Dimension(60, 25));
            setMinimumSize(new Dimension(50, 20));
            setMaximumSize(new Dimension(70, 30));
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // Always maintain button appearance regardless of row selection
            setForeground(Color.BLACK); // Always black text for visibility
            setBackground(UIManager.getColor("Button.background"));
            
            // Add subtle border to make buttons more visible
            setBorder(BorderFactory.createRaisedBevelBorder());
            
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

}
