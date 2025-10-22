package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Management Window
 * Handles database creation, deletion, and data entry
 */
@Slf4j
public class DataManagementView extends JFrame {

    private final GameController gameController;
    private final LandingPageView landingPage;

    // Database management components
    private JComboBox<String> databaseSelector;
    private JTextField newDatabaseField;
    private JButton createDatabaseButton;
    private JButton deleteDatabaseButton;

    // Individual entry components
    private JTextField spanishField;
    private JTextField englishField;
    private JButton addIndividualButton;

    // Bulk entry components
    private JTextArea bulkTextArea;
    private JButton pasteButton;
    private JButton loadFileButton;
    private JButton processBulkButton;

    // Navigation components
    private JButton viewWordsButton;
    private JButton playGameButton;
    private JButton learnedWordsButton;
    private JButton backToLandingButton;

    public DataManagementView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("Data Management - English Learning Game");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(900, 600));
        
        initComponents();
        setupLayout();
        addListeners();
        refreshDatabaseSelector();
        
        log.info("Data management window initialized");
    }

    private void initComponents() {
        // Database Management Section
        databaseSelector = new JComboBox<>();
        databaseSelector.setPreferredSize(new Dimension(200, 30));
        
        newDatabaseField = new JTextField(25);
        newDatabaseField.setToolTipText("Enter name for new database");
        
        createDatabaseButton = createStyledButton("Create Database", "Create a new database");
        deleteDatabaseButton = createStyledButton("Delete Database", "Delete selected database");
        
        // Individual Entry Section
        spanishField = new JTextField(25);
        spanishField.setToolTipText("Enter Spanish expression");
        
        englishField = new JTextField(25);
        englishField.setToolTipText("Enter English translation");
        
        addIndividualButton = createStyledButton("Add Expression", "Add individual expression pair");
        
        // Bulk Entry Section
        bulkTextArea = new JTextArea(10, 60);
        bulkTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bulkTextArea.setToolTipText("Enter expressions in format: Spanish - English\\nCasa - house\\nCasa - home");
        
        loadFileButton = createStyledButton("Load File", "Load expressions from file");
        processBulkButton = createStyledButton("Add Expressions", "Add all entered expressions to database");
        
        // Navigation buttons
        viewWordsButton = createStyledButton("View Words", "View saved words");
        playGameButton = createStyledButton("Play Game", "Start interactive game");
        learnedWordsButton = createStyledButton("Learned Words", "View learned words");
        backToLandingButton = createStyledButton("Back to Main Menu", "Return to main menu");
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
        if (buttonText.contains("Create") || buttonText.contains("Add") || buttonText.contains("Process")) {
            return new Color(16, 185, 129); // Vibrant green for creation actions
        } else if (buttonText.contains("Delete")) {
            return new Color(220, 38, 127); // Vibrant pink for deletion
        } else if (buttonText.contains("Paste") || buttonText.contains("Load")) {
            return new Color(59, 130, 246); // Vibrant blue for data operations
        } else if (buttonText.contains("View") || buttonText.contains("Play")) {
            return new Color(245, 101, 101); // Vibrant coral for navigation
        } else if (buttonText.contains("Back")) {
            return new Color(124, 58, 237); // Vibrant purple for return actions
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
        
        // Database Management Section
        JPanel dbSection = createSectionPanel("Database Management");
        JPanel dbTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dbTopPanel.add(new JLabel("Current Database:"));
        dbTopPanel.add(databaseSelector);
        dbTopPanel.add(Box.createHorizontalStrut(10));
        dbTopPanel.add(createDatabaseButton);
        dbTopPanel.add(deleteDatabaseButton);
        
        JPanel dbBottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dbBottomPanel.add(new JLabel("New Database Name:"));
        dbBottomPanel.add(newDatabaseField);
        
        dbSection.add(dbTopPanel);
        dbSection.add(dbBottomPanel);
        
        // Individual Entry Section
        JPanel individualSection = createSectionPanel("Individual Expression Entry");
        JPanel individualPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        individualPanel.add(new JLabel("Spanish:"));
        individualPanel.add(spanishField);
        individualPanel.add(Box.createHorizontalStrut(10));
        individualPanel.add(new JLabel("English:"));
        individualPanel.add(englishField);
        individualPanel.add(Box.createHorizontalStrut(10));
        individualPanel.add(addIndividualButton);
        individualSection.add(individualPanel);
        
        // Bulk Entry Section
        JPanel bulkSection = createSectionPanel("Bulk Expression Entry");
        JPanel bulkTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bulkTopPanel.add(loadFileButton);
        bulkTopPanel.add(processBulkButton);
        
        JScrollPane scrollPane = new JScrollPane(bulkTextArea);
        scrollPane.setPreferredSize(new Dimension(900, 200));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        JPanel bulkInstructions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bulkInstructions.add(new JLabel("Paste your list directly into the text box or load it from a file with Load File."));
        
        JPanel bulkFormat = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bulkFormat.add(new JLabel("Format: Spanish - English (one per line)"));
        bulkFormat.add(Box.createHorizontalStrut(10));
        bulkFormat.add(new JLabel("Example: Casa - house, home"));
        
        bulkSection.add(bulkTopPanel);
        bulkSection.add(bulkInstructions);
        bulkSection.add(bulkFormat);
        bulkSection.add(scrollPane);
        
        // Navigation Section
        JPanel navSection = createSectionPanel("Navigation");
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        navPanel.add(viewWordsButton);
        navPanel.add(playGameButton);
        navPanel.add(learnedWordsButton);
        navPanel.add(backToLandingButton);
        navSection.add(navPanel);
        
        // Add all sections to main panel
        mainPanel.add(dbSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(individualSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(bulkSection);
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
        // Database management
        createDatabaseButton.addActionListener(e -> createDatabase());
        deleteDatabaseButton.addActionListener(e -> deleteDatabase());
        
        // Individual entry
        addIndividualButton.addActionListener(e -> addIndividualExpression());
        
        // Bulk entry
        loadFileButton.addActionListener(e -> loadFromFile());
        processBulkButton.addActionListener(e -> processBulkData());
        
        // Navigation
        viewWordsButton.addActionListener(e -> openViewWords());
        playGameButton.addActionListener(e -> openGame());
        learnedWordsButton.addActionListener(e -> openLearnedWords());
        backToLandingButton.addActionListener(e -> returnToLanding());
        
        // Enter key for individual entry
        englishField.addActionListener(e -> addIndividualExpression());
    }

    private void refreshDatabaseSelector() {
        databaseSelector.removeAllItems();
        gameController.getAvailableDatabases().forEach(databaseSelector::addItem);
        log.debug("Database selector refreshed with {} databases", databaseSelector.getItemCount());
    }

    private void createDatabase() {
        String dbName = newDatabaseField.getText().trim();
        if (dbName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a database name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (gameController.createNewDatabase(dbName)) {
            JOptionPane.showMessageDialog(this, "Database '" + dbName + "' created successfully!");
            newDatabaseField.setText("");
            refreshDatabaseSelector();
            log.info("Database '{}' created successfully", dbName);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create database '" + dbName + "'. It might already exist.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteDatabase() {
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            JOptionPane.showMessageDialog(this, "Please select a database to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if ("learned_words".equals(selectedDb)) {
            JOptionPane.showMessageDialog(this, "Cannot delete the learned words database", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete database '" + selectedDb + "'?", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            // TODO: Implement delete database functionality in GameController
            JOptionPane.showMessageDialog(this, "Delete functionality not yet implemented", "Info", JOptionPane.INFORMATION_MESSAGE);
            log.info("Delete database '{}' requested", selectedDb);
        }
    }

    private void addIndividualExpression() {
        String spanish = spanishField.getText().trim();
        String english = englishField.getText().trim();
        
        if (spanish.isEmpty() || english.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both Spanish and English expressions", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            JOptionPane.showMessageDialog(this, "Please select a database", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create entities
        SpanishExpression spanishExpr = new SpanishExpression(spanish, 0, new ArrayList<>());
        EnglishExpression englishExpr = new EnglishExpression(english, 0, new ArrayList<>());
        
        // Add to each other's translations
        spanishExpr.getTranslations().add(englishExpr);
        englishExpr.getTranslations().add(spanishExpr);
        
        // TODO: Add to database through GameController
        JOptionPane.showMessageDialog(this, "Individual expression added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        
        // Clear fields
        spanishField.setText("");
        englishField.setText("");
        
        log.info("Individual expression added: '{}' - '{}'", spanish, english);
    }


    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("All Files", "*"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                bulkTextArea.setText(content.toString());
                log.info("File loaded: {}", selectedFile.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to load file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                log.error("Failed to load file", e);
            }
        }
    }

    private void processBulkData() {
        String content = bulkTextArea.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter some expressions to process", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            JOptionPane.showMessageDialog(this, "Please select a database", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            List<String> lines = Arrays.asList(content.split("\n"));
            int processedCount = 0;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Parse format: Spanish - English, English2
                if (line.contains(" - ")) {
                    String[] parts = line.split(" - ");
                    if (parts.length == 2) {
                        String spanish = parts[0].trim();
                        String[] englishTranslations = parts[1].split(",");
                        
                        // Create Spanish expression
                        SpanishExpression spanishExpr = new SpanishExpression(spanish, 0, new ArrayList<>());
                        
                        // Create English expressions
                        for (String english : englishTranslations) {
                            english = english.trim();
                            if (!english.isEmpty()) {
                                EnglishExpression englishExpr = new EnglishExpression(english, 0, new ArrayList<>());
                                
                                // Add to each other's translations
                                spanishExpr.getTranslations().add(englishExpr);
                                englishExpr.getTranslations().add(spanishExpr);
                                
                                processedCount++;
                            }
                        }
                        
                        // TODO: Add to database through GameController
                    }
                }
            }
            
            JOptionPane.showMessageDialog(this, "Processed " + processedCount + " expressions successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            bulkTextArea.setText("");
            log.info("Bulk data processed: {} expressions", processedCount);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error processing bulk data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            log.error("Error processing bulk data", e);
        }
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
