package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;

/**
 * Main game view using Swing
 * Provides a modern, functional interface for the English learning game
 */
@Slf4j
public class MainGameView extends JFrame {
    
    private final GameController gameController;
    
    // Main components
    private JPanel mainPanel;
    private JComboBox<String> databaseComboBox;
    private JButton selectDatabaseButton;
    private JButton newDatabaseButton;
    private JButton startGameButton;
    private JButton saveGameButton;
    private JButton loadGameButton;
    
    // Game components
    private JLabel currentDatabaseLabel;
    private JLabel spanishExpressionLabel;
    private JTextField answerTextField;
    private JButton submitAnswerButton;
    private JLabel resultLabel;
    private JLabel scoreLabel;
    
    // Database management
    private JTextField newDatabaseTextField;
    private JButton createDatabaseButton;
    
    public MainGameView(GameController gameController) {
        this.gameController = gameController;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateUI();
        
        setTitle("English Learning Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(true);
    }
    
    private void initializeComponents() {
        // Main panel with modern styling
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 245));
        
        // Database selection components
        databaseComboBox = new JComboBox<>();
        databaseComboBox.setPreferredSize(new Dimension(200, 30));
        databaseComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        
        selectDatabaseButton = createStyledButton("Select Database", new Color(52, 152, 219));
        newDatabaseButton = createStyledButton("New Database", new Color(46, 204, 113));
        startGameButton = createStyledButton("Start Game", new Color(155, 89, 182));
        saveGameButton = createStyledButton("Save Game", new Color(241, 196, 15));
        loadGameButton = createStyledButton("Load Game", new Color(230, 126, 34));
        
        // Game components
        currentDatabaseLabel = createStyledLabel("No database selected", new Color(52, 73, 94));
        spanishExpressionLabel = createStyledLabel("", new Color(44, 62, 80));
        spanishExpressionLabel.setFont(new Font("Arial", Font.BOLD, 24));
        spanishExpressionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        answerTextField = new JTextField();
        answerTextField.setPreferredSize(new Dimension(300, 40));
        answerTextField.setFont(new Font("Arial", Font.PLAIN, 16));
        answerTextField.setHorizontalAlignment(SwingConstants.CENTER);
        
        submitAnswerButton = createStyledButton("Submit Answer", new Color(231, 76, 60));
        resultLabel = createStyledLabel("", new Color(39, 174, 96));
        scoreLabel = createStyledLabel("", new Color(52, 73, 94));
        
