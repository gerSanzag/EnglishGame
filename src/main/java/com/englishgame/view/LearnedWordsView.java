package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.EnglishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Learned Words Window
 * Displays all learned words and expressions
 */
@Slf4j
public class LearnedWordsView extends JFrame {

    private final GameController gameController;
    private final LandingPageView landingPage;

    // Components
    private JTable learnedWordsTable;
    private JLabel statsLabel;
    private JButton refreshButton;
    private JButton backToLandingButton;
    private JButton dataManagementButton;
    private JButton viewWordsButton;
    private JButton playGameButton;

    public LearnedWordsView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("Learned Words - English Learning Game");
        setSize(800, 600);
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
        String[] columnNames = {"English Expression", "Score", "Spanish Translations", "Date Learned"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        learnedWordsTable = new JTable(tableModel);
        learnedWordsTable.setRowHeight(25);
        learnedWordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Stats label
        statsLabel = new JLabel("Loading learned words...", SwingConstants.CENTER);
        statsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statsLabel.setForeground(new Color(0, 150, 0));
        
        // Buttons
        refreshButton = createStyledButton("🔄 Refresh", "Refresh the learned words list");
        backToLandingButton = createStyledButton("🏠 Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("📊 Manage Data", "Go to data management");
        viewWordsButton = createStyledButton("📚 View Words", "View all saved words");
        playGameButton = createStyledButton("🎮 Play Game", "Start interactive game");
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
        
        // Top panel with stats and refresh button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topLeftPanel.add(statsLabel);
        
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.add(refreshButton);
        
        topPanel.add(topLeftPanel, BorderLayout.WEST);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        
        // Center panel with table
        JScrollPane tableScrollPane = new JScrollPane(learnedWordsTable);
        tableScrollPane.setPreferredSize(new Dimension(750, 400));
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Bottom panel with navigation buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(dataManagementButton);
        bottomPanel.add(viewWordsButton);
        bottomPanel.add(playGameButton);
        bottomPanel.add(backToLandingButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addListeners() {
        // Buttons
        refreshButton.addActionListener(e -> refreshLearnedWordsTable());
        backToLandingButton.addActionListener(e -> returnToLanding());
        dataManagementButton.addActionListener(e -> openDataManagement());
        viewWordsButton.addActionListener(e -> openViewWords());
        playGameButton.addActionListener(e -> openGame());
    }

    private void refreshLearnedWordsTable() {
        DefaultTableModel model = (DefaultTableModel) learnedWordsTable.getModel();
        model.setRowCount(0); // Clear existing data
        
        try {
            // Get learned words from the learned_words database
            List<EnglishExpression> learnedWords = gameController.getEnglishExpressionsFromDatabase("learned_words");
            
            for (EnglishExpression learnedWord : learnedWords) {
                String spanishTranslations = learnedWord.getTranslations().stream()
                    .map(spanish -> spanish.getExpression())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");
                
                // For now, we'll use a placeholder for the date learned
                // In a real implementation, you'd track when words were learned
                String dateLearned = "Recently"; // TODO: Implement actual date tracking
                
                model.addRow(new Object[]{
                    learnedWord.getExpression(),
                    learnedWord.getScore(),
                    spanishTranslations,
                    dateLearned
                });
            }
            
            // Update stats
            int totalLearned = learnedWords.size();
            statsLabel.setText("🏆 Total Learned Words: " + totalLearned);
            
            if (totalLearned == 0) {
                statsLabel.setText("📚 No learned words yet. Keep playing to learn new words!");
                statsLabel.setForeground(new Color(255, 140, 0));
            } else {
                statsLabel.setForeground(new Color(0, 150, 0));
            }
            
            log.info("Learned words table refreshed with {} words", totalLearned);
            
        } catch (Exception e) {
            log.error("Error refreshing learned words table", e);
            JOptionPane.showMessageDialog(this, "Error loading learned words: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            statsLabel.setText("❌ Error loading learned words");
            statsLabel.setForeground(Color.RED);
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
}
