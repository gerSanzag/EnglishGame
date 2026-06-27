package com.englishgame.view;

import com.englishgame.UiText;
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
    private JButton moveSelectedButton;
    private JButton deleteSelectedButton;
    private JButton selectAllRowsButton;
    private JButton clearSelectionButton;
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

    private String ui(String en, String es) {
        return UiText.t(gameController.getAppGameMode(), en, es);
    }

    private void initComponents() {
        // Learned words table
        String[] columnNames = {"Expression", "Translation", "Score",
                ui("Added", "Inclusión"), ui("Source database", "BBDD origen"), "Move", "Delete"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5 || column == 6;
            }
        };
        learnedWordsTable = new JTable(tableModel);
        learnedWordsTable.setRowHeight(35);
        learnedWordsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        learnedWordsTable.setToolTipText(ui(
                "Hold Ctrl/Cmd or Shift to select multiple rows; use the bulk buttons.",
                "Mantén Ctrl/Cmd o Shift para seleccionar varias filas; usa los botones de grupo."));
        
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

        String promptSort = gameController.getAppGameMode().getBrowseSortPromptLabel() + " (A-Z)";
        sortSelector = new JComboBox<>(new String[] {
                promptSort,
                ui("English (A-Z)", "Inglés (A-Z)"),
                ui("Score (high to low)", "Score (mayor a menor)"),
                ui("Inclusion (newest first)", "Inclusión (reciente primero)"),
                ui("Inclusion (oldest first)", "Inclusión (antigua primero)"),
                ui("Source database (A-Z)", "BBDD origen (A-Z)")
        });
        sortSelector.setPreferredSize(new Dimension(300, 30));
        sortSelector.setToolTipText(ui(
                "Sort by prompt, English, score, inclusion date, or practice source database",
                "Ordena por prompt, inglés, score, inclusión o base de práctica de origen"));

        recordsCountLabel = new JLabel(ui("Records: 0", "Registros: 0"));
        recordsCountLabel.setFont(new Font("Arial", Font.BOLD, 13));
        recordsCountLabel.setForeground(new Color(70, 70, 80));
        
        // Buttons
        refreshButton = createStyledButton("Refresh", "Refresh the learned words list");
        deleteAllButton = createStyledButton("Delete All", "Delete all learned words");
        moveSelectedButton = createStyledButton(ui("Move selected", "Mover seleccionados"),
                ui("Move selected rows to a practice database",
                        "Mueve las filas seleccionadas a una base de práctica"));
        deleteSelectedButton = createStyledButton(ui("Delete selected", "Borrar seleccionados"),
                ui("Delete selected rows from Learned Words",
                        "Elimina las filas seleccionadas de Learned Words"));
        selectAllRowsButton = createStyledButton(ui("Select all", "Seleccionar todo"),
                ui("Select all visible rows in the table",
                        "Selecciona todas las filas visibles en la tabla"));
        clearSelectionButton = createStyledButton(ui("Clear selection", "Quitar selección"),
                ui("Deselect all rows", "Deselecciona todas las filas"));
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
        } else if (buttonText.contains("Delete selected") || buttonText.contains("Borrar seleccionados")) {
            return new Color(220, 38, 127);
        } else if (buttonText.contains("Move selected") || buttonText.contains("Mover seleccionados")) {
            return new Color(245, 158, 11);
        } else if (buttonText.contains("Select all") || buttonText.contains("Seleccionar")
                || buttonText.contains("Clear selection") || buttonText.contains("Quitar selección")) {
            return new Color(100, 116, 139);
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
        searchPanel.add(new JLabel(ui("Sort:", "Orden:")));
        searchPanel.add(sortSelector);
        searchPanel.add(Box.createHorizontalStrut(14));
        searchPanel.add(recordsCountLabel);
        searchSection.add(searchPanel);
        
        // Learned Words Table Section
        JPanel tableSection = createSectionPanel("Learned Words and Expressions");
        JPanel bulkSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        bulkSelectionPanel.add(moveSelectedButton);
        bulkSelectionPanel.add(deleteSelectedButton);
        bulkSelectionPanel.add(selectAllRowsButton);
        bulkSelectionPanel.add(clearSelectionButton);
        tableSection.add(bulkSelectionPanel);
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
        moveSelectedButton.addActionListener(e -> moveSelectedLearnedWords());
        deleteSelectedButton.addActionListener(e -> deleteSelectedLearnedWords());
        selectAllRowsButton.addActionListener(e -> selectAllVisibleRows());
        clearSelectionButton.addActionListener(e -> learnedWordsTable.clearSelection());
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
            JOptionPane.showMessageDialog(this,
                    ui("Error loading learned words: ", "Error al cargar palabras aprendidas: ") + e.getMessage(),
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

        recordsCountLabel.setText(ui("Records: ", "Registros: ") + filtered.size());
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
        if (selected.startsWith("Ingl") || selected.startsWith("English")) {
            return SortMode.ENGLISH_AZ;
        }
        if (selected.startsWith("Score")) {
            return SortMode.SCORE_DESC;
        }
        if (selected.startsWith("Inclusión") || selected.startsWith("Inclusion")) {
            if (selected.contains("reciente") || selected.contains("newest")) {
                return SortMode.INCLUSION_DESC;
            }
            return SortMode.INCLUSION_ASC;
        }
        if (selected.startsWith("BBDD origen") || selected.startsWith("Source database")) {
            return SortMode.SOURCE_DB_AZ;
        }
        return SortMode.SPANISH_AZ;
    }

    private void deleteAllLearnedWords() {
        int result = JOptionPane.showConfirmDialog(this,
            ui("Are you sure you want to delete ALL learned words?\n\nThis action cannot be undone!",
                    "¿Seguro de borrar TODAS las palabras aprendidas?\n\nEsta acción no se puede deshacer."),
            ui("Confirm Delete All", "Confirmar borrar todo"),
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                log.info("Delete all learned words requested");
                boolean deleted = gameController.deleteAllExpressions(LEARNED_WORDS_DB);
                if (deleted) {
                    JOptionPane.showMessageDialog(this,
                            ui("All learned words were deleted.",
                                    "Se eliminaron todas las palabras aprendidas."),
                            ui("Delete completed", "Borrado completado"),
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            ui("There were no learned words to delete.",
                                    "No había palabras aprendidas que borrar."),
                            ui("No data", "Sin datos"), JOptionPane.INFORMATION_MESSAGE);
                }
                refreshLearnedWordsTable();
            } catch (Exception e) {
                log.error("Error deleting all learned words", e);
                JOptionPane.showMessageDialog(this,
                        ui("Error deleting learned words: ", "Error al borrar palabras aprendidas: ")
                                + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reviewLearnedWords() {
        log.info("Review learned words requested");
        if (!gameController.hasAnyReviewContent()) {
            JOptionPane.showMessageDialog(this,
                    ui("No expressions in Learned words or Words definitely learned to review.",
                            "No hay expresiones en Learned words ni en Words definitely learned para repasar."),
                    "Review", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        setVisible(false);
        LearnedWordsReviewView review = new LearnedWordsReviewView(gameController, this, landingPage, () -> {
            LearnedWordsView.this.setVisible(true);
            refreshLearnedWordsTable();
        });
        review.setVisible(true);
    }

    private List<String> selectedExpressionsFromTable() {
        int[] rows = learnedWordsTable.getSelectedRows();
        List<String> expressions = new ArrayList<>();
        for (int row : rows) {
            Object value = learnedWordsTable.getValueAt(row, 0);
            if (value == null) {
                continue;
            }
            String expr = value.toString().trim();
            if (!expr.isEmpty() && !expressions.contains(expr)) {
                expressions.add(expr);
            }
        }
        return expressions;
    }

    private void selectAllVisibleRows() {
        int count = learnedWordsTable.getRowCount();
        if (count == 0) {
            return;
        }
        learnedWordsTable.clearSelection();
        learnedWordsTable.addRowSelectionInterval(0, count - 1);
    }

    private void moveSelectedLearnedWords() {
        List<String> expressions = selectedExpressionsFromTable();
        if (expressions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    ui("Select one or more rows in the table (Ctrl/Cmd or Shift + click).",
                            "Selecciona una o más filas en la tabla (Ctrl/Cmd o Shift + clic)."),
                    ui("No selection", "Sin selección"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> targets = new ArrayList<>(gameController.getMoveTargetsFromLearnedWordsDatabase());
        if (targets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    ui("No vocabulary database is available as a destination.",
                            "No hay ninguna base de vocabulario disponible como destino."),
                    ui("No destination", "Sin destino"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String targetDatabase = (String) JOptionPane.showInputDialog(
                this,
                ui("Destination for " + expressions.size() + " expression(s):",
                        "Destino para " + expressions.size() + " expresión(es):"),
                ui("Move selected", "Mover seleccionados"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                targets.toArray(),
                targets.get(0));
        if (targetDatabase == null || targetDatabase.isBlank()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                ui("Move " + expressions.size() + " expression(s) from Learned Words to \""
                                + targetDatabase + "\"?",
                        "¿Mover " + expressions.size() + " expresión(es) de Learned Words a \""
                                + targetDatabase + "\"?"),
                ui("Confirm bulk move", "Confirmar movimiento en grupo"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        int moved = 0;
        int failed = 0;
        for (String expression : expressions) {
            try {
                if (gameController.moveExpression(LEARNED_WORDS_DB, targetDatabase, expression)) {
                    moved++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.error("Error moving learned word '{}' in bulk", expression, e);
            }
        }
        refreshLearnedWordsTable();
        JOptionPane.showMessageDialog(this,
                ui("Moved: ", "Movidas: ") + moved
                        + (failed > 0 ? "\n" + ui("Not moved: ", "No movidas: ") + failed : ""),
                ui("Bulk move", "Movimiento en grupo"),
                failed > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedLearnedWords() {
        List<String> expressions = selectedExpressionsFromTable();
        if (expressions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    ui("Select one or more rows in the table (Ctrl/Cmd or Shift + click).",
                            "Selecciona una o más filas en la tabla (Ctrl/Cmd o Shift + clic)."),
                    ui("No selection", "Sin selección"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                ui("Delete " + expressions.size() + " expression(s) from Learned Words permanently?",
                        "¿Borrar " + expressions.size() + " expresión(es) de Learned Words permanentemente?"),
                ui("Confirm bulk delete", "Confirmar borrado en grupo"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        int deleted = 0;
        int failed = 0;
        for (String expression : expressions) {
            try {
                if (gameController.deleteExpression(LEARNED_WORDS_DB, expression)) {
                    deleted++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.error("Error deleting learned word '{}' in bulk", expression, e);
            }
        }
        refreshLearnedWordsTable();
        JOptionPane.showMessageDialog(this,
                ui("Deleted: ", "Borradas: ") + deleted
                        + (failed > 0 ? "\n" + ui("Not deleted: ", "No borradas: ") + failed : ""),
                ui("Bulk delete", "Borrado en grupo"),
                failed > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    private void moveLearnedWordToPractice(String englishExpression) {
        if (englishExpression == null || englishExpression.isBlank()) {
            return;
        }
        List<String> targets = new ArrayList<>(gameController.getMoveTargetsFromLearnedWordsDatabase());
        if (targets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    ui("No other vocabulary database is available to move this word into.",
                            "No hay otra base de vocabulario disponible como destino."),
                    ui("No Target Database", "Sin base de destino"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String targetDatabase = (String) JOptionPane.showInputDialog(
                this,
                ui("Move English expression '" + englishExpression + "' from learned words to:",
                        "Mover la expresión '" + englishExpression + "' desde learned words a:"),
                ui("Move to Vocabulary", "Mover a vocabulario"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                targets.toArray(),
                targets.get(0));
        if (targetDatabase == null || targetDatabase.isEmpty()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                ui("Move '" + englishExpression + "' from learned words to '" + targetDatabase + "'?",
                        "¿Mover '" + englishExpression + "' de learned words a '" + targetDatabase + "'?"),
                ui("Confirm Move", "Confirmar movimiento"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            boolean moved = gameController.moveExpression(LEARNED_WORDS_DB, targetDatabase, englishExpression);
            if (moved) {
                JOptionPane.showMessageDialog(this,
                        ui("Moved '" + englishExpression + "' to '" + targetDatabase + "'.",
                                "Movida '" + englishExpression + "' a '" + targetDatabase + "'."),
                        ui("Move Successful", "Movimiento correcto"),
                        JOptionPane.INFORMATION_MESSAGE);
                refreshLearnedWordsTable();
            } else {
                JOptionPane.showMessageDialog(this,
                        ui("Could not move '" + englishExpression
                                        + "'. It may no longer be in learned words.",
                                "No se pudo mover '" + englishExpression
                                        + "'. Puede que ya no esté en learned words."),
                        ui("Move Failed", "Movimiento fallido"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Error moving learned word", e);
            JOptionPane.showMessageDialog(this,
                    ui("Error: ", "Error: ") + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteLearnedWord(String englishExpression) {
        if (englishExpression == null || englishExpression.isBlank()) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(this,
                ui("Delete learned word '" + englishExpression + "' permanently?",
                        "¿Borrar permanentemente la palabra aprendida '" + englishExpression + "'?"),
                ui("Confirm Delete", "Confirmar borrado"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            boolean deleted = gameController.deleteExpression(LEARNED_WORDS_DB, englishExpression);
            if (deleted) {
                JOptionPane.showMessageDialog(this,
                        ui("Deleted '" + englishExpression + "' from learned words.",
                                "Borrada '" + englishExpression + "' de learned words."),
                        ui("Delete Successful", "Borrado correcto"),
                        JOptionPane.INFORMATION_MESSAGE);
                refreshLearnedWordsTable();
            } else {
                JOptionPane.showMessageDialog(this,
                        ui("Could not delete '" + englishExpression + "'.",
                                "No se pudo borrar '" + englishExpression + "'."),
                        ui("Delete Failed", "Borrado fallido"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Error deleting learned word", e);
            JOptionPane.showMessageDialog(this,
                    ui("Error: ", "Error: ") + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