        // Database management
        newDatabaseTextField = new JTextField();
        newDatabaseTextField.setPreferredSize(new Dimension(200, 30));
        newDatabaseTextField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        createDatabaseButton = createStyledButton("Create", new Color(46, 204, 113));
    }
    
    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(120, 35));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });
        
        return button;
    }
    
    private JLabel createStyledLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        return label;
    }
    
    private void setupLayout() {
        // Top panel - Database management
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Center panel - Game area
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel - Controls
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 2),
            "Database Management",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(52, 73, 94)
        ));
        
        panel.add(new JLabel("Database:"));
        panel.add(databaseComboBox);
        panel.add(selectDatabaseButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(newDatabaseButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(saveGameButton);
        panel.add(loadGameButton);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 2),
            "Game Area",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(52, 73, 94)
        ));
        
        // Current database info
        JPanel infoPanel = new JPanel(new FlowLayout());
        infoPanel.setBackground(new Color(245, 245, 245));
        infoPanel.add(currentDatabaseLabel);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Game area
        JPanel gamePanel = new JPanel(new GridBagLayout());
        gamePanel.setBackground(new Color(245, 245, 245));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Spanish expression
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gamePanel.add(spanishExpressionLabel, gbc);
        
        // Answer input
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        gamePanel.add(new JLabel("Your answer:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gamePanel.add(answerTextField, gbc);
        
        // Submit button
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gamePanel.add(submitAnswerButton, gbc);
        
        // Result and score
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gamePanel.add(resultLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gamePanel.add(scoreLabel, gbc);
        
        panel.add(gamePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 2),
            "Game Controls",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(52, 73, 94)
        ));
        
        panel.add(startGameButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(new JLabel("New Database Name:"));
        panel.add(newDatabaseTextField);
        panel.add(createDatabaseButton);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Database selection
        selectDatabaseButton.addActionListener(this::handleDatabaseSelection);
        newDatabaseButton.addActionListener(this::handleNewDatabaseDialog);
        createDatabaseButton.addActionListener(this::handleCreateDatabase);
        
        // Game controls
        startGameButton.addActionListener(this::handleStartGame);
        submitAnswerButton.addActionListener(this::handleSubmitAnswer);
        
        // Save/Load
        saveGameButton.addActionListener(this::handleSaveGame);
        loadGameButton.addActionListener(this::handleLoadGame);
        
        // Enter key in answer field
        answerTextField.addActionListener(this::handleSubmitAnswer);
    }
    
    private void handleDatabaseSelection(ActionEvent e) {
        Optional.ofNullable((String) databaseComboBox.getSelectedItem())
                .ifPresentOrElse(
                    databaseName -> {
                        if (gameController.selectDatabase(databaseName)) {
                            updateUI();
                            showMessage("Database selected: " + databaseName, "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showMessage("Failed to select database: " + databaseName, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    },
                    () -> showMessage("Please select a database", "Warning", JOptionPane.WARNING_MESSAGE)
                );
    }
    
    private void handleNewDatabaseDialog(ActionEvent e) {
        String databaseName = JOptionPane.showInputDialog(this, "Enter new database name:", "New Database", JOptionPane.QUESTION_MESSAGE);
        Optional.ofNullable(databaseName)
                .filter(name -> !name.trim().isEmpty())
                .ifPresentOrElse(
                    name -> {
                        if (gameController.createNewDatabase(name.trim())) {
                            updateUI();
                            showMessage("Database created: " + name, "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showMessage("Failed to create database: " + name, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    },
                    () -> showMessage("Database name cannot be empty", "Warning", JOptionPane.WARNING_MESSAGE)
                );
    }
    
    private void handleCreateDatabase(ActionEvent e) {
        Optional.ofNullable(newDatabaseTextField.getText())
                .filter(name -> !name.trim().isEmpty())
                .ifPresentOrElse(
                    name -> {
                        if (gameController.createNewDatabase(name.trim())) {
                            newDatabaseTextField.setText("");
                            updateUI();
                            showMessage("Database created: " + name, "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showMessage("Failed to create database: " + name, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    },
                    () -> showMessage("Database name cannot be empty", "Warning", JOptionPane.WARNING_MESSAGE)
                );
    }
    
    private void handleStartGame(ActionEvent e) {
        Optional.ofNullable(gameController.getCurrentDatabase())
                .ifPresentOrElse(
                    databaseName -> {
                        SpanishExpression expression = gameController.startNewRound();
                        if (expression != null) {
                            spanishExpressionLabel.setText("Translate: \"" + expression.getExpression() + "\"");
                            answerTextField.setText("");
                            answerTextField.requestFocus();
                            resultLabel.setText("");
                            scoreLabel.setText("");
                            updateUI();
                        } else {
                            showMessage("No expressions available in database: " + databaseName, "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    },
                    () -> showMessage("Please select a database first", "Warning", JOptionPane.WARNING_MESSAGE)
                );
    }
    
    private void handleSubmitAnswer(ActionEvent e) {
        Optional.ofNullable(answerTextField.getText())
                .filter(answer -> !answer.trim().isEmpty())
                .ifPresentOrElse(
                    answer -> {
                        boolean isCorrect = gameController.processAnswer(answer.trim());
                        if (isCorrect) {
                            resultLabel.setText("✓ Correct! Well done!");
                            resultLabel.setForeground(new Color(39, 174, 96));
                        } else {
                            resultLabel.setText("✗ Incorrect. Try again!");
                            resultLabel.setForeground(new Color(231, 76, 60));
                        }
                        
                        // Update score display
                        updateScoreDisplay();
                        answerTextField.setText("");
                        answerTextField.requestFocus();
                    },
                    () -> showMessage("Please enter an answer", "Warning", JOptionPane.WARNING_MESSAGE)
                );
    }
    
    private void handleSaveGame(ActionEvent e) {
        gameController.saveGameState();
        showMessage("Game saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void handleLoadGame(ActionEvent e) {
        gameController.loadGameState();
        updateUI();
        showMessage("Game loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updateUI() {
        // Update database combo box
        databaseComboBox.removeAllItems();
        gameController.getAvailableDatabases().forEach(databaseComboBox::addItem);
        
        // Update current database label
        Optional.ofNullable(gameController.getCurrentDatabase())
                .ifPresentOrElse(
                    databaseName -> currentDatabaseLabel.setText("Current Database: " + databaseName),
                    () -> currentDatabaseLabel.setText("No database selected")
                );
        
        // Update game state
        updateScoreDisplay();
    }
    
    private void updateScoreDisplay() {
        Optional.ofNullable(gameController.getCurrentSpanishExpression())
                .ifPresent(expression -> {
                    String scoreText = String.format("Score: %d", expression.getScore());
                    scoreLabel.setText(scoreText);
                });
    }
    
    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
    
    public void showView() {
        setVisible(true);
        log.info("Main game view displayed");
    }
}
