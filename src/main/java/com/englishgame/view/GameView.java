package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Interactive Game Window
 * Main game interface for learning English
 */
@Slf4j
public class GameView extends JFrame {

    private final GameController gameController;
    private final LandingPageView landingPage;

    // Game components
    private JComboBox<String> databaseSelector;
    private JLabel spanishExpressionLabel;
    private JTextField englishTranslationField;
    private JButton submitButton;
    private JButton newRoundButton;
    private JLabel feedbackLabel;
    private JLabel scoreLabel;
    private JLabel progressLabel;

    // Navigation components
    private JButton backToLandingButton;
    private JButton dataManagementButton;
    private JButton viewWordsButton;
    private JButton learnedWordsButton;

    // Game state
    private SpanishExpression currentSpanishExpression;
    private int correctAnswers = 0;
    private int totalAnswers = 0;

    public GameView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("Interactive Game - English Learning Game");
        setSize(1000, 800);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        
        initComponents();
        setupLayout();
        addListeners();
        refreshDatabaseSelector();
        
        log.info("Game window initialized");
    }

    private void initComponents() {
        // Database selector
        databaseSelector = new JComboBox<>();
        databaseSelector.setPreferredSize(new Dimension(200, 30));
        
        // Game area
        spanishExpressionLabel = new JLabel("Select a database and start a new round!", SwingConstants.CENTER);
        spanishExpressionLabel.setFont(new Font("Arial", Font.BOLD, 24));
        spanishExpressionLabel.setForeground(new Color(0, 102, 204));
        
        englishTranslationField = new JTextField(20);
        englishTranslationField.setFont(new Font("Arial", Font.PLAIN, 16));
        englishTranslationField.setToolTipText("Enter your English translation here");
        
        submitButton = createStyledButton("Submit Answer", "Submit your translation");
        newRoundButton = createStyledButton("New Round", "Get a new Spanish expression");
        
        feedbackLabel = new JLabel("", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        
        scoreLabel = new JLabel("Score: 0/0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        scoreLabel.setForeground(new Color(0, 150, 0));
        
        progressLabel = new JLabel("Progress: 0%", SwingConstants.CENTER);
        progressLabel.setFont(new Font("Arial", Font.BOLD, 14));
        progressLabel.setForeground(new Color(255, 140, 0));
        
        // Navigation buttons
        backToLandingButton = createStyledButton("Back to Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("Manage Data", "Go to data management");
        viewWordsButton = createStyledButton("View Words", "View saved words");
        learnedWordsButton = createStyledButton("Learned Words", "View learned words");
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
        if (buttonText.contains("Submit") || buttonText.contains("Answer")) {
            return new Color(16, 185, 129); // Vibrant green for submit actions
        } else if (buttonText.contains("New") || buttonText.contains("Round")) {
            return new Color(59, 130, 246); // Vibrant blue for new actions
        } else if (buttonText.contains("Manage") || buttonText.contains("Data")) {
            return new Color(37, 99, 235); // Vibrant blue for data management
        } else if (buttonText.contains("View") || buttonText.contains("Words")) {
            return new Color(16, 185, 129); // Vibrant green for view actions
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
        dbSection.add(dbPanel);
        
        // Game Area Section
        JPanel gameSection = createSectionPanel("Interactive Game");
        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Spanish expression
        spanishExpressionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gamePanel.add(spanishExpressionLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // English input
        englishTranslationField.setAlignmentX(Component.CENTER_ALIGNMENT);
        englishTranslationField.setMaximumSize(new Dimension(300, 35));
        gamePanel.add(englishTranslationField);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Game buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(submitButton);
        buttonPanel.add(newRoundButton);
        gamePanel.add(buttonPanel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Feedback
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gamePanel.add(feedbackLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Score and progress
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(scoreLabel);
        statsPanel.add(progressLabel);
        gamePanel.add(statsPanel);
        
        gameSection.add(gamePanel);
        
        // Navigation Section
        JPanel navSection = createSectionPanel("Navigation");
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        navPanel.add(dataManagementButton);
        navPanel.add(viewWordsButton);
        navPanel.add(learnedWordsButton);
        navPanel.add(backToLandingButton);
        navSection.add(navPanel);
        
        // Add all sections to main panel
        mainPanel.add(dbSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(gameSection);
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
        databaseSelector.addActionListener(e -> {
            String selectedDb = (String) databaseSelector.getSelectedItem();
            if (selectedDb != null) {
                gameController.selectDatabase(selectedDb);
                resetGameDisplay();
                log.info("Database selected: {}", selectedDb);
            }
        });
        
        // Game buttons
        newRoundButton.addActionListener(e -> startNewRound());
        submitButton.addActionListener(e -> processAnswer());
        
        // Enter key for answer submission
        englishTranslationField.addActionListener(e -> processAnswer());
        
        // Navigation buttons
        backToLandingButton.addActionListener(e -> returnToLanding());
        dataManagementButton.addActionListener(e -> openDataManagement());
        viewWordsButton.addActionListener(e -> openViewWords());
        learnedWordsButton.addActionListener(e -> openLearnedWords());
    }

    private void refreshDatabaseSelector() {
        databaseSelector.removeAllItems();
        gameController.getAvailableDatabases().forEach(databaseSelector::addItem);
        log.debug("Database selector refreshed with {} databases", databaseSelector.getItemCount());
    }

    private void startNewRound() {
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        currentSpanishExpression = gameController.startNewRound();
        if (currentSpanishExpression != null) {
            spanishExpressionLabel.setText("Translate: \"" + currentSpanishExpression.getExpression() + "\"");
            englishTranslationField.setText("");
            feedbackLabel.setText("");
            englishTranslationField.requestFocus();
            log.info("New round started with Spanish expression: '{}'", currentSpanishExpression.getExpression());
        } else {
            spanishExpressionLabel.setText("No expressions available. Add some words or select another database.");
            feedbackLabel.setText("Please add expressions to this database or select another one.");
        }
    }

    private void processAnswer() {
        if (currentSpanishExpression == null) {
            JOptionPane.showMessageDialog(this, "Please start a new round first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String userTranslation = englishTranslationField.getText().trim();
        if (userTranslation.isEmpty()) {
            feedbackLabel.setText("Please enter a translation.");
            feedbackLabel.setForeground(Color.RED);
            return;
        }
        
        boolean isCorrect = gameController.processAnswer(userTranslation);
        totalAnswers++;
        
        if (isCorrect) {
            correctAnswers++;
            feedbackLabel.setText("✅ Correct! Well done!");
            feedbackLabel.setForeground(new Color(0, 150, 0));
            log.info("Correct answer: '{}' for '{}'", userTranslation, currentSpanishExpression.getExpression());
        } else {
            feedbackLabel.setText("❌ Incorrect. Try again or start a new round.");
            feedbackLabel.setForeground(Color.RED);
            log.info("Incorrect answer: '{}' for '{}'", userTranslation, currentSpanishExpression.getExpression());
        }
        
        updateStats();
        englishTranslationField.setText("");
    }

    private void updateStats() {
        scoreLabel.setText("Score: " + correctAnswers + "/" + totalAnswers);
        int progress = totalAnswers > 0 ? (correctAnswers * 100) / totalAnswers : 0;
        progressLabel.setText("Progress: " + progress + "%");
        
        // Color coding for progress
        if (progress >= 80) {
            progressLabel.setForeground(new Color(0, 150, 0)); // Green
        } else if (progress >= 60) {
            progressLabel.setForeground(new Color(255, 140, 0)); // Orange
        } else {
            progressLabel.setForeground(Color.RED); // Red
        }
    }

    private void resetGameDisplay() {
        spanishExpressionLabel.setText("Select a database and start a new round!");
        englishTranslationField.setText("");
        feedbackLabel.setText("");
        currentSpanishExpression = null;
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
