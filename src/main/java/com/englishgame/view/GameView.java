package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
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
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
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
        backToLandingButton = createStyledButton("🏠 Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("📊 Manage Data", "Go to data management");
        viewWordsButton = createStyledButton("📚 View Words", "View saved words");
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
        
        // Top panel with database selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(new JLabel("Database:"));
        topPanel.add(databaseSelector);
        
        // Center panel with game area
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        centerPanel.setBackground(new Color(248, 249, 250));
        
        // Spanish expression
        spanishExpressionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(spanishExpressionLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // English input
        englishTranslationField.setAlignmentX(Component.CENTER_ALIGNMENT);
        englishTranslationField.setMaximumSize(new Dimension(300, 35));
        centerPanel.add(englishTranslationField);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(submitButton);
        buttonPanel.add(newRoundButton);
        centerPanel.add(buttonPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Feedback
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(feedbackLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Score and progress
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(scoreLabel);
        statsPanel.add(progressLabel);
        centerPanel.add(statsPanel);
        
        // Bottom panel with navigation
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(dataManagementButton);
        bottomPanel.add(viewWordsButton);
        bottomPanel.add(learnedWordsButton);
        bottomPanel.add(backToLandingButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
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
