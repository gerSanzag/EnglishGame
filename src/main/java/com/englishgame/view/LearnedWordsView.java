package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.EnglishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
        refreshButton = createStyledButton("Refresh", "Refresh the learned words list");
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
        
        // Statistics Section
        JPanel statsSection = createSectionPanel("Statistics");
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        statsPanel.add(statsLabel);
        statsPanel.add(Box.createHorizontalStrut(20));
        statsPanel.add(refreshButton);
        statsSection.add(statsPanel);
        
        // Learned Words Table Section
        JPanel tableSection = createSectionPanel("Learned Words and Expressions");
        JScrollPane tableScrollPane = new JScrollPane(learnedWordsTable);
        tableScrollPane.setPreferredSize(new Dimension(900, 400));
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableSection.add(tableScrollPane);
        
        // Navigation Section
        JPanel navSection = createSectionPanel("Navigation");
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        navPanel.add(dataManagementButton);
        navPanel.add(viewWordsButton);
        navPanel.add(playGameButton);
        navPanel.add(backToLandingButton);
        navSection.add(navPanel);
        
        // Add all sections to main panel
        mainPanel.add(statsSection);
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
