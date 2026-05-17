package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.EnglishExpression;
import com.englishgame.util.InclusionDisplay;
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
 * Learned Words Window
 * Displays all learned words and expressions
 */
@Slf4j
public class LearnedWordsView extends JFrame {

    private static final String LEARNED_WORDS_DB = "learned_words";

    private final GameController gameController;
    private final LandingPageView landingPage;
    
    private enum SortMode {
        SPANISH_AZ, ENGLISH_AZ, SCORE_DESC, INCLUSION_DESC, INCLUSION_ASC, SOURCE_DB_AZ
    }

    private static final class RowData {
        private final String expression;
        private final String translation;
        private final int score;
        private final String spanishKey;
        private final String englishKey;
        private final long includedAtMillis;
        private final String practiceSourceDatabase;

        private RowData(String expression, String translation, int score, String spanishKey, String englishKey,
                long includedAtMillis, String practiceSourceDatabase) {
            this.expression = expression;
            this.translation = translation;
            this.score = score;
            this.spanishKey = spanishKey;
            this.englishKey = englishKey;
            this.includedAtMillis = includedAtMillis;
            this.practiceSourceDatabase = practiceSourceDatabase;
        }
    }

    // Data storage for filtering/sorting
    private final List<RowData> allData = new ArrayList<>();

    // Components
    private JTable learnedWordsTable;
    private JTextField searchField;
    private JComboBox<String> sortSelector;
    private JLabel recordsCountLabel;
    private JButton refreshButton;
    private JButton deleteAllButton;
    private JButton reviewButton;
    private JButton backToLandingButton;
    private JButton dataManagementButton;
    private JButton viewWordsButton;
    private JButton playGameButton;

    public LearnedWordsView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("Learned Words - English Learning Game");
        setSize(1000, 800);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        
        initComponents();
        setupLayout();
        addListeners();
        refreshLearnedWordsTable();
        
