package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * View Words Window
 * Displays all saved words and expressions
 */
@Slf4j
public class ViewWordsView extends JFrame {

    private final GameController gameController;
    private final LandingPageView landingPage;

    // Components
    private JComboBox<String> databaseSelector;
    private JTable wordsTable;
    private JButton refreshButton;
    private JButton backToLandingButton;
    private JButton dataManagementButton;
    private JButton playGameButton;
    private JButton learnedWordsButton;

    public ViewWordsView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("View Words - English Learning Game");
        setSize(900, 600);
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
        String[] columnNames = {"Type", "Expression", "Score", "Translations"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        wordsTable = new JTable(tableModel);
        wordsTable.setRowHeight(25);
        wordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Buttons
        refreshButton = createStyledButton("🔄 Refresh", "Refresh the words list");
        backToLandingButton = createStyledButton("🏠 Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("📊 Manage Data", "Go to data management");
        playGameButton = createStyledButton("🎮 Play Game", "Start interactive game");
        learnedWordsButton = createStyledButton("🏆 Learned Words", "View learned words");
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setToolTipText(tooltip);
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(100, 149, 237));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(70, 130, 180));
            }
        });
        
        return button;
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Top panel with database selector and refresh button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(new JLabel("Database:"));
        topPanel.add(databaseSelector);
        topPanel.add(refreshButton);
        
        // Center panel with table
        JScrollPane tableScrollPane = new JScrollPane(wordsTable);
        tableScrollPane.setPreferredSize(new Dimension(800, 400));
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Bottom panel with navigation buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(dataManagementButton);
        bottomPanel.add(playGameButton);
        bottomPanel.add(learnedWordsButton);
        bottomPanel.add(backToLandingButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addListeners() {
        // Database selection
        databaseSelector.addActionListener(e -> refreshWordsTable());
        
        // Buttons
        refreshButton.addActionListener(e -> refreshWordsTable());
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
        
        try {
            // Get Spanish expressions
            List<SpanishExpression> spanishExpressions = gameController.getSpanishExpressionsFromDatabase(selectedDb);
            for (SpanishExpression spanish : spanishExpressions) {
                String translations = spanish.getTranslations().stream()
                    .map(EnglishExpression::getExpression)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");
                
                model.addRow(new Object[]{
                    "Spanish",
                    spanish.getExpression(),
                    spanish.getScore(),
                    translations
                });
            }
            
            // Get English expressions
            List<EnglishExpression> englishExpressions = gameController.getEnglishExpressionsFromDatabase(selectedDb);
            for (EnglishExpression english : englishExpressions) {
                String translations = english.getTranslations().stream()
                    .map(SpanishExpression::getExpression)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");
                
                model.addRow(new Object[]{
                    "English",
                    english.getExpression(),
                    english.getScore(),
                    translations
                });
            }
            
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
}