        log.info("Learned words window initialized");
    }

    private void initComponents() {
        // Learned words table
        String[] columnNames = {"Expression", "Translation", "Score", "Inclusión", "BBDD origen", "Move", "Delete"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5 || column == 6;
            }
        };
        learnedWordsTable = new JTable(tableModel);
        learnedWordsTable.setRowHeight(35);
        learnedWordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set custom renderer for button columns
        learnedWordsTable.getColumn("Move").setCellRenderer(new ButtonRenderer("Move"));
        learnedWordsTable.getColumn("Move").setCellEditor(new ButtonEditor(new JCheckBox()));
        learnedWordsTable.getColumn("Delete").setCellRenderer(new ButtonRenderer("Delete"));
        learnedWordsTable.getColumn("Delete").setCellEditor(new ButtonEditor(new JCheckBox()));
        
        // Search field
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setToolTipText("Search learned words...");
        searchField.setPreferredSize(new Dimension(200, 30));

        sortSelector = new JComboBox<>(new String[] {
                "Español (A-Z)", "Inglés (A-Z)", "Score (mayor a menor)",
                "Inclusión (reciente primero)", "Inclusión (antigua primero)",
                "BBDD origen (A-Z)"
        });
        sortSelector.setPreferredSize(new Dimension(300, 30));
        sortSelector.setToolTipText("Ordena por español, inglés, score, inclusión o base de práctica de origen");

        recordsCountLabel = new JLabel("Registros: 0");
        recordsCountLabel.setFont(new Font("Arial", Font.BOLD, 13));
        recordsCountLabel.setForeground(new Color(70, 70, 80));
        
        // Buttons
        refreshButton = createStyledButton("Refresh", "Refresh the learned words list");
        deleteAllButton = createStyledButton("Delete All", "Delete all learned words");
        reviewButton = createStyledButton("Review", "Review learned words");
        backToLandingButton = createStyledButton("Back to Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("Manage Data", "Go to data management");
        viewWordsButton = createStyledButton("View Words", "View all saved words");
        playGameButton = createStyledButton("Play Game", "Start interactive game");
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
        } else if (buttonText.contains("Delete") && buttonText.contains("All")) {
            return new Color(220, 38, 127); // Vibrant pink for delete all
        } else if (buttonText.contains("Review")) {
            return new Color(124, 58, 237); // Vibrant purple for review
        } else if (buttonText.contains("Manage") || buttonText.contains("Data")) {
            return new Color(37, 99, 235); // Vibrant blue for data management
        } else if (buttonText.contains("View") || buttonText.contains("Words")) {
            return new Color(16, 185, 129); // Vibrant green for view actions
        } else if (buttonText.contains("Play") || buttonText.contains("Game")) {
            return new Color(245, 101, 101); // Vibrant coral for game
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
        
        // Learned Words Table Section
        JPanel tableSection = createSectionPanel("Learned Words and Expressions");
        JScrollPane tableScrollPane = new JScrollPane(learnedWordsTable);
        tableScrollPane.setPreferredSize(new Dimension(900, 400));
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableSection.add(tableScrollPane);
        
        // Actions Section
        JPanel actionsSection = createSectionPanel("Actions");
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        actionsPanel.add(refreshButton);
        actionsPanel.add(Box.createHorizontalStrut(10));
        actionsPanel.add(deleteAllButton);
        actionsPanel.add(Box.createHorizontalStrut(10));
        actionsPanel.add(reviewButton);
        actionsSection.add(actionsPanel);
        
        // Navigation Section
        JPanel navSection = createSectionPanel("Navigation");
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        navPanel.add(dataManagementButton);
        navPanel.add(viewWordsButton);
        navPanel.add(playGameButton);
        navPanel.add(backToLandingButton);
        navSection.add(navPanel);
        
        // Add all sections to main panel
        mainPanel.add(searchSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(tableSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(actionsSection);
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
        // Search field
        searchField.addActionListener(e -> filterLearnedWordsTable());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterLearnedWordsTable(); }
            public void removeUpdate(DocumentEvent e) { filterLearnedWordsTable(); }
            public void insertUpdate(DocumentEvent e) { filterLearnedWordsTable(); }
        });
        sortSelector.addActionListener(e -> filterLearnedWordsTable());
        
        // Buttons
        refreshButton.addActionListener(e -> refreshLearnedWordsTable());
        deleteAllButton.addActionListener(e -> deleteAllLearnedWords());
        reviewButton.addActionListener(e -> reviewLearnedWords());
        backToLandingButton.addActionListener(e -> returnToLanding());
        dataManagementButton.addActionListener(e -> openDataManagement());
        viewWordsButton.addActionListener(e -> openViewWords());
        playGameButton.addActionListener(e -> openGame());
    }

    private void refreshLearnedWordsTable() {
        DefaultTableModel model = (DefaultTableModel) learnedWordsTable.getModel();
        model.setRowCount(0); // Clear existing data
        allData.clear(); // Clear stored data
        
        try {
            // Get learned words from the learned_words database
            List<EnglishExpression> learnedWords = gameController.getEnglishExpressionsFromDatabase(LEARNED_WORDS_DB);
            
            for (EnglishExpression learnedWord : learnedWords) {
                String spanishTranslations = learnedWord.getTranslations().stream()
                    .map(spanish -> spanish.getExpression())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");
                
                allData.add(new RowData(
                        learnedWord.getExpression(),
                        spanishTranslations,
                        learnedWord.getScore(),
                        spanishTranslations,
                        learnedWord.getExpression(),
                        learnedWord.getIncludedAtEpochMillis(),
                        formatPracticeSourceForDisplay(learnedWord.getPracticeSourceDatabase())));
            }

            filterLearnedWordsTable();
            
            log.info("Learned words table refreshed with {} words", learnedWords.size());
            
        } catch (Exception e) {
            log.error("Error refreshing learned words table", e);
            JOptionPane.showMessageDialog(this, "Error loading learned words: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDataManagement() {
        log.info("Opening data management window");
        this.setVisible(false);
        DataManagementView dataManagementView = new DataManagementView(gameController, landingPage);
        dataManagementView.setVisible(true);
    }

    private void openViewWords() {
        log.info("Opening view words window");
        this.setVisible(false);
        ViewWordsView viewWordsView = new ViewWordsView(gameController, landingPage);
        viewWordsView.setVisible(true);
    }

    private void openGame() {
        log.info("Opening game window");
        this.setVisible(false);
        GameView gameView = new GameView(gameController, landingPage);
        gameView.setVisible(true);
    }

    private void returnToLanding() {
        log.info("Returning to landing page");
        this.setVisible(false);
        landingPage.returnToLanding();
    }

    private void filterLearnedWordsTable() {
        String searchText = searchField.getText().toLowerCase().trim();
        DefaultTableModel model = (DefaultTableModel) learnedWordsTable.getModel();
        model.setRowCount(0); // Clear existing data

        List<RowData> filtered = new ArrayList<>();
        for (RowData rowData : allData) {
            String expression = rowData.expression.toLowerCase();
            String translation = rowData.translation.toLowerCase();
            String sourceDb = rowData.practiceSourceDatabase.toLowerCase();
            if (searchText.isEmpty() || expression.contains(searchText) || translation.contains(searchText)
                    || sourceDb.contains(searchText)) {
                filtered.add(rowData);
            }
        }

        filtered.sort(buildComparatorForSelectedSort());

        for (RowData rowData : filtered) {
            model.addRow(new Object[] {
                    rowData.expression,
                    rowData.translation,
                    rowData.score,
                    InclusionDisplay.formatIncludedAt(rowData.includedAtMillis),
                    rowData.practiceSourceDatabase,
                    "Move",
                    "Delete"
            });
        }

        recordsCountLabel.setText("Registros: " + filtered.size());
        log.debug("Learned words table filtered with search text: '{}', showing {} rows", searchText, model.getRowCount());
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
            case INCLUSION_DESC -> Comparator.comparingLong((RowData r) -> r.includedAtMillis)
                    .reversed()
                    .thenComparing(byExpression);
            case INCLUSION_ASC -> Comparator.comparingLong((RowData r) -> r.includedAtMillis)
                    .thenComparing(byExpression);
            case SPANISH_AZ -> Comparator.comparing((RowData r) -> r.spanishKey, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byExpression);
            case SOURCE_DB_AZ -> Comparator.comparing((RowData r) -> r.practiceSourceDatabase, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byExpression);
        };
    }

    private static String formatPracticeSourceForDisplay(String practiceSourceDatabase) {
        if (practiceSourceDatabase == null || practiceSourceDatabase.isBlank()) {
            return "—";
        }
        return practiceSourceDatabase.trim();
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
        if (selected.startsWith("Inclusión")) {
            if (selected.contains("reciente")) {
                return SortMode.INCLUSION_DESC;
            }
            return SortMode.INCLUSION_ASC;
        }
        if (selected.startsWith("BBDD origen")) {
            return SortMode.SOURCE_DB_AZ;
        }
        return SortMode.SPANISH_AZ;
    }

    private void deleteAllLearnedWords() {
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete ALL learned words?\n\nThis action cannot be undone!",
            "Confirm Delete All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                log.info("Delete all learned words requested");
                boolean deleted = gameController.deleteAllExpressions(LEARNED_WORDS_DB);
                if (deleted) {
                    JOptionPane.showMessageDialog(this,
                            "Se eliminaron todas las palabras aprendidas.",
                            "Borrado completado", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "No había palabras aprendidas que borrar.",
                            "Sin datos", JOptionPane.INFORMATION_MESSAGE);
                }
                refreshLearnedWordsTable();
            } catch (Exception e) {
                log.error("Error deleting all learned words", e);
                JOptionPane.showMessageDialog(this, "Error al borrar palabras aprendidas: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reviewLearnedWords() {
        log.info("Review learned words requested");
        List<EnglishExpression> pending =
                gameController.getEnglishExpressionsFromDatabase(LEARNED_WORDS_DB);
        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay ninguna expresión en Learned Words para repasar.", "Review",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        setVisible(false);
        LearnedWordsReviewView review = new LearnedWordsReviewView(gameController, this, landingPage, () -> {
            LearnedWordsView.this.setVisible(true);
            refreshLearnedWordsTable();
        });
        review.setVisible(true);
    }

    private void moveLearnedWordToPractice(String englishExpression) {
        if (englishExpression == null || englishExpression.isBlank()) {
            return;
        }
        List<String> targets = new ArrayList<>(gameController.getAvailableDatabases());
        if (targets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No other vocabulary database is available to move this word into.",
                    "No Target Database", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String targetDatabase = (String) JOptionPane.showInputDialog(
                this,
                "Move English expression '" + englishExpression + "' from learned words to:",
                "Move to Vocabulary",
                JOptionPane.QUESTION_MESSAGE,
                null,
                targets.toArray(),
                targets.get(0));
        if (targetDatabase == null || targetDatabase.isEmpty()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Move '" + englishExpression + "' from learned words to '" + targetDatabase + "'?",
                "Confirm Move", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            boolean moved = gameController.moveExpression(LEARNED_WORDS_DB, targetDatabase, englishExpression);
            if (moved) {
                JOptionPane.showMessageDialog(this,
                        "Moved '" + englishExpression + "' to '" + targetDatabase + "'.",
                        "Move Successful", JOptionPane.INFORMATION_MESSAGE);
                refreshLearnedWordsTable();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not move '" + englishExpression + "'. It may no longer be in learned words.",
                        "Move Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Error moving learned word", e);
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteLearnedWord(String englishExpression) {
        if (englishExpression == null || englishExpression.isBlank()) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(this,
                "Delete learned word '" + englishExpression + "' permanently?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            boolean deleted = gameController.deleteExpression(LEARNED_WORDS_DB, englishExpression);
            if (deleted) {
                JOptionPane.showMessageDialog(this,
                        "Deleted '" + englishExpression + "' from learned words.",
                        "Delete Successful", JOptionPane.INFORMATION_MESSAGE);
                refreshLearnedWordsTable();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not delete '" + englishExpression + "'.",
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Error deleting learned word", e);
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    // Custom button editor for table cells
    class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private int editedColumn;
        private String englishExpression;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> {
                String expr = englishExpression;
                int col = editedColumn;
                fireEditingStopped();
                if (col == 5) {
                    log.info("Move learned word: {}", expr);
                    moveLearnedWordToPractice(expr);
                } else if (col == 6) {
                    log.info("Delete learned word: {}", expr);
                    deleteLearnedWord(expr);
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            editedColumn = column;
            englishExpression = (String) table.getValueAt(row, 0);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }
    }
}
